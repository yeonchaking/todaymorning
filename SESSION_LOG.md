# SESSION_LOG — 오늘도출근

> "세션 종료" 시 1회만, 이 파일 **맨 위에** 항목을 추가한다 (최신 순).
> 살아있는 TODO는 **맨 위 항목의 것만**이다. 아래 항목 TODO는 역사적 기록.
> 오래된 항목은 `SESSION_LOG_ARCHIVE.md`로 이동(최근 5개만 유지). 작성 규칙은 `CLAUDE.md` "세션 운영" 참고.

---

## [2026-06-13] 세션로그 운영 정책 재정의 + 기존 로그 정리

**방향/목표:** 세션로그가 "개발자 일기"로 흐르며 새 세션을 오염시키는 문제를 잡기 위해, 로그의 목적·형식·TODO 정책을 코드/프로젝트 주체로 재정의하고 누적된 로그를 그 기준으로 정리.

**결정 사항:**
- 세션로그를 **프로젝트·코드 주체 기록**으로 재정의(목적 선언 신설) — 이유: 주어가 개발자가 되며 TODO에 당연한 테스트·커밋 메커니즘이 섞여 들어왔음.
- 작성은 **"세션 종료" 시 1회만, 작업 중 저장 금지**(`[작업중]` 예외 없음) — 이유: 한 세션=한 작업단위 경계 복원(06-13 하루 4개 항목이 증상).
- 카드 형식 주어 전환(`작업 결과`→`코드/프로젝트 변화`) + 결정마다 **왜** 필수 + TODO 자격기준(테스트/빌드/커밋 메커니즘 제외) — 이유: 코드/git로 복원 불가한 것만 남기기 위해.
- **아카이브=원본 전체 백업, 메인 로그=앱 영향 핵심 압축 보존** — 이유: 단순 "최근 5개만 유지"는 과거 개발이력의 앱 영향 정보(인증 함정·DB 마이그레이션 등)를 잃음.

**코드/프로젝트 변화:**
- `CLAUDE.md` "세션 운영" 절 전면 개정: 목적 선언·작성시점 규율·코드주어 카드형식·TODO 자격/해석 정책 추가.
- `SESSION_LOG_ARCHIVE.md` 신설 — 14개 원본 항목 전체 보존.
- `SESSION_LOG.md` 재구성 — 최근 5개(원형 유지) + 과거 6개 압축본(앱 영향 핵심만). 해소된/검증성 TODO 제거, 코드로 확인되는 파일 나열·테스트 체크리스트·커밋 bash 제거.

**다음 세션 TODO:** (앱 작업 — 이전부터 살아있어 의도적으로 유지)
- [ ] 지하철 역·방향 직접 선택 화면 신규 추가 (모델·타임어택은 SUBWAY 다수노선 이미 대응)
- [ ] 예외처리 전체 정리: HttpException 코드별 공통 매핑 유틸, 카카오/버스/지하철 메시지 표준화, 재시도·백오프, '동' 단위 지역검색 결과 필터링 검토
- [ ] Git 리모트 연결 (리모트 0개 → GitHub repo 생성 후 `git remote add origin <URL>` + `git push -u origin main`)
- [ ] (정식 배포) T-map/카카오 API 키 백엔드 프록시화 + 한도 제어, 또는 ODsay(무료 1,000/일) 검토

---

## [2026-06-13] 설정/메인 UI 시나리오 개선 + 입력 검증 + 핀 크기 조정

**목표:** 간단한 UI 시나리오 변경 — 시각 입력 방식 개선, 메인 화면 미션 형식 표시, 입력 검증, 지도 핀 축소

**결정 사항:**
- 시각 입력: ▲▼ 스피너 → **탭 시 Material3 시계(TimePicker) 다이얼로그**(24시간제). `material-icons-extended` 미의존이라 시계 아이콘은 `⏰` 이모지로 대체
- 메인 화면: 단순 "알람 07:00" 카드 → **"🎯 오늘의 미션 / n시에 기상해서 / m시까지 / ○○에서 노선 타기!!"** 미션 형식
- 미션 카드 정류장 줄: "에서"만 작은 폰트(15sp) + 줄바꿈 → 노선·교통수단은 큰 글씨(타기!!)
- 알람 시각 ≥ 목표 탑승 시각이면 저장 차단(스낵바 안내)
- 지도 출근버스 핀: 32×44dp → **14×22dp**(절반 + 가로 추가 축소)

**작업 결과:**
- `ui/SettingsScreen.kt`: `TimeSpinner/TimePicker(old)` 제거 → `TimeDisplayCard`(Surface onClick) + `TimePickerDialog`(AlertDialog + M3 TimePicker, `rememberTimePickerState`). 저장 시 `alarmTotal >= targetTotal` 검증 + `showTimeError` 스낵바 추가
- `ui/MainActivity.kt`: `AlarmCard` → `MissionCard(settings)`. `buildAnnotatedString`로 "에서" SpanStyle 축소 + 줄바꿈, `MissionTransitType`/`UserSettings` import 추가, 미션 미설정 시 "🚉 대중교통 타기!!" 폴백
- `ui/busselect/BusSelectScreen.kt`: 마커 비트맵 `markerW/H` 14×22dp로 축소

**다음 세션 TODO:**
- [ ] 지하철 역·방향 직접 선택 화면 신규 추가 (모델·타임어택은 SUBWAY 다수노선 이미 대응)
- [ ] 예외처리 전체 정리: HttpException 코드별 공통 매핑 유틸, 카카오/버스/지하철 메시지 표준화, 재시도·백오프, '동' 단위 지역검색 결과 필터링 검토
- [ ] (정식 배포) T-map/카카오 API 키 백엔드 프록시화 + 한도 제어, 또는 ODsay(무료 1,000/일) 검토

---

## [2026-06-13] 위치 선택 버그 수정 + API 한도 대응 → 미션 대상 직접선택 전환

**목표:** 지도 확대 시 줌 리셋 버그 수정 → API 한도(429/일10) 진단 → 경로탐색 시나리오 재설계

**결정 사항:**
- LocationPicker: 카메라 피드백 루프 제거. 지도 드래그 추측 대신 **주소 입력 Dialog → 검색결과 선택 → 핀 확인** 방식으로 전환 (역지오코딩 0회). 진입 시 빈 Dialog 자동 표시
- API 한도 진단: 카카오 `coord2address`는 일일 쿼터 소진(-10), T-map `transit/routes`는 **무료 트라이얼 일/10 공유 한도**(429=Too Many Requests). 정식 서비스 불가 확인
- **미션 대상 설정 시나리오 전환**(메모리 mission-target-pivot): T-map 경로탐색 보류 → 지도에서 첫 정류장 직접 선택 + **다수 노선 선택**(아무거나 타면 성공). 버스 먼저, 지하철 곧바로
- 집/회사 위치는 당장 불필요 → 설정 화면에서 주석처리(추후 경로탐색 복구 시 되살림)

**작업 결과:**
- `LocationPickerScreen/ViewModel`: `cameraMoveEvent`(SharedFlow)로 프로그램 이동만 반영, 사용자 줌/드래그는 카메라 비간섭. 주소검색 Dialog + "여기가 맞나요?" 확인카드 + 재입력(펜)
- `RouteSelectViewModel/Screen`: 에러 분류 토스트 추가 (429→"잠시 후", 경로없음→"출발지·목적지 정확히", 기타→코드표시)
- 데이터 모델: `MissionRoute` 추가, `UserSettings.missionRoutes: List<MissionRoute>`로 단일→리스트 전환, DataStore Gson 직렬화
- `BusSelectViewModel/Screen`: 노선 체크박스 다수선택 + "선택 완료(N)". `BusSelectResult`는 정류장+노선리스트
- `NavGraph`: 설정 "출근 버스 선택하기" → `BUS_SELECT` 연결, 결과 저장. `ROUTE_SELECT`(T-map)는 연결 해제·코드 보관
- `TimeAttackViewModel`: 선택 노선 전부 도착정보 합산 조회
- `SettingsScreen`: 미션요약을 다수노선(`missionRoutesLabel`)+정류장으로 표시, 집/회사 섹션 주석처리

**다음 세션 TODO:**
- [ ] 지하철 역·방향 직접 선택 화면 신규 추가 (모델·타임어택은 SUBWAY 다수노선 이미 대응)
- [ ] 예외처리 전체 정리: HttpException 코드별 공통 매핑 유틸, 카카오/버스/지하철 메시지 표준화, 재시도·백오프, '동' 단위 지역검색 결과 필터링 검토
- [ ] (정식 배포) T-map/카카오 API 키 백엔드 프록시화 + 한도 제어, 또는 ODsay(무료 1,000/일) 검토

---

## [2026-06-13] 지하철 역명 정규화 + 세션로그 TODO 정리

**목표:** 남은 TODO 마무리 — 지하철 `start.name` 정규화

**결정 사항:**
- 지하철 도착 조회 시점에 역명 정규화 (저장값은 표시용으로 원본 유지)
- 테스트·E2E·실기기·빌드 확인 항목은 앞으로 TODO에 넣지 않음 (사용자 피드백, 메모리 저장)

**작업 결과:**
- `TransitRepository.getSubwayArrivals`: `normalizeStationName()` 추가 — 끝 "역" 제거(강남역→강남), 괄호 부가정보 제거(시청(2호선)→시청, 총신대입구(이수)역→총신대입구), 공백 정리, 한 글자 보존. 9개 케이스 검증 통과
- 최신 entry의 E2E/정규화 TODO 제거 → 다음 세션 TODO 비움

**다음 세션 TODO:**
- 없음

---

## [2026-06-13] T-map 경로 응답 검증 + 버스 arsId 변환 보정

**목표:** T-map 대중교통 경로 API 실제 응답과 DTO/추출 로직 대조, 불일치 보정

**결정 사항:**
- 버스 정류장은 T-map `stationID`를 쓰지 않고, 탑승 정류장 좌표 → 서울버스 `getStationByPos`로 실제 arsId를 변환해 저장 (T-map ID ≠ 서울버스 arsId 확인됨)
- arsId 변환 실패 시 해당 경로는 목록에서 제외 (쓸 수 없는 미션 타겟 저장 방지)

**작업 결과:**
- 실제 호출 검증: `apis.openapi.sk.com/transit/routes` HTTP 200, **TMAP_API_KEY 정상**(401 없음, l/I 문제 아님 — 06-13 TODO 해소)
- 불일치 ① DTO 필드명: `passStopList.stationList` → 실제는 **`stations`** (기존엔 항상 빈 리스트로 파싱됨)
- 불일치 ② `stationID`(예 772580, 6자리)는 T-map 내부 ID이고 서울버스 arsId(예 22012, 5자리)와 무관 — 06-12 "arsId 매핑 정확도" TODO 검증 결과 **가정이 틀렸음 확인**
- `TmapRouteDto.kt`: `@SerializedName("stations")` 수정, 주석 보강, `type`/`routeColor` 필드 추가
- `RouteSelectViewModel.kt`: `BusApiService` 주입, `parseItinerary` suspend화, `resolveBusArsId()` 추가(좌표→arsId, 반경 200m 최근접). 검증 시 14m 거리 정류장이 정확히 일치
- 빌드 통과 확인 (사용자)

**다음 세션 TODO:**
- 없음 (지하철 역명 정규화는 06-13 완료)

---

## [2026-06-13] API 키 세팅 마무리 + 빌드 에러 수정

**목표:** 위치 기반 미션 타겟에 필요한 4개 API 키(TMAP/KAKAO_REST/BUS/SUBWAY)를 BuildConfig까지 연결 완료하고 빌드 통과시키기

**결정 사항:**
- TMAP은 필요 — 미션 타깃이 "집→정류장/역 도보 시간 역산" 기반이라 도보 경로가 필요한데, 카카오는 도보/대중교통 길찾기 API를 공개 제공하지 않음. 목적지까지 대중교통 최단시간도 쓸 거라 **TMAP + TMAP 대중교통** 두 제품 모두 한 프로젝트에 추가(appKey 공유)
- 키는 `local.properties`(.gitignore 처리됨)에만 저장, git/메모리에 커밋 안 함

**작업 결과:**
- `local.properties`: SUBWAY_API_KEY 채움, KAKAO_REST_API_KEY·TMAP_API_KEY 신규 추가 → 4개 키 모두 채워짐
  - BUS → TransitRepository, SUBWAY → TransitRepository, KAKAO_REST → LocationPickerViewModel(`KakaoAK` 헤더), TMAP → RouteSelectViewModel(`appKey`) 로 정상 소비 확인
- TMAP 콘솔: "오늘도출근" 앱 활성화, appKey 발급 완료 (TMAP Free + TMAP 대중교통 Free)
- `ui/NavGraph.kt` 컴파일 에러 수정: `collectAsState`를 일반 함수처럼 호출하던 것을 StateFlow 확장 함수 호출로 변경, `getStateFlow`가 초기값을 포함하므로 중복 default 인자 제거, `collectAsState`·`getValue` import 추가 → `compileDebugKotlin` 통과
- 커밋: `fix: NavGraph의 collectAsState 확장 함수 호출 오류 수정` (2d33435)

**다음 세션 TODO:**
- [ ] **Git 리모트 연결** — 현재 이 저장소에 리모트가 0개라 SourceTree 푸시가 무한 로딩됨. GitHub에 빈 repo 생성 후 `git remote add origin <URL>` + `git push -u origin main` 필요

---

> 아래는 이전 세션들에서 **앱 동작·빌드·DB·인증에 영향 가는 핵심**만 추린 압축본이다. 원본 전체 기록은 `SESSION_LOG_ARCHIVE.md`.

---

## [2026-06-12] T-map 경로 API 연동 — 첫 비-도보 구간 자동 추출

**방향:** 경로탐색으로 미션타겟 자동 추출 (이후 06-13에 API 한도로 보류·연결 해제됨)

**핵심/결정:**
- T-map 대중교통 경로 API 추가: `POST apis.openapi.sk.com/transit/routes`, `appKey` 헤더, `@Named("tmap")` Retrofit. `TMAP_API_KEY` BuildConfig 필드.
- 미션타겟 추출 규칙: 경로 `legs` 중 **첫 번째 비-도보(`mode != "WALK"`) 구간**. BUS=arsId, SUBWAY=`start.name`. 도보만/ID 없으면 제외.
- (현재 상태) 이 방식은 06-13 무료 한도(429)로 보류 → `ROUTE_SELECT` 라우트 연결 해제(코드는 보관).

---

## [2026-06-11] 타임어택 동작 3건 — 세로 고정 / 자동실패 폐지 / 즉시 로드

**핵심/결정:**
- `MainActivity` **세로모드 고정**(`screenOrientation="portrait"`) — 이유: 회전 시 Activity·ViewModel 재생성으로 버스정보 끊김, 가로 불필요.
- **목표시각 초과 자동실패 제거** → "성공하셨나요?" 직접선택 UI — 이유: 제시간 탑승했어도 버튼을 늦게 누르면 실패 처리되던 문제.
- 타임어택 진입 시 첫 조회 전 `dataStore.userSettings.first()`로 저장값 로드 대기 — 이유: init 시점 기본값(빈 ID)으로 "설정 없음" 분기에 빠지던 버그.
- 자동 새로고침 사실상 10초(`delay 1s × 10` 카운트다운).

---

## [2026-06-11] 데이터 모델 전면 교체 — 수동 ID 입력 → 위치/경로 기반

**방향:** 수동 버스/지하철 ID 입력 폐기 → 집·회사 위치 + 경로탐색 (1단계: 모델·위치 UI)

**핵심/결정:**
- `UserSettings` 재설계: `TransitType`·수동 ID 필드 제거 → `home/workLat·Lng·Address`, `missionTransitType`, `missionStopId/RouteId/RouteName/StopName/Direction`. DataStore `doublePreferencesKey` 사용. (이후 06-13에 `missionRoutes` 리스트로 또 진화)
- `TransitType` enum은 `TransitArrival`로 이동(ArrivalCard에서 계속 사용).
- Kakao Local API 추가: `dapi.kakao.com`(역지오코딩 `coord2address` + 주소검색 `search/address`), `KAKAO_REST_API_KEY` BuildConfig → 현 LocationPicker에서 사용.

---

## [2026-06-10] 알람화면 개편 — "끌 때까지 울리는" 진짜 알람 ⭐

**방향:** 알림음 1회 → 사용자가 직접 해제할 때까지 울리는 알람시계 경험

**핵심/결정:**
- **포그라운드 서비스가 알람음을 소유**(`AlarmRingService`): MediaPlayer 루프 + Vibrator + 풀스크린 인텐트(`alarm_ring_channel`). 앱 강제종료돼도 재진입 가능.
- 해제 방식 **슬라이드 투 디스미스**(85% 밀어야), **스누즈 의도적 제외**(출근 습관 앱 특성).
- 알람음 `RingtoneManager.TYPE_ALARM` + `STREAM_ALARM`/`USAGE_ALARM` 루프 → 무음·진동 모드에서도 알람 볼륨으로 울림.
- `AlarmRingActivity` 잠금화면 위 전체화면(`setShowWhenLocked`/`turnScreenOn`, keyguard dismiss, `singleTask`/`excludeFromRecents`).
- Manifest 필수: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 권한 + `foregroundServiceType=mediaPlayback`.
- (함정) 파일 덮어쓰기 시 일부 .kt에 NUL 패딩 끼는 현상 발생 → 제거함.

---

## [2026-05-29] 지도 기반 버스 선택(카카오맵) ⭐ 인증 함정 多

**방향:** 정류장/노선 ID 직접입력 → 지도 검색·선택 UX

**핵심/결정:**
- 지도 SDK **카카오맵 v2**(`com.kakao.maps.open:android`) — 네이버는 종량제라 회피. `KakaoMapSdk.init`(Application), `KAKAO_NATIVE_APP_KEY` BuildConfig, 위치 권한, ProGuard 규칙.
- ⚠️ **카카오맵 인증 요구사항(2024-12~): 비즈앱 전환 + [제품설정>카카오맵] 사용설정 ON 필수.** 미설정 시 401/403. 정식 출시 시 원본앱 카카오맵 API 권한 심사(3~5일).
- 디버그 빌드 패키지명 `com.yeon.todaymorning.debug`(applicationIdSuffix), 등록 키해시 `+8qgEGP1R4A+UarF0lFXXfTt7hc=`.
- 서울 버스 API(`ws.bus.go.kr`): `getStationByPos`(주변)/`getStationByName`(검색, 파라미터 `keyword`)/`getStationByUid`(경유노선+도착). HTTP만 지원 → `network_security_config` 필수.
- 목표시각 자동실패 알람을 `SettingsViewModel` 저장 시 `AlarmScheduler.scheduleMissionFailAt`로 등록(MissionFailReceiver 연결).
- (출시 전 잔여) `NetworkModule` OkHttp 로깅 BODY→BASIC 환원.

---

## [2026-05-24~25] Phase 4·5·6 — 기록 저장 / 게이미피케이션 / 안정화

**핵심/결정 (DB·빌드·네비게이션에 직접 영향):**
- Room: `MissionRecord`에 `@Index(date, unique=true)`, **DB version 1→2 + `MIGRATION_1_2`**(중복 날짜 제거 후 유니크 인덱스) — 마이그레이션 유지 필수.
- `MissionDao` `OnConflictStrategy.IGNORE`(REPLACE 아님 — 성공이 실패로 덮어써지는 버그 방지) + `updateToSuccess()`. `MissionRepository.insertTodayResult()`(성공 덮어쓰기 방지, 실패→성공 업데이트).
- `UserLevel` ROOKIE→DIAMOND 6단계(streak 기반).
- Navigation: `RESULT` 라우트, 흐름 TimeAttack→Result→Main, `MissionResultScreen` BackHandler(뒤로가기 시 Main).
- Lottie 성공/실패 애니메이션 `assets/`에 수동 제작.
- `build.gradle.kts`: release `isMinifyEnabled`/`isShrinkResources`=true, debug `.debug` suffix. `proguard-rules.pro`(Hilt/Room/Retrofit/Gson/Lottie).
