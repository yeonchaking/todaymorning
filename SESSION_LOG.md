# SESSION_LOG — 오늘도출근

> "세션 종료" 시 1회만, 이 파일 **맨 위에** 항목을 추가한다 (최신 순).
> **살아있는 TODO는 `TODO.md`에만 있다.** 세션로그의 `[TODO 리스트 변경]`은 그 세션이 백로그를 어떻게 바꿨는지의 이력일 뿐이다.
> 오래된 항목은 `SESSION_LOG_ARCHIVE.md`로 이동(최근 5개만 유지). 작성 규칙은 `CLAUDE.md` "세션 운영" 참고.

---

## [2026-07-08] 버스 도착정보 전면 불통 해결 — 서버 개편으로 serviceKey 파라미터명이 대소문자 엄격 판정으로 바뀜

**방향/목표:** 최근(운영계정 전환 시점 전후)부터 버스 도착정보가 전혀 안 오는 문제의 원인 규명과 수정.

**결정 사항:**
- **원인은 키(운영계정 전환)가 아니라 서버 개편으로 판정** — 이유: 브라우저 재현 결과 대문자 `ServiceKey`는 401 `{"error":"Unauthorized","message":"serviceKey 파라미터가 필요합니다."}`(신규 게이트웨이식 JSON 에러), 소문자 `serviceKey`는 같은 키로 정상 JSON. 즉 ws.bus.go.kr가 파라미터명을 소문자만 인식하도록 바뀐 것. 기존 키는 그대로 유효.
- **수정 범위는 `BusApiService.kt` 파라미터명 3곳으로 한정** — 이유: 키를 쿼리로 보내는 곳은 이 파일뿐(지하철 API는 URL 경로에 키 포함 방식이라 무관, 실호출로 정상 확인).

**코드/프로젝트 변화:**
- `BusApiService.kt` — `@Query("ServiceKey")` → `@Query("serviceKey")` 3곳(getArrivalByStationId / getStationByPos / getStationByName). 실기기 로그캣으로 200 OK + `headerCd:0` + 도착 데이터 수신 확인, 사용자 화면 표시도 확인.
- 진단 과정에서 확인된 사실 2가지(코드 변경 없음):
  - ws.bus.go.kr는 해외 IP를 차단하는 것으로 보임(샌드박스에서 빈 응답) — 원격 환경에서 버스 API 직접 검증은 불가, 브라우저/실기기로 확인해야 함.
  - 수정 직후 "여전히 안 됨"으로 보였던 건 심야 시간대라 선택 노선이 **운행종료**였기 때문(정상 동작 — `TransitRepository`가 운행종료/출발대기를 걸러냄). 다른 노선으로 바꾸니 정상 표시. 기존 TODO "막차/첫차 계산 → 운행종료 안내"가 바로 이 혼동을 없애는 작업.

**[TODO 리스트 변경]:**
- 해결: 없음 (백로그 외 긴급 버그 수정)
- 추가: 없음

---

## [2026-07-06] 오류처리·엣지케이스 조사 정리 — 실제 수정은 Phase 2로, 안내 토스트는 유지 결정

**방향/목표:** `TODO.md`의 "오류·예외 처리 표준화", "엣지케이스", "🔊 안내 토스트 정리" 세 항목을 이번 세션에서 다루기로 합의했으나, 실제 코드 작업은 Phase 2로 미루고 대신 각 항목의 케이스·현재 코드 동작·수정 방향을 문서로 남기는 쪽으로 스코프를 좁혔다.

**결정 사항:**
- **오류·예외 처리 표준화 / 엣지케이스 — 코드 변경 없이 조사 결과만 문서화, 실제 구현은 Phase 2** — 이유: 사용자가 "다 phase2에서 할래, 대신 케이스는 정리해두고 싶다"고 스코프를 명시적으로 좁힘.
- **🔊 안내 토스트(`MissionEngine.maybeAnnounce`)는 그대로 유지, 코드 변경 없음** — 이유: 확인 결과 이 토스트가 실제 TTS 발화(`tts?.announce(a)`)와 같은 조건 블록에서 동시에 뜨는 것으로 드러남 — 음성 안내가 나오는 순간 화면에도 같은 문구를 잠깐 띄우는 용도이므로, 개발자모드 게이트로 묶거나 제거할 필요가 없다고 판단.

**코드/프로젝트 변화:**
- 신규 `오류_예외처리_표준화_케이스정리.md` — "도착정보 없음(정상)" / "네트워크 실패" / "운행종료" 세 케이스가 지금은 `TransitRepository`의 `catch { emptyList() }`에서 전부 뭉개져 구분 불가능하다는 점을 코드 위치(L20-39, L119-143, L146-155)와 함께 정리. `MissionEngine.fetchArrivals`의 기존 `catch (e: Exception)` 분기가 사실상 거의 발동하지 않는 죽은 코드나 다름없다는 점도 확인. Phase 2 구현 방향(에러 매핑 유틸 신설, 예외를 삼키지 않고 던지도록 변경)까지 명시.
- 신규 `엣지케이스_정리.md` — 세 하위 케이스 조사 결과: **자정 넘김은 실제 버그**로 확인(`MissionEngine.startCountdown()`이 목표 시각을 항상 "오늘 날짜"로만 계산해, 알람 23:50/목표 00:10처럼 자정을 걸치는 조합이면 시작하자마자 목표가 23시간 넘게 지난 것으로 잘못 계산됨 — `AlarmScheduler.nextDailyTrigger`엔 이미 있는 롤오버 처리가 `MissionEngine` 쪽엔 빠져 있어 생긴 불일치). 수정 방향(임계값 3시간 롤오버)까지 문서에 코드 스니펫으로 남김. **미션 미설정 상태로 알람**과 **목표 시각 직전 진입**은 코드 추적 결과 기존 로직으로 이미 정상 동작하는 것으로 확인 — 둘 다 변경 불필요.
- `TODO.md` 갱신 — 위 두 항목에 정리 문서 링크 반영(실제 구현은 Phase 2로 태그), 🔊 안내 토스트 항목은 결정 반영해 완료 처리.
- **코드(런타임 동작) 변경 없음** — 이번 세션은 조사·문서화만 진행.

**[TODO 리스트 변경]:**
- 해결: 🔊 안내 토스트 정리 (유지 결정으로 종결)
- 추가: 없음 (오류·예외 처리 표준화 / 엣지케이스 두 항목은 기존 TODO에 "Phase 2 + 정리 문서 링크"로 갱신, 신규 항목 아님)

---

## [2026-07-06] Play Store 업로드 준비 착수 — 백로그 정리 + 실사용 버그 발견/수정

**방향/목표:** 구글 플레이 콘솔 업로드를 앞두고 `TODO.md` 백로그 전체를 다시 훑어 "지금 할 것 / 다음 세션 / P3 보류 / 외부(Play Console) 작업"으로 재분류했다. 그 과정에서 실사용 버그(목표 시각 경과 시 TTS·폴링·서비스가 조기 종료되는 문제)를 발견해 같이 고쳤다.

**결정 사항:**
- **지하철 미션 직접선택 화면 / 막차·첫차 계산 반영은 다음 세션(Phase 2)으로** — 이유: 이번 세션 범위를 업로드 준비 항목 위주로 좁힘.
- **타임어택 신호등 정밀화 / 플로팅 위젯 전면 숨기기는 P3(낮은 우선순위)로 보류** — 이유: 신호등은 막차 계산과 연동돼야 의미가 있어 그 작업 이후로, 플로팅 위젯 숨기기는 "필요없는 기능 같다"는 판단.
- **`foregroundServiceType=mediaPlayback`(AlarmRingService)/`dataSync`(MissionService) 둘 다 유지, 코드 변경 없음** — 이유: mediaPlayback은 실제 알람음 재생 용도와 정확히 부합, dataSync도 "주기적 데이터 조회"라는 공식 정의와 일치하고 Android 15 하루 6시간 제한도 출퇴근 미션 실사용 패턴에 문제없음. Play Console 제출 시 사유 문구만 명확히 쓰기로.
- **"성공 시 자동실패 취소" 로직은 지금 구현하지 않고 주의사항으로만 유지** — 이유: `cancelMissionFail()`을 성공 시점에 호출하도록 바꾸면, 그 자리에 얹혀 있는 "다음날 자동실패 재등록"까지 같이 사라져 체인이 끊기는 리스크가 있음. 지금 굳이 손댈 이유가 없어 보류.
- **API 키 트래픽 한도는 개발계정 → 운영계정 전환 + 증설 신청으로 해결** — 이유: 서버 없이 클라이언트가 공공 API를 직접 호출하는 구조라 모든 사용자가 앱에 내장된 같은 키를 공유함. 클로즈드 테스트(테스터 12명) 단계부터 개발계정 기본 한도(기능별 일일 1,000건)를 넘길 수 있다고 계산돼 미리 처리.

**코드/프로젝트 변화:**
- 앱 이름 한글화(`strings.xml` app_name → "오늘도출근"), 릴리즈 OkHttp 로깅 레벨을 `BuildConfig.DEBUG` 분기로 BASIC 환원(`NetworkModule`).
- 스플래시 화면 신규 도입 — `core-splashscreen` + `res/drawable/splash_icon.xml`(런처 아이콘과 별개 리소스, 로고 교체 시 이 파일만 바꾸면 됨) + `Theme.Todaymorning.Splash`(라이트/다크 배경 분리) + `MainActivity.installSplashScreen()`.
- 상태별 UI 일관성 — `RouteSelectScreen`/`BusSelectScreen`(노선 바텀시트)/`LocationPickerScreen`(주소검색)이 로딩·에러·빈상태에서 M3 기본색을 쓰고 있어 `TimeAttackScreen` 등과 어긋나 있던 걸, 신규 `ui/common/StateViews.kt`(`SectionLoading`/`InlineLoading`/`EmptyStateText`/`ErrorStateText`)로 통일.
- **버그 수정**: `MissionEngine.startCountdown()`이 목표 시각 경과 즉시 `markFinished()`를 호출해 `startPolling()`(도착 폴링·TTS)과 `MissionService.watchFinished()`(서비스 자체 종료)를 동시에 멈추던 문제. 이 앱은 목표 시각을 살짝 넘겨 도착하는 차편도 "타면 성공"으로 인정해 하단 액션이 성공/실패 수동 선택으로 바뀌는 설계인데, 정작 그 순간 도착정보·TTS·서비스가 죽어 있었음. `remaining <= 0`일 때는 카운트다운만 멈추도록 수정, 실제 종료는 `onBoardingSuccess()`/`onMissionFail()`(사용자의 직접 선택)에서만 일어나게 함.
- 신규 문서 `구글 플레이 콘솔 업로드 준비(오늘도출근).md` — 앱 내부/외부(Play Console) 작업 구분, API 키 트래픽 정책 상세, 권한/서비스타입 심사 제출 문구 초안, 추천 순서.
- **빌드 미검증** — 샌드박스에 Android SDK 없음(정적 참조·import 정합성만 확인). 실기기/Android Studio 빌드 확인은 사용자 환경에서.

**[TODO 리스트 변경]:**
- 해결: 앱 이름 한글화 / 스플래시 화면 / 상태별 UI 일관성 점검 / OkHttp 로깅 레벨 환원 / `foregroundServiceType` mediaPlayback·dataSync 적절성 재검토(유지 결정) / API 키·한도 정책 확정(운영계정 전환) / 목표 시각 경과 시 TTS·폴링·서비스 조기 종료 버그
- 추가: 없음(기존 항목 재분류만 — 지하철 화면·막차계산은 "다음 세션" 태그, 신호등 정밀화·플로팅위젯 숨기기는 "P3" 태그)

---

## [2026-07-01] 권한 온보딩 게이트 도입 — 앱 실행마다 3종 권한 하드 체크

**방향/목표:** "권한 온보딩 흐름"(TODO 1.기능) 해소. 정확한 알람·전체화면 인텐트·알림 권한이 미허용이면 알람이 조용히 무력화되는데, 기존엔 메인 화면의 배너 하나(그마저도 재계산 안 되는 버그 있음)뿐이었다. 이걸 앱 시작 시점의 하드 게이트 화면으로 대체한다.

**결정 사항:**
- 정확한 알람·전체화면 인텐트·알림 **셋 다 하드 게이트**(스킵 버튼 없음) — 이유: 사용자가 명시적으로 "다 ㄱㄱ"로 결정. 처음엔 정확한 알람만 하드게이트하고 나머지는 스킵 허용하는 절충안을 제안했으나 기각됨.
- 온보딩 재진입 기준은 "1회성 플래그"가 아니라 **앱을 새로 열 때마다 권한 상태 재평가** — 이유: 사용자가 "권한 미허용 시 매번 진입"을 선택. 배터리 최적화 등으로 나중에 권한이 회수돼도 다음 실행에서 다시 걸린다.
- 지하철 미션 직접선택 화면은 이번 세션 범위에서 **제외** — 이유: 사용자가 "권한 온보딩만" 하자고 스코프를 좁힘(지하철은 지도 여부 등 추가 논의 필요해 다음 세션으로).
- 기존 `MainScreen`의 `PermissionBanner`(정확한 알람만 다루고, `remember`라 설정 갔다 와도 재계산 안 되던 버그 있음) **제거**하고 온보딩 게이트로 대체 — 이유: 온보딩이 진입 시점에 이미 3종 다 걸러주므로 중복.
- `fromAlarm`(알람이 직접 연 경우)은 온보딩으로 가로채지 않음 — 이유: 알람이 울렸다는 건 최소한 정확한 알람 권한은 있었단 뜻이라, 기존 타임어택 진입 흐름을 그대로 존중.

**코드/프로젝트 변화:**
- 신규 `ui/onboarding/PermissionOnboardingScreen.kt` — 정확한 알람/전체화면 표시(Android14+)/알림(Android13+) 3단계 카드. 설정 화면 다녀오면 콜백이 없는 두 권한(정확한 알람·전체화면 인텐트)을 위해 `ON_RESUME`마다 재확인. 게이트 조건 재사용을 위해 `allOnboardingPermissionsGranted(context)` 공개 함수 노출.
- `NavGraph.kt` — `Routes.ONBOARDING` 추가. `fromAlarm`이 아니면 시작화면을 `allOnboardingPermissionsGranted` 결과로 분기(전부 허용 시 MAIN, 아니면 ONBOARDING).
- `MainActivity.kt` — `onCreate`에서 무조건 실행되던 전체화면 인텐트 강제 설정이동 + 알림권한 자동요청 로직 제거(온보딩으로 위임). `MainScreen`의 `PermissionBanner`·`showPermissionBanner`·관련 미사용 `context`/import 정리.
- **빌드 미검증** — 샌드박스에 Android SDK 없음(정적 참조·import·브레이스 정합성만 파일 재독으로 확인). 사용자가 Android Studio에서 빌드 확인 후 직접 커밋 예정.

**[TODO 리스트 변경]:**
- 해결: "권한 온보딩 흐름" (1. 기능)
- 추가: 없음

---

## [2026-07-01] 히든 개발자모드 신설 + 진단 토스트 첫 적용

**방향/목표:** 개발용 기능을 일반 사용자에게는 숨기고 필요할 때만 켤 수 있는 "개발자모드(isDev)" 스위치를 프로젝트 전반에 깔고, 이미 화면에 노출되던 진단용 토스트 중 하나를 그 첫 적용 대상으로 게이트했다.

**결정 사항:**
- 트리거를 메인 화면 타이틀 "오늘도출근" 10연속 탭으로 결정 — 이유: 설정 화면 등에 노출하면 일반 사용자도 우연히 발견할 수 있어 "히든" 의미가 없어짐.
- `isDevMode`를 별도 저장소 대신 기존 `UserSettings`/`UserSettingsDataStore` 파이프라인에 편입 — 이유: `MissionEngine`처럼 이미 `dataStore.userSettings`를 읽는 곳에서 별도 배선 없이 바로 조건 검사 가능.
- 진단 토스트 두 개(⏱ 폴링 상태, 🔊 안내 발화) 중 이번엔 ⏱ 폴링 토스트만 게이트 — 이유: 사용자가 지목한 범위가 폴링 토스트였음. 🔊 토스트는 범위 밖으로 남겨둠(아래 TODO 참고).

**코드/프로젝트 변화:**
- `domain/model/UserSettings.kt` — `isDevMode: Boolean = false` 필드 추가.
- `data/datastore/UserSettingsDataStore.kt` — `IS_DEV_MODE` 키, `userSettings` flow 매핑, `saveSettings()` 반영, 부분저장 `saveDevMode()` 추가.
- `ui/MainActivity.kt` — `TopHeader`의 타이틀 `Text`에 탭 카운터(`rememberSaveable` 아닌 `remember` — 프로세스 재시작 시 리셋) 추가, 10회 도달 시 콜백 호출 후 리셋.
- `ui/main/MainViewModel.kt` — `toggleDevMode()` 추가: `isDevMode` 반전 저장 + "개발자모드가 설정/해제되었습니다" 토스트. `@ApplicationContext Context` 신규 주입.
- `mission/MissionEngine.kt` — `maybeAnnounce`의 `⏱ N대·최단 ◯◯s...` 폴링 디버그 토스트를 `settings.isDevMode`일 때만 노출하도록 감쌈. `🔊 N분 전 안내` 토스트는 변경 없음(항상 노출).
- **빌드 미검증** — 샌드박스에 Android SDK 없음(정적 참조 일치만 확인). 실기기/Android Studio 빌드 확인은 사용자 환경.

**[TODO 리스트 변경]:**
- 해결: 없음
- 수정: 기타 작업의 "TTS 디버그 토스트 제거" 항목을 범위 축소 — ⏱ 폴링 토스트는 개발자모드 게이트로 해결됐으므로, 남은 "🔊 안내 토스트 정리" 여부만 별도 결정 대상으로 남김.

---

## [2026-06-29] 미션 백그라운드 상시 동작(포그라운드 서비스) + 노티/플로팅 위젯 + 알람 즉시 표시

**방향/목표:** 화면을 꺼도 미션이 멈추던 구조적 한계(viewModelScope 루프가 Activity 생명주기에 묶임)를 풀고, 미션 진행 상태를 화면 밖에서도 보고/조작할 수 있게 만든다. 사용자 시나리오: 알람 후 폰을 보며 뒹굴다 준비하고, 화면을 다시 켰을 때 미션이 그대로 살아 있어야 한다. 음성안내·알림·플로팅 위젯 세 경로로 노출. 추가로 알람이 폰 사용 중에도 즉시 뜨도록, 메인 시각 표시가 변경을 못 따라오던 것도 수정.

**결정 사항:**
- **미션 로직을 화면에서 떼어 `MissionEngine`(@Singleton)으로, 호스팅은 `MissionService`(포그라운드)** — 이유: 폴링·카운트다운·TTS·기록이 ViewModel viewModelScope에 있으면 화면 STOP/Doze 시 멈춤. 엔진을 싱글톤으로 두고 포그라운드 서비스가 살리면 화면과 무관하게 10초 폴링이 지속됨. ViewModel은 엔진 위임형 얇은 어댑터로 축소.
- **서비스 종료는 엔진이 판단(finished) → 서비스 stopSelf** — 이유: 성공/실패/목표경과 시점을 엔진이 알고, 서비스는 그 신호만 보고 내려가게 해 책임 분리. 기존 매일 자동실패 재등록 체인(MissionFailReceiver)은 의존성 때문에 손대지 않음.
- **foregroundServiceType=dataSync** — 이유: 주기 네트워크 갱신이 핵심 행위. 단 Play 심사 사유 제출 대상 + Android15 6시간/일 제한(아침 미션엔 충분) → 신규 TODO로 검토 예약.
- **TTS '미션 시작 게이트'를 ttsTimings 와 분리(ttsLeadMinutes, 기본 15)** — 이유: 기존 ttsTimings={10,5,3}은 '각 차편 도착 N분 전' 의미. "막 40분 전부터 들을 필요 없다"는 요청은 '미션 전체 음성안내 시작 시점'이라 별도 개념. 목표 lead분 전부터만 발화.
- **TTS 진단 정보(initStatus/isReady) 토스트 노출** — 이유: 사용자 테스트에서 "음성 안 나옴"이 트리거 미발생인지 엔진(한국어 데이터)인지 게이트인지 구분 불가였음. ⏱ 토스트에 엔진상태·게이트(열림/닫힘) 표기 → 원인이 게이트였음을 사용자와 함께 특정.
- **알림 남은시간은 시스템 크로노미터(카운트다운)** — 이유: 1초마다 재notify 없이 시스템이 갱신 → 배터리/깜빡임 회피. 다음 버스 텍스트만 주기 갱신.
- **알림/위젯 도착정보 갱신을 emission 구독 대신 주기 폴링(현재값 직접 읽기)으로** — 이유: collectLatest 가 첫 빈 값만 반영하고 이후 갱신을 못 받던 증상. 엔진 .value 를 주기적으로 읽어 그리니 emission 타이밍/conflation 무관하게 안정적.
- **버스 2대 두 줄 표시** — 이유: 사용자 요청. 위젯은 멀티라인 TextView, 알림은 BigTextStyle(접힘=첫 줄, 펼침=두 줄, 접힌 알림은 한 줄 한계).
- **플로팅 위젯은 전통 View(WindowManager TYPE_APPLICATION_OVERLAY)** — 이유: 오버레이용 Compose LifecycleOwner 세팅이 번거로움. 드래그 이동 + (slop 이내) 탭 시 앱 진입. 투명도는 root.alpha 로 위젯 전체 적용.
- **위젯 on/off·투명도를 미션 화면에서 제어(DataStore 저장, 서비스가 구독)** — 이유: 사용자 요청. 토글/투명도 변경이 1초 내 위젯에 반영. 권한 미허용 시 허용 유도.
- **알람 시 AlarmReceiver 에서 액티비티 직접 실행** — 이유: 풀스크린 인텐트는 잠금/화면꺼짐에만 즉시 동작, 폰 사용 중엔 헤드업만. setAlarmClock 정확알람 브로드캐스트는 잠깐 BAL 허용되므로 거기서 AlarmRingActivity 직접 startActivity → 사용 중에도 슬라이드 화면 즉시 표시. 풀스크린 인텐트는 백업으로 유지.
- **메인 settings 를 Lazily + collectAsStateWithLifecycle** — 이유: WhileSubscribed(5s)면 설정 화면에 오래 머문 뒤 복귀 시 옛 캐시값이 잠깐 보임. Lazily 로 .value 를 항상 최신 유지 + 복귀(ON_START) 재수집.

**코드/프로젝트 변화:**
- 신규 `mission/MissionEngine.kt`(@Singleton, 폴링·카운트다운·TTS·기록·15분 게이트), `mission/MissionOverlay.kt`(오버레이 위젯), `alarm/MissionService.kt`(포그라운드 호스트+알림+위젯 루프).
- `ui/timeattack/TimeAttackViewModel.kt` — 엔진 위임형으로 재작성, 진입 시 서비스 기동, 위젯 토글/투명도 setter.
- `ui/timeattack/TimeAttackScreen.kt` — 플로팅 위젯 토글+투명도 슬라이더+권한 유도.
- `domain/model/UserSettings.kt`/`data/datastore/UserSettingsDataStore.kt` — ttsLeadMinutes, floatingWidgetEnabled, floatingWidgetOpacity 추가·저장.
- `ui/SettingsViewModel.kt`/`SettingsScreen.kt` — setTtsSettings(lead) 시그니처, '미션 음성안내 시작' 칩 + '차편별 안내 시점' 분리.
- `alarm/TtsManager.kt` — isReady/initStatus 노출.
- `alarm/AlarmReceiver.kt` — 알람화면 직접 실행 추가.
- `ui/main/MainViewModel.kt`/`ui/MainActivity.kt` — settings Lazily + collectAsStateWithLifecycle.
- `AndroidManifest.xml` — MissionService 등록, FOREGROUND_SERVICE_DATA_SYNC, SYSTEM_ALERT_WINDOW.
- **빌드 미검증** — 샌드박스에 Android SDK/JDK17 없음(정적 참조 일치만 확인). 실기기 빌드·동작 확인은 사용자 환경.

**[TODO 리스트 변경]:**
- 해결: "타임어택 백그라운드 동작 (포그라운드 Service 이전)" — 미션 엔진/서비스로 닫음.
- 추가: (UI) 플로팅 위젯 — 우리 앱이 전면일 때 숨기기(현재 미션 화면 위에도 겹쳐 뜸); (제출 게이트) SYSTEM_ALERT_WINDOW 권한 Play 사유 제출; (제출 게이트) MissionService dataSync 타입 적절성 재검토.

---

## [2026-06-21] TTS 음성 안내 구현 — '버스 도착 기준 · 모든 차편' 트리거로 정착

**방향/목표:** v2 placeholder였던 설정 "음성 안내(TTS)" 그룹(rememberSaveable 로컬 상태일 뿐 저장·동작 전무)을 실제 동작으로 닫음. 안드로이드 내장 TextToSpeech를 붙여, 타임어택 중 차편이 임박할 때 음성으로 읽어주도록 함. 알람음·진동과 같은 "registry/DataStore 부분저장 + 사용처에서 읽음" 구조를 재사용.

**결정 사항:**
- **트리거 기준을 '목표 시각'이 아니라 '버스 도착 시간'으로 변경** — 이유: 사용자 테스트에서 "버스가 5분 후 도착"일 때 울리길 기대. 출근 앱에선 "버스 5분 후 도착, 지금 나가라"가 데드라인 카운트다운보다 행동가능(actionable)함. 최초 구현(목표시각까지 10·5·3분)을 폐기하고 도착초 기준으로 재작성.
- **'가장 가까운 1대'가 아니라 리스트의 '모든 차편' 각각 발화** — 이유: 사용자 요청("설정된 모든 버스, 리스트에 떠있는 모든 버스"). 차편을 `노선명|방면|동일그룹순번`으로 식별(노선당 1·2번째 도착 구분), 차편별로 직전 도착초를 추적해 하향 통과 시 발화. 같은 차편의 같은 시점은 `spokenMarks`로 1회만.
- **도착초 < 0(정보없음/운행종료) 차편 제외** — 이유: -1이 오름차순 정렬상 맨 앞에 와서 '최단'으로 오판되어 트리거를 오염시킴. 유효 차편(≥0)만 대상으로.
- **발화 문장에서 `[3번째 전]` 꼬리표 제거, 남은시간 접두사 없음** — 이유: 사용자 요청. "651번 버스가 5분후 도착 예정입니다"만. arrivalMessage의 `[` 이후를 잘라냄. 1분 이내는 "곧 도착합니다".
- **발화 시 짧은 진동 동반(폴링당 1회) + 디버그 토스트** — 이유: 사용자 요청 + 트리거 동작이 눈에 안 보여 진단이 어려웠음. `⏱ N대 · 최단 ◯◯s`(폴링마다)와 `🔊 N분 전 안내`(트리거 시)로 가시화. 토스트는 검증용 임시물(신규 TODO로 제거 예약).
- **TtsManager를 @Singleton으로 두지 않음** — 이유: TimeAttackViewModel onCleared에서 shutdown하므로, 싱글톤이면 재진입 시 죽은 엔진 재사용됨. ViewModel마다 새 인스턴스.
- **여러 건 동시 발화는 QUEUE_ADD** — 이유: QUEUE_FLUSH면 앞 안내를 끊음. 한 폴링에 여러 대가 임계를 지나면 순차 재생.

**코드/프로젝트 변화:**
- 신규 `alarm/TtsManager.kt` — TextToSpeech(한국어) 래퍼. `sentenceFor`(순수 함수)에 대본 로직 집중, 엔진 미준비/언어 미지원 시 전부 무음 fallback.
- `domain/model/UserSettings.kt` — `ttsEnabled`, `ttsTimings:Set<Int>` 필드 추가.
- `data/datastore/UserSettingsDataStore.kt` — `TTS_ENABLED`/`TTS_TIMINGS`(CSV) 키, 전체 저장 반영, `saveTtsSettings` 부분저장.
- `ui/SettingsViewModel.kt`/`SettingsScreen.kt` — 토글·시점칩을 저장값과 바인딩, 즉시 부분저장(로컬 rememberSaveable 제거).
- `ui/timeattack/TimeAttackViewModel.kt` — `@ApplicationContext` 주입, `maybeAnnounce()`를 fetchArrivals(10초 폴링)에 연동, 차편별 추적(`lastSecondsByKey`/`spokenMarks`), `vibrate()`/`showToast()` 헬퍼, onCleared에서 ttsManager.shutdown. 카운트다운 루프에서는 TTS 분리(목표시각 기준 폐기).
- **빌드는 미검증** — 이 세션 샌드박스에 Android SDK/JDK17 없음(정적 참조 일치만 확인). 실기기 빌드·동작 확인은 사용자 환경.

**미해결/이관:** 화면이 꺼지면 폴링·카운트다운·TTS가 멈추는 구조적 한계 확인됨(viewModelScope 루프가 Activity 생명주기에 묶임). 포그라운드 Service 이전이 TTS 실사용의 전제 → 신규 TODO.

**[TODO 리스트 변경]:**
- 해결: v2 미구현 [0] "TTS 음성 안내"
- 추가: (기능) "타임어택 백그라운드 동작 (포그라운드 Service 이전)" / (기타) "TTS 디버그 토스트 제거"
