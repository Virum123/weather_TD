# Weather TD

전날 동시간 대비 기후 변화를 확인하기 위한 Tableau 데이터 수집 프로젝트입니다.

## Repository Structure

```text
weather_TD/
├─ src/
│  ├─ ingestion/                # API 수집 로직
│  │  └─ collect_weather_to_sheets.py
│  └─ common/                   # 공통 유틸 (추가 예정)
├─ data/
│  ├─ raw/                      # 원천 데이터 임시 저장
│  └─ processed/                # 전처리 데이터
├─ docs/
│  ├─ tableau/                  # Tableau 설계/계산식 문서
│  └─ app/                      # 앱 전환 문서
├─ config/                      # 설정 파일 확장 영역
├─ scripts/                     # 실행 보조 스크립트
├─ .env.example
├─ requirements.txt
└─ README.md
```

## Quick Start

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
copy .env.example .env
```

`.env` 필수값:
- `OPENWEATHER_API_KEY`

선택값:
- `DEFAULT_LAT`, `DEFAULT_LON`, `DEFAULT_TIMEZONE`
- `ALLOWED_COUNTRIES` (예: `KR,JP`)
- `SPLIT_WORKSHEET_BY_COUNTRY` (`true`면 `raw_data_kr`, `raw_data_jp`로 분리 저장)
- `GOOGLE_SHEET_ID`, `GOOGLE_WORKSHEET`, `GOOGLE_SERVICE_ACCOUNT_FILE`
- `--location-name` (표시 위치명 강제 지정, 예: `Tokyo`)

## Run

Dry run:

```bash
python src/ingestion/collect_weather_to_sheets.py --lat 37.5665 --lon 126.9780 --dry-run
```

Google Sheets append:

```bash
python src/ingestion/collect_weather_to_sheets.py --lat 37.5665 --lon 126.9780
```

GPS JSON 입력(앱에서 받은 좌표 포맷 테스트):

```bash
python src/ingestion/collect_weather_to_sheets.py --gps-json "{\"lat\":35.6895,\"lon\":139.6917}" --allow-countries KR,JP --location-name Tokyo --dry-run
```

국가 제한은 `.env`의 `ALLOWED_COUNTRIES=KR,JP`로도 설정할 수 있습니다.

나라별 워크시트 분리 저장:

```bash
python src/ingestion/collect_weather_to_sheets.py --gps-json "{\"lat\":37.5665,\"lon\":126.9780}" --allow-countries KR,JP --split-by-country
python src/ingestion/collect_weather_to_sheets.py --gps-json "{\"lat\":35.6895,\"lon\":139.6917}" --allow-countries KR,JP --split-by-country --location-name Tokyo
```

## GitHub Actions 자동 수집 (1시간)

워크플로 파일: `.github/workflows/hourly_collection.yml`

설정 순서:
1. GitHub 저장소 `Settings > Secrets and variables > Actions` 이동
2. 아래 Secrets 추가
   - `OPENWEATHER_API_KEY`
   - `GOOGLE_SHEET_ID`
   - `GOOGLE_SERVICE_ACCOUNT_JSON` (서비스계정 JSON 전체 문자열)
3. `Actions` 탭에서 `Hourly Weather Collection` 워크플로 활성화

동작:
- 매시간(UTC 기준 매시 정각)에 실행
- 서울(`raw_data_kr`) + 도쿄(`raw_data_jp`) 각각 append
- 도쿄 데이터는 `location_name=Tokyo`로 저장
- `collected_at_utc`는 시간 단위(`HH:00:00`)로 정렬되어 저장되고, 실제 실행 시각은 `executed_at_utc`에 별도 기록

비용 참고:
- GitHub Actions는 리포지토리 유형/요금제에 따라 무료 사용량이 다릅니다.
- 퍼블릭 리포지토리는 일반적으로 무료이고, 프라이빗은 월 무료 분량을 초과하면 과금될 수 있습니다.
