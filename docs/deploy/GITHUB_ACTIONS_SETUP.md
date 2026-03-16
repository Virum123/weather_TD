# GitHub Actions 설정 가이드 (1시간 자동 수집)

이 문서는 아래 3가지를 완료하는 방법입니다.
- GitHub Secrets 등록
- 원격 푸시
- Actions 수동 1회 실행 및 시트 저장 확인

## 핵심 답변

- `OPENWEATHER_API_KEY`, `GOOGLE_SHEET_ID`, `GOOGLE_SERVICE_ACCOUNT_JSON`는 **로컬 파일에 저장하는 것이 아니라 GitHub Secrets에 등록**해야 합니다.
- 즉, 이 단계는 **GitHub 웹에서 직접** 해야 합니다.
- 로컬 `.env`는 로컬 실행용이고, GitHub Actions 러너는 `.env`를 보지 않습니다.

## 1) GitHub Secrets 등록 (웹)

대상 저장소: `https://github.com/Virum123/weather_TD`

1. 저장소 페이지로 이동
2. `Settings` 탭 클릭
3. 왼쪽 메뉴 `Secrets and variables` -> `Actions`
4. `New repository secret`로 아래 3개 생성

- `OPENWEATHER_API_KEY`
  - 값: OpenWeather API 키 문자열
- `GOOGLE_SHEET_ID`
  - 값: 시트 URL `.../d/<여기>/edit` 의 `<여기>` 부분
- `GOOGLE_SERVICE_ACCOUNT_JSON`
  - 값: 서비스 계정 JSON 파일 내용 전체 (중괄호 포함 전체 텍스트)

주의:
- JSON은 파일 경로가 아니라 **파일 내용 전체 텍스트**를 넣어야 합니다.
- 공백/줄바꿈 포함 그대로 붙여넣어도 됩니다.

## 2) 원격 푸시

로컬 프로젝트 경로에서 실행:

```bash
git push origin main
```

이미 `origin`이 연결되어 있으면 위 한 줄로 충분합니다.

## 3) Actions 수동 1회 실행

1. 저장소 `Actions` 탭 이동
2. 왼쪽에서 `Hourly Weather Collection` 선택
3. `Run workflow` 클릭
4. 브랜치 `main` 선택 후 실행

## 4) 성공 확인

1. 실행 로그에서 아래 문구 확인
   - `Appended to sheet=..., worksheet=raw_data_kr`
   - `Appended to sheet=..., worksheet=raw_data_jp`
2. Google Sheets에서 탭 확인
   - `raw_data_kr`
   - `raw_data_jp`
3. 각 탭에 최신 시각 행이 추가되었는지 확인

## 자주 나는 문제

- `SpreadsheetNotFound (404)`
  - `GOOGLE_SHEET_ID` 값이 틀렸거나, 시트 공유가 안 됨
- 권한 오류(403)
  - 시트를 서비스계정 이메일에 `Editor`로 공유하지 않음
- 액션은 성공했는데 데이터 없음
  - 로그에서 `dry-run`으로 돌지 않았는지, `Appended to sheet`가 있는지 확인
