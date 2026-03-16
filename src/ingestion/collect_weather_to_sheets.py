import argparse
import json
import os
from datetime import datetime, timezone
from typing import Literal
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

import gspread
import requests
from dotenv import load_dotenv
from google.oauth2.service_account import Credentials


WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather"
AIR_POLLUTION_URL = "https://api.openweathermap.org/data/2.5/air_pollution"
COUNTRY_TIMEZONE_MAP = {
    "KR": "Asia/Seoul",
    "JP": "Asia/Tokyo",
}


def fetch_json(url: str, params: dict) -> dict:
    response = requests.get(url, params=params, timeout=20)
    response.raise_for_status()
    return response.json()


def get_weather_payload(api_key: str, lat: float, lon: float) -> dict:
    return fetch_json(
        WEATHER_URL,
        {
            "lat": lat,
            "lon": lon,
            "appid": api_key,
            "units": "metric",
        },
    )


def get_air_payload(api_key: str, lat: float, lon: float) -> dict:
    return fetch_json(
        AIR_POLLUTION_URL,
        {
            "lat": lat,
            "lon": lon,
            "appid": api_key,
        },
    )


def build_row(
    weather: dict,
    air: dict,
    tz_name: str,
    location_name_override: str | None,
    collected_at_utc: datetime,
    executed_at_utc: datetime,
) -> dict:
    resolved_tz = tz_name
    try:
        local_dt = collected_at_utc.astimezone(ZoneInfo(tz_name))
    except ZoneInfoNotFoundError:
        resolved_tz = "UTC"
        local_dt = collected_at_utc

    air_item = air["list"][0]
    components = air_item.get("components", {})

    api_location_name = weather.get("name", "")
    location_name = location_name_override.strip() if location_name_override else api_location_name

    return {
        "collected_at_utc": collected_at_utc.isoformat(),
        "collected_at_local": local_dt.isoformat(),
        "executed_at_utc": executed_at_utc.isoformat(),
        "timezone": resolved_tz,
        "location_name": location_name,
        "country": weather.get("sys", {}).get("country", ""),
        "lat": weather.get("coord", {}).get("lat", ""),
        "lon": weather.get("coord", {}).get("lon", ""),
        "temp_c": weather.get("main", {}).get("temp", ""),
        "feels_like_c": weather.get("main", {}).get("feels_like", ""),
        "wind_speed_mps": weather.get("wind", {}).get("speed", ""),
        "pm10": components.get("pm10", ""),
        "pm2_5": components.get("pm2_5", ""),
        "aqi": air_item.get("main", {}).get("aqi", ""),
    }


def parse_gps_json(gps_json: str) -> tuple[float, float]:
    try:
        payload = json.loads(gps_json)
    except json.JSONDecodeError as exc:
        raise ValueError("--gps-json must be a valid JSON string.") from exc

    lat = payload.get("lat", payload.get("latitude"))
    lon = payload.get("lon", payload.get("longitude"))
    if lat is None or lon is None:
        raise ValueError("--gps-json must include lat/lon or latitude/longitude.")
    return float(lat), float(lon)


def resolve_coordinates(args: argparse.Namespace) -> tuple[float, float]:
    if args.gps_json:
        return parse_gps_json(args.gps_json)

    if args.lat is not None or args.lon is not None:
        if args.lat is None or args.lon is None:
            raise ValueError("When using CLI coordinates, provide both --lat and --lon.")
        return args.lat, args.lon

    return float(os.getenv("DEFAULT_LAT", "0")), float(os.getenv("DEFAULT_LON", "0"))


def parse_allowed_countries(args: argparse.Namespace) -> set[str]:
    raw = args.allow_countries or os.getenv("ALLOWED_COUNTRIES", "")
    if not raw.strip():
        return set()
    return {item.strip().upper() for item in raw.split(",") if item.strip()}


def enforce_country_policy(country_code: str, allowed: set[str]) -> None:
    if not allowed:
        return
    if country_code.upper() not in allowed:
        countries = ", ".join(sorted(allowed))
        raise ValueError(
            f"Location country '{country_code}' is not allowed. Allowed countries: {countries}"
        )


def parse_bool(value: str | None, default: bool = False) -> bool:
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "y", "on"}


def normalize_collection_time(
    executed_at_utc: datetime, mode: Literal["actual", "hour"]
) -> datetime:
    if mode == "hour":
        return executed_at_utc.replace(minute=0, second=0, microsecond=0)
    return executed_at_utc


def resolve_worksheet_name(
    base_worksheet_name: str, country_code: str, split_by_country: bool
) -> str:
    if not split_by_country:
        return base_worksheet_name
    suffix = country_code.lower() if country_code else "unknown"
    return f"{base_worksheet_name}_{suffix}"


def append_to_sheet(
    row: dict,
    sheet_id: str,
    worksheet_name: str,
    service_account_file: str,
) -> None:
    scopes = [
        "https://www.googleapis.com/auth/spreadsheets",
        "https://www.googleapis.com/auth/drive",
    ]
    creds = Credentials.from_service_account_file(service_account_file, scopes=scopes)
    client = gspread.authorize(creds)
    spreadsheet = client.open_by_key(sheet_id)

    try:
        worksheet = spreadsheet.worksheet(worksheet_name)
    except gspread.WorksheetNotFound:
        worksheet = spreadsheet.add_worksheet(title=worksheet_name, rows=1000, cols=30)

    existing_header = worksheet.row_values(1)
    headers = list(row.keys())
    if existing_header != headers:
        worksheet.update(range_name="A1", values=[headers])

    if is_duplicate_for_same_hour(worksheet, row, headers):
        print(f"Skipped duplicate hourly row for worksheet={worksheet_name}")
        return

    worksheet.append_row(list(row.values()), value_input_option="USER_ENTERED")


def hour_key(iso_dt: str) -> str:
    dt = datetime.fromisoformat(iso_dt.replace("Z", "+00:00")).astimezone(timezone.utc)
    return dt.strftime("%Y-%m-%dT%H")


def safe_float(value: str | float | int | None) -> float | None:
    if value is None or value == "":
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def is_duplicate_for_same_hour(worksheet, row: dict, headers: list[str]) -> bool:
    values = worksheet.get_all_values()
    if len(values) <= 1:
        return False

    last = values[-1]
    last_map = {h: (last[i] if i < len(last) else "") for i, h in enumerate(headers)}

    last_country = str(last_map.get("country", "")).upper()
    row_country = str(row.get("country", "")).upper()
    if last_country != row_country:
        return False

    last_lat = safe_float(last_map.get("lat"))
    last_lon = safe_float(last_map.get("lon"))
    row_lat = safe_float(row.get("lat"))
    row_lon = safe_float(row.get("lon"))
    if None in (last_lat, last_lon, row_lat, row_lon):
        return False
    if abs(last_lat - row_lat) > 0.01 or abs(last_lon - row_lon) > 0.01:
        return False

    last_collected = str(last_map.get("collected_at_utc", "")).strip()
    row_collected = str(row.get("collected_at_utc", "")).strip()
    if not last_collected or not row_collected:
        return False

    return hour_key(last_collected) == hour_key(row_collected)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Collect weather + air quality data and append to Google Sheets."
    )
    parser.add_argument("--lat", type=float, default=None, help="Latitude")
    parser.add_argument("--lon", type=float, default=None, help="Longitude")
    parser.add_argument(
        "--gps-json",
        default=None,
        help='GPS json payload, e.g. \'{"lat":37.56,"lon":126.97}\'',
    )
    parser.add_argument(
        "--allow-countries",
        default=None,
        help="Comma-separated ISO country codes, e.g. KR,JP",
    )
    parser.add_argument(
        "--split-by-country",
        action="store_true",
        help="Append data into separate worksheets by country code.",
    )
    parser.add_argument(
        "--location-name",
        default=None,
        help="Optional location display name override (e.g. Tokyo).",
    )
    parser.add_argument("--timezone", default=None, help="IANA timezone, e.g. Asia/Seoul")
    parser.add_argument(
        "--timestamp-mode",
        choices=("actual", "hour"),
        default=None,
        help="Store the real execution time or align collected_at timestamps to the hour.",
    )
    parser.add_argument("--dry-run", action="store_true", help="Print payload only")
    return parser.parse_args()


def main() -> None:
    load_dotenv()
    args = parse_args()

    api_key = os.getenv("OPENWEATHER_API_KEY")
    if not api_key:
        raise ValueError("OPENWEATHER_API_KEY is missing. Add it to .env.")

    lat, lon = resolve_coordinates(args)
    allowed_countries = parse_allowed_countries(args)

    weather = get_weather_payload(api_key, lat, lon)
    air = get_air_payload(api_key, lat, lon)
    country_code = weather.get("sys", {}).get("country", "").upper()
    enforce_country_policy(country_code, allowed_countries)
    tz_name = args.timezone or COUNTRY_TIMEZONE_MAP.get(country_code) or os.getenv(
        "DEFAULT_TIMEZONE", "UTC"
    )
    timestamp_mode = args.timestamp_mode or os.getenv("COLLECTION_TIMESTAMP_MODE", "actual")
    executed_at_utc = datetime.now(timezone.utc)
    collected_at_utc = normalize_collection_time(executed_at_utc, timestamp_mode)
    row = build_row(
        weather,
        air,
        tz_name,
        args.location_name,
        collected_at_utc,
        executed_at_utc,
    )

    print("Collected row:")
    print(json.dumps(row, ensure_ascii=False, indent=2))

    if args.dry_run:
        return

    sheet_id = os.getenv("GOOGLE_SHEET_ID")
    base_worksheet_name = os.getenv("GOOGLE_WORKSHEET", "raw_data")
    service_account_file = os.getenv("GOOGLE_SERVICE_ACCOUNT_FILE", "service_account.json")
    split_by_country = args.split_by_country or parse_bool(
        os.getenv("SPLIT_WORKSHEET_BY_COUNTRY"), default=False
    )
    worksheet_name = resolve_worksheet_name(
        base_worksheet_name=base_worksheet_name,
        country_code=country_code,
        split_by_country=split_by_country,
    )

    if not sheet_id:
        print("GOOGLE_SHEET_ID is not set. Skipped Google Sheets append.")
        return

    if not os.path.exists(service_account_file):
        raise FileNotFoundError(
            f"Service account file not found: {service_account_file}. "
            "Set GOOGLE_SERVICE_ACCOUNT_FILE correctly."
        )

    append_to_sheet(row, sheet_id, worksheet_name, service_account_file)
    print(f"Appended to sheet={sheet_id}, worksheet={worksheet_name}")


if __name__ == "__main__":
    main()
