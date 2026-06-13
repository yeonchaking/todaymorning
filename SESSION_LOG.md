# SESSION_LOG — 오늘도출근

> 세션 종료 시 Claude가 자동으로 이 파일에 요약을 추가한다 (최신 순).
> 세션이 10개를 넘으면 오래된 항목은 `SESSION_LOG_ARCHIVE.md`로 이동.

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
- [ ] TMAP_API_KEY의 `l/I` 글자 — 콘솔 복사값이라 정상이라 했으나, 인증오류(401) 나면 재대조
- [ ] RouteSelectScreen 실제 동작 확인(경로 카드 표시 → 선택 → 미션 타겟 저장 → 타임어택 도착정보)

---

## [2026-06-12] 경로 기반 미션 타겟 — 2단계: T-map API + 경로 선택 화면

**목표:** T-map 대중교통 경로 API 연동, RouteSelectScreen 구현, 첫 번째 대중교통 구간 자동 추출 → 미션 타겟 저장

**작업 결과:**

| 파일 | 변경 내용 |
|------|-----------|
| `data/api/dto/TmapRouteDto.kt` | 신규. T-map 경로 요청/응답 DTO 전체 (`TmapRouteRequest`, `TmapRouteResponse` ~ `TmapStation`) |
| `data/api/TmapApiService.kt` | 신규. `@POST("transit/routes")` — `appKey` 헤더, JSON Body |
| `di/NetworkModule.kt` | T-map Retrofit 인스턴스 추가 (`apis.openapi.sk.com`, `@Named("tmap")`) |
| `app/build.gradle.kts` | `TMAP_API_KEY` BuildConfig 필드 추가 |
| `ui/routeselect/RouteSelectViewModel.kt` | 신규. T-map API 호출 → `RouteOption` 파싱. 버스(arsId 추출)/지하철(역명) 분기. `selectRoute()` → DataStore 저장 |
| `ui/routeselect/RouteSelectScreen.kt` | 신규. 경로 카드 목록(소요시간·환승·요금·미션타겟 요약). 선택 시 자동 저장 후 설정으로 복귀 |
| `ui/NavGraph.kt` | `ROUTE_SELECT` 라우트 추가. `onFindRoute` placeholder → 실제 navigate 연결 |

**핵심 로직 — 미션 타겟 추출:**
- T-map 경로 `legs` 중 첫 번째 비-도보(`mode != "WALK"`) 구간이 미션 대상
- BUS/EXPRESSBUS: `passStopList.stationList[0].stationID` = arsId (서울버스 API 호환)
- SUBWAY: `start.name` = 역명 (지하철 API 호환)
- 도보만 있거나 stationID 없으면 해당 경로 제외

**local.properties 추가 필요:**
```
TMAP_API_KEY=발급받은_티맵_API_키
```
T-map Developers(https://tmapapi.sktelecom.com) → 앱 키 발급

**빌드/테스트 필요:**
- Android Studio에서 Gradle sync 후 빌드 확인
- 설정 화면 → "출근 경로 탐색하기" → RouteSelectScreen → 경로 카드 표시 확인
- 경로 선택 → 설정 화면으로 복귀 + 미션 타겟 카드 표시 확인
- 타임어택 화면에서 실제 해당 노선 버스 도착정보 표시 확인

**다음 세션 TODO:**
- [ ] Android Studio 빌드 + 실기기 E2E 테스트
- [ ] T-map API 응답 실제 확인 (arsId 매핑 정확도)
- [ ] 커밋: Phase 1 + Phase 2 변경 파일 전체 (index.lock 해제 후)

---

## [2026-06-11] 타임어택 화면 버그 수정 2건

### 작업 1 — 타임어택 진입 시 버스 정보 즉시 로드 안 되는 버그 수정

**원인:** `TimeAttackViewModel.init`에서 `startPolling()`이 즉시 호출되어 `fetchArrivals()`를 실행하는데, 이 시점에 `settings.value`가 DataStore 로드 전 기본값(`UserSettings()` — 빈 ID)이라 "설정 정보 없음" 분기로 빠짐. 수동 새로고침 시에는 DataStore가 이미 로드돼 있어 정상 동작.

**수정:**
- `TimeAttackViewModel.startPolling()` — 첫 `fetchArrivals()` 호출 전 `dataStore.userSettings.first()`로 실제 저장값 로드 대기 추가 (기존 `startCountdown()`과 동일한 패턴)

**변경 파일:** `ui/timeattack/TimeAttackViewModel.kt`

---

### 작업 2 — 가로 회전 시 버스 정보 끊김 방지 + 자동 새로고침 10초 + 카운트다운 UI

**원인:** 화면 회전 시 Activity가 재생성되어 ViewModel이 새로 초기화 → 버스 정보 재로드 필요. 사용 시나리오상 가로모드 불필요.

**수정:**
| 파일 | 변경 내용 |
|------|-----------|
| `AndroidManifest.xml` | `MainActivity`에 `android:screenOrientation="portrait"` 추가 — 회전 자체를 차단 |
| `TimeAttackViewModel.kt` | `_refreshCountdown: MutableStateFlow<Int>(10)` 추가. `startPolling()` 루프를 `delay(30s)` → 10→1 카운트다운(`delay 1s × 10`) 구조로 변경 → 사실상 10초 폴링 |
| `TimeAttackScreen.kt` | 도착 정보 헤더 영역: 로딩 아닐 때 `새로고침` 버튼 왼쪽에 `n초 뒤` 레이블 표시 |

---

### 작업 3 — 목표 시각 초과 시 자동 실패 제거 + 성공/실패 직접 선택 UI

**원인:** 목표 시각이 지나는 순간 `onMissionFail()` 자동 호출 → 제시간에 탑승했지만 버튼을 바로 못 누른 경우도 실패 처리됨.

**수정:**
| 파일 | 변경 내용 |
|------|-----------|
| `TimeAttackViewModel.kt` | `startCountdown()` — `remaining <= 0` 시 `onMissionFail()` 자동 호출 제거. 미션 상태가 Active인 동안 타이머 계속 진행 |
| `TimeAttackScreen.kt` | 버튼 분기 추가: `remainingSeconds > 0`이면 기존 "탑승 완료!" 버튼, `<= 0`이면 "미션에 성공하셨나요?" 카드 + ❌ 실패 / ✅ 성공 선택 버튼 |
| `TimeAttackScreen.kt` | `CountdownCard` — 초과 시 표시 `"⚠️ +timeText 초과"` → `timeText` + 라벨 `"지남"` 으로 변경. 실패 확정 문구도 `"❌ 시간 초과"` → `"❌ 미션 실패"` |

---

### 커밋 대기 중

Android Studio가 `index.lock` 보유 중이라 원격 커밋 불가. 집에 와서 터미널로 직접 실행 권장:

```bash
git add app/src/main/AndroidManifest.xml \
        app/src/main/java/com/yeon/todaymorning/ui/timeattack/TimeAttackViewModel.kt \
        app/src/main/java/com/yeon/todaymorning/ui/timeattack/TimeAttackScreen.kt
git commit -m "fix: 타임어택 버스 즉시 로드 + 세로 고정 + 10초 자동 새로고침 + 시간초과 직접 선택 UI"
```

---

## [2026-06-11] 경로 기반 미션 타겟 — 1단계: 위치 설정 + 모델 리팩터

**목표:** 수동 버스/지하철 ID 입력 → 집·회사 위치 설정 + T-map 경로 탐색으로 완전 대체 (1단계: 위치 설정 UI + 모델 교체)

**결정 사항:**
- 경로 탐색 API: T-map 대중교통 경로 (Phase 2)
- 집/회사 위치: KakaoMap 중앙 핀 + 주소 검색으로 설정
- 미션 타겟: T-map 경로 첫 번째 대중교통 구간 자동 추출 (Phase 2)
- 기존 `TransitType(BUS/SUBWAY/BOTH)` + 수동 ID 입력 → 완전 제거

**작업 결과:**

| 파일 | 변경 내용 |
|------|-----------|
| `domain/model/UserSettings.kt` | `TransitType`, 버스/지하철 ID 필드 제거 → `homeLat/Lng/Address`, `workLat/Lng/Address`, `missionTransitType`, `missionStopId/RouteId/RouteName/StopName/Direction` 추가 |
| `domain/model/TransitArrival.kt` | `TransitType` enum 이동 (UserSettings에서 분리, ArrivalCard에서 계속 사용) |
| `data/datastore/UserSettingsDataStore.kt` | DataStore 키 전면 교체. `doublePreferencesKey` 사용 |
| `data/api/KakaoLocalApiService.kt` | 신규. 역지오코딩(`coord2address`) + 주소 검색(`search/address`) |
| `data/api/dto/KakaoLocalDto.kt` | 신규. Kakao Local API 응답 DTO |
| `di/NetworkModule.kt` | Kakao Local API Retrofit 인스턴스 추가 (`dapi.kakao.com`) |
| `ui/locationpicker/LocationPickerViewModel.kt` | 신규. 카메라 이동 → 역지오코딩, 주소 검색, 결과 선택 |
| `ui/locationpicker/LocationPickerScreen.kt` | 신규. KakaoMap 중앙 고정 핀 + 주소 검색바 + 내 위치 버튼 + 확정 버튼 |
| `ui/SettingsScreen.kt` | 버스/지하철 섹션 제거 → 집/회사 위치 카드 + 경로 탐색 버튼 |
| `ui/NavGraph.kt` | `BUS_SELECT` 제거 → `HOME_PICKER`, `WORK_PICKER` 추가. 위치 결과 SavedStateHandle로 전달 |
| `ui/timeattack/TimeAttackViewModel.kt` | `fetchArrivals()` — 버스/지하철 분기를 `missionTransitType` 기반으로 변경 |
| `app/build.gradle.kts` | `KAKAO_REST_API_KEY` BuildConfig 필드 추가 |

**local.properties 추가 필요:**
```
KAKAO_REST_API_KEY=발급받은_카카오_REST_API_키
```
카카오 개발자 콘솔 → 앱 → 앱 키 → REST API 키

**2단계 TODO (다음 세션):**
- [ ] T-map API 연동 (`POST https://apis.openapi.sk.com/transit/routes`)
- [ ] RouteSelectScreen — 경로 옵션 카드 목록 + 선택
- [ ] 첫 번째 대중교통 구간 자동 추출 → `missionStopId` 등 저장
- [ ] `local.properties`에 `TMAP_API_KEY` 추가

---

## [2026-06-10] 알람 화면 개편 — "끌 때까지 울리는" 진짜 알람 구현

**목표:** 기존엔 알람이 알림음 1회로 끝나고 바로 타임어택으로 넘어감. 사용자가 직접 해제할 때까지 소리·진동이 계속 울리는 알람시계 경험으로 개편.

**결정 사항:**
- 끄는 방식: **슬라이드 투 디스미스**(끝까지 밀어야 해제). 단순 탭/버튼 실수 방지.
- **스누즈 없음** — 출근 습관 앱 특성상 의도적으로 제외.
- 아키텍처: "알림 1발" → **포그라운드 서비스가 알람음을 소유**하는 구조로 전환. 앱이 강제종료돼도 알림이 남아 재진입 가능.
- 알람음은 시스템 기본 알람음(`RingtoneManager.TYPE_ALARM`) `STREAM_ALARM`/`USAGE_ALARM` 루프 → 무음·진동 모드에서도 알람 볼륨으로 울림. (별도 음원 파일 미사용)
- 해제 후 흐름: 슬라이드 → 서비스 정지 → `MainActivity`를 `EXTRA_FROM_ALARM=true`로 실행 → 기존 NavGraph 분기로 타임어택 진입(기존 흐름 재사용).

**작업 결과:**
- 신규 `alarm/AlarmRingService.kt`: 포그라운드 서비스. MediaPlayer 루프 + Vibrator 패턴(1s on/1s off) + 풀스크린 인텐트 알림(`alarm_ring_channel`, 소리는 채널 아닌 서비스가 담당). `start()/stop()` 정적 헬퍼, `ACTION_STOP` 처리.
- 신규 `ui/AlarmRingActivity.kt`: 잠금화면 위 전체화면(`setShowWhenLocked/turnScreenOn`+keyguard dismiss). Compose 슬라이드 컨트롤(`detectHorizontalDragGestures`, 85% 이상 밀면 해제). 실시간 시계 표시. 뒤로가기로 못 끔. 썸 아이콘은 의존성 리스크 줄이려 텍스트 화살표(→) 사용.
- `alarm/AlarmReceiver.kt`: 알림 직접 발송 로직 제거 → `AlarmRingService.start(context)` 호출로 단순화. 미사용 import 정리.
- `AndroidManifest.xml`: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 권한 추가. `AlarmRingActivity`(showWhenLocked/turnScreenOn/excludeFromRecents/singleTask), `AlarmRingService`(foregroundServiceType=mediaPlayback) 등록.

**검증:**
- 샌드박스에 Android SDK 없어 실제 Gradle 빌드는 미실행. 참조 무결성·미사용 import·NUL 바이트 정적 점검 통과.
- ⚠️ 파일 덮어쓰기 과정에서 일부 .kt에 NUL 패딩이 끼는 현상 발견 → 제거 완료. (Android Studio에서 한 번 더 빌드 권장)

**다음 세션 TODO:**
- [ ] Android Studio 실제 빌드 + 실기기 테스트: 알람 시각에 풀스크린 알람 뜨는지, 소리/진동 반복, 슬라이드 해제 → 타임어택 진입까지 E2E
- [ ] Android 14+ FGS 시작 제한 확인(`setAlarmClock` 예외로 허용 예상이나 실기기 검증)
- [ ] 화면 꺼짐/잠금/DND 상태에서 알람 동작 확인, 통화 중·이어폰 케이스 점검
- [ ] (선택) 설정 화면에 알람음 선택/볼륨 기능 추가
- [ ] 기존 버스 API 키 활성화·E2E TODO(이전 세션) 잔존

---

## [2026-05-29] 지도 기반 버스 선택 UX 구현 (카카오맵)

**목표:** 정류장/노선 ID 직접입력 UX를 지도 기반 검색·선택으로 전면 교체 + 핵심 루프 완성

**결정 사항:**
- 지도 SDK는 **카카오맵 v2** (`com.kakao.maps.open:android`). 네이버는 종량제라 회피. 키 해시 등록 필수.
- 버스 데이터는 서울 `ws.bus.go.kr` 공개 API: `getStationByPos`(주변), `getStationByName`(검색), `getStationByUid`(경유노선+도착). 지하철은 v1 비활성(자리만).

**작업 결과:**
- 빌드: settings/libs/app gradle에 카카오맵+location 의존성, `KAKAO_NATIVE_APP_KEY` BuildConfig, Application에서 `KakaoMapSdk.init`, 위치 권한, ProGuard 규칙.
- 데이터: `StationDtos.kt`(DTO 3종), `BusApiService` 검색 메서드 2개, `TransitRepository.nearbyBusStops/searchBusStops/getRoutesAtStop`.
- 모델: `BusStop`/`BusRouteOption`, `UserSettings`에 표시이름 필드(busStopName/busRouteName/busDirection)+`hasBus`, DataStore 키 확장.
- UI: `ui/busselect/BusSelectScreen`+`BusSelectViewModel`(카카오 MapView·검색바·내주변 GPS·라벨핀·노선 바텀시트), `SettingsScreen` ID칸 제거→지도선택 버튼+요약카드, `NavGraph` bus_select 라우트+savedStateHandle 결과반환.
- 루프: `AlarmScheduler.scheduleMissionFailAt`로 목표시각 자동실패 알람을 `SettingsViewModel` 저장 시 등록(기존 MissionFailReceiver 연결).

**빌드/실기기 검증 진행 상황 (2026-05-30 새벽):**
- ✅ 빌드 성공. `getStationByName` 호출부 파라미터명 불일치(stSrch→keyword) 수정함.
- ✅ 카카오맵 인증 해결: 디버그 패키지명은 `com.yeon.todaymorning.debug`(applicationIdSuffix), 키해시 `+8qgEGP1R4A+UarF0lFXXfTt7hc=` 등록. 401→403→해결 순.
  - ⚠️ 2024-12부터 카카오맵은 비즈앱+카카오맵 사용설정 ON 필수. 원본앱은 비즈전환됨, [제품설정>카카오맵] 사용설정 ON으로 지도 정상 표시.
  - 정식 출시 시 원본앱 카카오맵 API 권한 심사(3~5일) 필요. 개발은 테스트앱으로 진행 가능.
- 🔑 키: BUS_API_KEY, KAKAO_NATIVE_APP_KEY 모두 local.properties에 입력 완료(커밋 금지).
- 🐞 진단 로깅 추가: TransitRepository catch에 Log.e, NetworkModule OkHttp 로깅 BASIC→**BODY**(출시 전 BASIC 환원 필요).
- ⏳ **현재 블로커: 버스 API 키 활성화 대기.** data.go.kr 두 서비스(정류소정보조회+버스도착정보조회) 당일(05-30) 신청·승인됐으나 인증서버 미반영 → 앱·포털 미리보기 모두 `SERVICE KEY IS NOT REGISTERED ERROR(코드30)`. 키 전파(수시간~1일) 후 자동 해결 예상. 코드 수정 불필요.

**다음 세션 TODO:**
- [ ] 버스 키 활성화 확인 (data.go.kr 미리보기에서 정상 데이터 뜨는지) → 앱에서 핀/검색/노선 동작 확인
- [ ] 실기기 E2E: 지도에서 정류장→노선 선택 → 저장 → 알람 → 타임어택 도착정보 → 탑승완료/자동실패 → 메인 스트릭
- [ ] 키 활성화 후에도 검색/노선만 안 되면 응답 JSON 필드명 vs StationDtos 대조(파싱 점검)
- [ ] 출시 전: OkHttp 로깅 BODY→BASIC 환원, 원본앱 카카오맵 권한 심사 신청, 릴리스 키해시 등록
- [ ] 앱 아이콘 적용: app_icon.png를 Image Asset으로 mipmap 교체(현재 기본 아이콘)

---

## [2026-05-25] 전체 구현 완료 + 테스트 체크리스트

**상태:** Phase 1~6 코드 구현 완료. 빌드 검증 및 실기기 테스트 대기 중.

---

### 커밋 방법 (먼저 할 것)

```bash
# Android Studio 완전히 닫은 후 Git Bash에서 실행
cd ~/AndroidStudioProjects/todaymorning
bash commit_phases.sh
```

Phase 4 / Phase 5 / Phase 6 커밋이 순서대로 생성됨.

---

### 전체 구현 내용 요약

#### Phase 4 — 미션 기록 저장 + 메인화면 통계
| 파일 | 변경 내용 |
|------|-----------|
| `data/db/MissionDao.kt` | `getRecentRecords()`, `getTotalCount()` 쿼리 추가 |
| `data/repository/MissionRepository.kt` | 신규. `getCurrentStreak()`, `getSuccessRate()`, `insertTodayResult()` |
| `di/DatabaseModule.kt` | `MissionRepository` Hilt 바인딩 추가 |
| `ui/main/MainViewModel.kt` | 신규. streak/성공률/최근기록 Flow 수집 |
| `ui/MainActivity.kt` | `MainScreen` 전면 재작성 — 알람카드, 통계카드, 기록 목록 |
| `ui/timeattack/TimeAttackViewModel.kt` | `MissionDao` 직접 주입 → `MissionRepository`로 교체 |

#### Phase 5 — 게이미피케이션
| 파일 | 변경 내용 |
|------|-----------|
| `domain/model/UserLevel.kt` | 신규. ROOKIE→DIAMOND 6단계 레벨 enum |
| `assets/lottie_success.json` | 신규. 초록 원 바운스 + 체크마크 Lottie |
| `assets/lottie_fail.json` | 신규. 빨간 원 팝업 + X마크 + 셰이크 Lottie |
| `ui/result/MissionResultViewModel.kt` | 신규. streak 로드 후 레벨 계산 |
| `ui/result/MissionResultScreen.kt` | 신규. Lottie 애니메이션 + 레벨 카드 + 동기부여 메시지 |
| `ui/NavGraph.kt` | `RESULT` 라우트 추가. 흐름: TimeAttack → Result → Main |
| `ui/timeattack/TimeAttackScreen.kt` | `onMissionDone()` → `onMissionComplete(isSuccess: Boolean)` |
| `ui/MainActivity.kt` | `StatsCard`에 레벨 배지(이모지 + 레벨명 + 설명) 추가 |

#### Phase 6 — 안정화 + 배포 준비
| 파일 | 변경 내용 |
|------|-----------|
| `data/db/MissionRecord.kt` | `@Index(value=["date"], unique=true)` 추가 |
| `data/db/AppDatabase.kt` | DB version 1→2, `MIGRATION_1_2` (중복 날짜 제거 후 유니크 인덱스) |
| `data/db/MissionDao.kt` | `OnConflictStrategy.REPLACE` → `IGNORE`, `updateToSuccess()` 추가 |
| `data/repository/MissionRepository.kt` | `insertTodayResult()` — 성공 덮어쓰기 방지, 실패→성공 업데이트 |
| `ui/timeattack/TimeAttackViewModel.kt` | `insert()` → `insertTodayResult()` |
| `ui/result/MissionResultScreen.kt` | `BackHandler` 추가 — 뒤로가기 시 TimeAttack 대신 Main으로 |
| `app/proguard-rules.pro` | Hilt / Room / Retrofit / Gson / Lottie 전체 규칙 |
| `app/build.gradle.kts` | release: `isMinifyEnabled=true`, `isShrinkResources=true` / debug: `.debug` suffix |

---

### 테스트 체크리스트

#### 빌드 검증
- [ ] Android Studio에서 debug 빌드 성공 (컴파일 오류 없음)
- [ ] release 빌드 성공 (ProGuard 적용, minify 후 앱 정상 실행)

#### 핵심 흐름 테스트
- [ ] **알람 발화** — 설정한 시각에 알람이 울리고 TimeAttackScreen이 열린다
- [ ] **탑승 성공** — "탑승 완료" 버튼 → 0.6초 후 MissionResultScreen(성공) → Lottie 애니메이션 재생
- [ ] **자동 실패** — 목표 시각 초과 시 자동으로 실패 처리 → MissionResultScreen(실패) 이동
- [ ] **결과 화면 → 메인** — "메인으로" 버튼, 뒤로가기 버튼 모두 Main으로 이동 (TimeAttack으로 돌아가지 않음)
- [ ] **메인화면 통계** — 성공/실패 후 메인 돌아왔을 때 streak, 성공률, 기록 갱신 확인

#### 엣지케이스 테스트
- [ ] **같은 날 중복 실행** — 하루에 알람 두 번 발화해도 DB에 레코드 1개만 생성됨
- [ ] **실패 후 성공** — 실패 기록이 있는 날 다시 성공하면 성공으로 업데이트됨 (실패 중복 생성 안 됨)
- [ ] **성공 후 실패 불가** — 성공 기록이 있는 날 MissionFailReceiver가 발화해도 덮어쓰지 않음
- [ ] **DB 마이그레이션** — Phase 1~3 때 설치한 앱에 업데이트 시 기존 데이터 유지되는지

#### 레벨 시스템 테스트
- [ ] streak 0일 → 루키 🌱
- [ ] streak 3일 → 브론즈 🥉
- [ ] streak 7일 → 실버 🥈
- [ ] streak 14일 → 골드 🥇
- [ ] 메인화면 StatsCard에 현재 레벨 표시 확인
- [ ] 결과화면에 레벨 + 동기부여 메시지 표시 확인

#### 설정 테스트
- [ ] 버스/지하철 정류장 ID 저장 후 타임어택 화면에서 실시간 도착 정보 표시
- [ ] 알람 시각 변경 후 정상 발화 확인
- [ ] "5초 후 테스트" 버튼으로 알람 빠른 검증

---

### 배포 전 남은 작업 (코드 외)

1. **API 키 입력** — `local.properties` 파일:
   ```
   BUS_API_KEY=여기에_공공데이터포털_버스API키
   SUBWAY_API_KEY=여기에_서울열린데이터광장_지하철API키
   ```

2. **Play Store 서명** — Android Studio > Build > Generate Signed Bundle/APK
   - 키스토어 파일 생성 (분실 시 업데이트 불가 — 안전한 곳에 백업)
   - `build.gradle.kts`에 `signingConfigs` 블록 추가

3. **Lottie 교체 (선택)** — [lottiefiles.com](https://lottiefiles.com)에서 무료 애니메이션 다운로드 후
   `app/src/main/assets/lottie_success.json`, `lottie_fail.json` 교체

---

## [2026-05-25] Phase 6 세션 요약

**목표:** Phase 6 — 안정화 + 배포 준비

**결정 사항:**
- MissionRecord에 date 유니크 인덱스 추가 → DB version 1→2 마이그레이션
- 중복 기록 방지는 DB 레벨(유니크 인덱스) + Repository 레벨(insertTodayResult) 이중으로
- OnConflictStrategy.REPLACE → IGNORE로 변경 (성공 기록이 실패로 덮어써지는 버그 방지)
- MissionResultScreen에 BackHandler 추가 (뒤로가기 → Main, TimeAttack으로 돌아가지 않음)
- release 빌드: isMinifyEnabled=true, isShrinkResources=true
- debug 빌드: applicationIdSuffix=".debug" (release와 동시 설치 가능)

**작업 결과:**
- `data/db/MissionRecord.kt` — @Index(unique=true) 추가
- `data/db/AppDatabase.kt` — version 2, MIGRATION_1_2 (중복 제거 후 유니크 인덱스)
- `data/db/MissionDao.kt` — OnConflictStrategy.IGNORE, updateToSuccess() 쿼리 추가
- `data/repository/MissionRepository.kt` — insertTodayResult() 추가
- `ui/timeattack/TimeAttackViewModel.kt` — insert → insertTodayResult 교체
- `ui/result/MissionResultScreen.kt` — BackHandler 추가
- `app/proguard-rules.pro` — Hilt/Room/Retrofit/Gson/Lottie 규칙 전부 작성
- `app/build.gradle.kts` — release minify/shrink 활성화, debug suffix 추가
- `commit_phases.sh` — Phase 6 커밋 섹션 추가

**다음 세션 TODO:**
- [ ] Android Studio에서 빌드 검증 (Phase 4+5+6 전체)
- [ ] commit_phases.sh 실행 (Android Studio 닫은 후 Git Bash에서)
- [ ] 실기기 테스트 — 알람→타임어택→성공/실패→결과화면→메인 전체 흐름
- [ ] Play Store 배포: 앱 서명 키스토어 생성, release APK/AAB 빌드
- [ ] (선택) Lottie JSON을 LottieFiles.com 공식 애니메이션으로 교체

---

## [2026-05-24] Phase 4 + Phase 5 세션 요약

**목표:** Phase 4 (메인화면 통계) 완성 + Phase 5 (게이미피케이션) 구현

**결정 사항:**
- MissionRepository를 @Singleton으로 새로 생성 (Hilt 연동)
- getCurrentStreak()는 suspend fun + for-loop으로 구현 (repeat 대신)
- TimeAttackViewModel → MissionDao 직접 주입 → MissionRepository로 교체
- Phase 5 결과 화면: TimeAttackScreen에서 즉시 분리 (0.6초 딜레이 후 navigate)
- UserLevel: ROOKIE~DIAMOND 6단계 streak 기반 레벨 시스템
- Lottie 애니메이션: assets/lottie_success.json, assets/lottie_fail.json 수동 제작

**작업 결과 (Phase 4):**
- `data/repository/MissionRepository.kt` 신규 생성
- `di/DatabaseModule.kt` — provideMissionRepository() 추가
- `ui/main/MainViewModel.kt` 신규 생성 (ui/main/ 서브패키지)
- `ui/MainActivity.kt` — MainScreen 전면 재작성 (LazyColumn, StatsCard, MissionRecordItem)
- `ui/timeattack/TimeAttackViewModel.kt` — MissionDao → MissionRepository로 교체

**작업 결과 (Phase 5):**
- `domain/model/UserLevel.kt` 신규 생성 (6단계 레벨 enum + fromStreak() + daysToNextLevel())
- `app/src/main/assets/lottie_success.json` 신규 생성
- `app/src/main/assets/lottie_fail.json` 신규 생성
- `ui/result/MissionResultViewModel.kt` 신규 생성
- `ui/result/MissionResultScreen.kt` 신규 생성 (Lottie + 레벨 카드 + 동기부여 메시지)
- `ui/NavGraph.kt` — RESULT 라우트 추가, TimeAttack→Result→Main 흐름 완성
- `ui/timeattack/TimeAttackScreen.kt` — onMissionComplete(isSuccess: Boolean) 콜백으로 변경
- `ui/MainActivity.kt` — StatsCard에 UserLevel 배지 추가

**다음 세션 TODO:**
- [ ] Android Studio에서 Phase 4+5 빌드 확인 (컴파일 오류 없는지 검증)
- [ ] Phase 6: 안정화 — 엣지케이스 처리 (날짜 변경, DB 중복 insert 방지)
- [ ] Phase 6: ProGuard 규칙 작성
- [ ] Phase 6: Play Store 배포 준비 (서명, 릴리즈 빌드)
- [ ] (선택) Lottie JSON을 LottieFiles.com 공식 애니메이션으로 교체

---

## [2026-05-24] 세션 요약

**목표:** CLAUDE.md 작성 및 세션 운영 체계 구축

**결정 사항:**
- CLAUDE.md를 프로젝트 지침서 형태로 전면 재작성
- 세션 시작/종료 트리거 및 요약 카드 형식 정의
- SESSION_LOG.md를 최신 순으로 누적 관리하기로 결정

**작업 결과:**
- `CLAUDE.md` 작성 완료 (앱 개요, 기술 스택, Phase 로드맵, 소스 구조, 주의사항 포함)
- `SESSION_LOG.md` 초기 파일 생성

**다음 세션 TODO:**
- [ ] Phase 2 진행 상태 확인 (SettingsScreen, SettingsViewModel, NavGraph 완성도 점검)
- [ ] MissionFailReceiver.kt — ViewModel 연동 구현
- [ ] 부팅 후 AlarmReceiver에서 AlarmScheduler 재등록 처리
