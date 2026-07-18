# SESSION_LOG_ARCHIVE — 오늘도출근

> `SESSION_LOG.md`에서 밀려난 오래된 세션 기록 (최신 순).
> **역사적 참고용**이다. 여기 적힌 TODO는 살아있는 할 일이 아니다 — 현재 할 일은 `SESSION_LOG.md` 맨 위 항목에서만 본다.

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

---

## [2026-06-21] 진동을 on/off 토글 → 패턴 선택 방식으로 전환

**방향/목표:** v2 placeholder였던 설정 "진동" 토글(rememberSaveable 로컬 상태일 뿐 저장 안 됨)을 실제 동작으로 닫음. 동시에 단순 on/off 대신 사용자가 진동 세기/리듬을 고를 수 있게 "패턴 선택" UX로 확장. 직전 세션의 알람음 선택과 **같은 구조를 그대로 재사용**해 일관성 확보.

**결정 사항:**
- **on/off 토글이 아니라 패턴 선택 다이얼로그로 설계** — 이유: 사용자 요청. 진동 없음/짧게/기본/길게/두 번씩 등 여러 리듬을 제공하고, 탭하면 그 진동을 실제로 느껴본 뒤 확인으로 확정하는 게 "진동을 끄거나 켜는" 것보다 실용적. OFF도 패턴의 한 항목으로 흡수(별도 boolean 불필요).
- **vibrationPatternId 단일 문자열 + VibrationPatterns registry** — 이유: 알람음(`alarmSoundId`/`AlarmSounds`)과 완전히 동일한 패턴. 패턴 추가는 `VibrationPatterns.PATTERNS`에 한 줄이면 다이얼로그에 자동 노출, 다른 코드 수정 불필요. 해석(waveform 변환·OFF 판정·라벨)을 한 곳에 모음. 기본값 `"basic"`(1초 패턴)이라 기존 사용자 동작 불변.
- **서비스가 울릴 때 DataStore를 직접 읽음** (Hilt 미적용 → `runBlocking.first()`) — 이유: 알람음과 동일. 진동 패턴 변경 시 알람 재등록 불필요, "울리는 순간"에만 필요하므로 그 시점에 읽음.
- **확인 버튼에서만 commit (다이얼로그는 임시 선택값 보유)** — 이유: 알람음은 탭 즉시 저장이지만, 진동은 탭=미리보기·확인=저장으로 분리. 여러 패턴을 느껴보다 취소하면 원래 값 유지되는 게 자연스러움. 요일반복 다이얼로그와 같은 commit 방식.

**코드/프로젝트 변화:**
- `alarm/VibrationPatterns.kt` 신규 — `PatternDef(id,label,waveform)` 목록 + `waveformOf`/`isOff`/`label`. (off/short 0.5s/basic 1s/long 5s/double)
- `UserSettings`에 `vibrationPatternId="basic"` 추가. `UserSettingsDataStore`에 `VIBRATION_PATTERN_ID` 키 + 읽기/saveSettings + `saveVibrationPattern` 부분저장. `SettingsViewModel.setVibrationPattern`.
- `AlarmRingService.startVibration` — 하드코딩 `longArrayOf(0,1000,1000)` 제거, 저장된 패턴 읽어 재생. OFF면 진동 시작 안 함(early return).
- `SettingsScreen` — `ToggleRow("진동")` + 죽은 `vibrate` 로컬 상태 제거 → `NavRow`(현재 패턴 라벨) + `VibrationPatternDialog`(탭 시 Vibrator로 미리보기 반복, 닫힐 때 cancel, 확인 시 저장). `SoundRow` 재사용.
- 빌드 함정: `SettingsScreen`에 `android.content.Context` import 누락으로 실기기 빌드 실패 → 추가로 해결. (샌드박스 빌드는 캐시 때문에 못 잡음 — 검증 시 `--rerun-tasks` 필요했음.)

**[TODO 리스트 변경]:**
- 해결: [0] "진동 on/off 저장·제어" (패턴 선택 방식으로 구현 완료)
- 추가: 없음

---

## [2026-06-21] 알람음 선택 기능 — 시스템 음원 선택 + 내장음원 확장 구조

**방향/목표:** v2 placeholder였던 설정 "알람음" 행(누르면 "준비 중" 스낵바만)을 실제 동작으로 닫음. 알람음이 폰 시스템 기본음 고정이라 기종마다 달랐던 문제를 사용자가 직접 고를 수 있게 전환. "지금은 음원을 새로 만들지 않고, 폰 음원 선택 + 내장음원을 나중에 쉽게 추가할 수 있는 구조만 준비"로 범위 합의.

**결정 사항:**
- **alarmSoundId 단일 문자열 규칙으로 음원 종류를 표현** (`""`=시스템 기본, `"builtin:<key>"`=내장, 그 외=시스템 ringtone `content://` URI) — 이유: DataStore에 enum+값 두 키로 나누지 않고 한 문자열로 직렬화하면 저장·해석이 단순. 해석은 `AlarmSounds` 한 곳에 모음.
- **내장음원은 registry(`AlarmSounds.BUILT_INS`)로 분리, 지금은 빈 목록** — 이유: `res/raw`에 파일 넣고 한 줄 등록하면 UI에 자동 노출되도록 해, 음원 추가 시 다른 코드 수정 불필요. 음원 합성은 보류(사용자 요청).
- **서비스가 울릴 때 DataStore를 직접 읽음**(Hilt 미적용 → `UserSettingsDataStore(applicationContext)` + `runBlocking.first()`) — 이유: 알람음 변경 시 알람 재등록이 불필요. 선택값은 "울리는 순간"에만 필요하므로 그 시점에 읽는 게 정합성 단순.
- **"최근 선택한 알람"을 현재 선택값과 별도 키(`lastPickedSoundId`)로 보존** — 이유: 휴대폰에서 고른 음원이, 이후 기본/내장으로 선택을 바꿔도 다이얼로그에서 사라지지 않아야 "리스트에서 없어졌다"는 오류 같은 UX가 사라짐. "휴대폰에서 고르기" 버튼과 같은 박스에 묶어 그룹으로 표시.
- **NavRow 값 텍스트 marquee 옵션 추가** — 이유: 긴 음악 제목("Over the Horizon by SUGA of BTS")이 제목 "알람음"을 세 줄로 밀어냄. 제목 1줄 고정 + 값은 넘치면 한 줄로 흐르게.

**코드/프로젝트 변화:**
- 신규 `alarm/AlarmSounds.kt` — 음원 카탈로그 + id 규칙 해석 단일 지점(라벨·기본URI·내장 조회).
- `UserSettings` + `UserSettingsDataStore` — `alarmSoundId`/`lastPickedSoundId` 필드·키 + `saveAlarmSound()`·`savePickedRingtone()`.
- `AlarmRingService.startSound()` — 저장된 선택값을 읽어 기본/내장/시스템으로 분기 재생, 실패 시 기본음 폴백. 내장음 fd(`rawAfd`)는 정지 시점까지 보관.
- `SettingsScreen` — 알람음 행이 현재 선택 라벨 표시 + 선택 다이얼로그(기본/내장 라디오 + 미리듣기 + 시스템 ringtone picker 런처 + "최근 선택한 알람" 묶음). `SettingsViewModel`에 저장 메서드.
- `CLAUDE.md` — "작업 착수 원칙(계획 승인 후 개발)" 항목을 세션 운영 맨 위에 추가.

**[TODO 리스트 변경]:**
- 해결: `0. v2 UI 미구현`의 **알람음 선택**
- 추가: 없음

**방향/목표:** 클로드 디자인이 임의로 넣은 홈 초록 CTA가 타임어택 화면으로 이동만 하던 것을, "메인에서 바로 다음차 도착 시간을 확인"하는 실용 기능으로 바꿈. v2 placeholder였던 "홈 다음버스 실시간 미리보기"를 닫는 작업.

**결정 사항:**
- **인라인 미리보기 대신 다이얼로그 on-demand 조회** — 이유: 홈에 상시 폴링을 붙이면 배터리/네트워크·생명주기 관리 비용이 큼. 버튼 누를 때만 1회 조회하는 다이얼로그가 "지금 도착정보 잘 들어오나 확인" 목적에 충분.
- **버튼 노출 조건을 `alarmEnabled && hasMissionTarget` → `hasMissionTarget`만으로** — 이유: 도착정보 확인은 알람 ON/OFF와 무관한 행위. 미션(정류장+노선)이 설정돼 있으면 항상 보이고, 설정이 없으면 버튼 자체를 숨겨 오작동 오해 제거.
- **조회 로직은 TimeAttackViewModel.fetchArrivals와 동일 방식 재사용** — 이유: DataStore 실제값 읽기 → 미션타입별 선택 노선 전부 합산 → 도착 빠른 순 정렬. 두 화면이 같은 규칙(아무거나 타면 성공)을 공유해야 일관됨.
- **막차/첫차 반영은 이번 범위 밖 → TODO로 분리** — 이유: 노선별 운행시각 API 연동이 별도 작업. "운행 종료/운행 전" 안내 문구는 후속.

**코드/프로젝트 변화:**
- `ui/main/MainViewModel.kt`: `TransitRepository` 주입, `ArrivalDialogState`(open/loading/arrivals/error) + `openArrivalDialog()`/`closeArrivalDialog()` 추가.
- `ui/MainActivity.kt`: `NextBusCta` 노출 조건을 `hasMissionTarget`로 변경, onClick을 다이얼로그 열기로 교체. `ArrivalDialog`/`ArrowButton`/`ArrivalCardBody` 컴포저블 신규 — 도착 1건 카드(노선/방면/N분 후, 버스🚌·지하철🚇), 여러 건은 ‹ › 화살표 + "n / m" 인디케이터, 로딩/빈/에러 처리, 새로고침. (`onStartTimeAttack` 파라미터는 미사용으로 남음 — 경고만.)
- 빈 결과 안내 문구에서 "(운행 종료 또는 API 키 미설정)" 괄호 제거 → "도착 정보가 없습니다."
- (검증 한계) 샌드박스에 Android SDK 없어 풀빌드 대신 색 토큰·import·기존 패턴 정적 확인. 실기기 빌드는 Android Studio에서.

**[TODO 리스트 변경]:**
- 해결: "홈 다음버스 실시간 미리보기"(섹션 0) — 다이얼로그 방식으로 닫음.
- 추가: "막차/첫차 계산 → 실시간 도착정보 반영"(섹션 1 기능) — 운행 종료/전 안내.

---

## [2026-06-20] 알람 ON/OFF·요일 반복 실동작 — 단일 게이트(applyAlarm)로 스케줄 통제

**방향/목표:** v2에서 화면만 있고 동작 없던 placeholder 중 "알람 마스터 스위치"와 "요일 반복" 두 항목을, 두 플래그(`alarmEnabled` + `repeatDays`)를 하나의 조건으로 묶어 실제 알람/자동실패 스케줄 등록·취소까지 연결.

**결정 사항:**
- **요일 게이팅을 런타임 차단이 아니라 "다음 발생 시각 계산"으로 처리** — 이유: 알람은 `setAlarmClock` 1회성이고 매일 반복을 Receiver 재등록으로 흉내내므로, "울린 뒤 꺼진 요일이면 무시"하면 알람음이 한 번 울리는 걸 못 막는다. `nextDailyTrigger(h,m,repeatDays)`가 꺼진 요일을 건너뛴 첫 시각을 반환(없으면 null).
- **활성/요일/취소를 한 진입점 `AlarmScheduler.applyAlarm(settings)`로 통합** — 이유: ON/OFF·요일·시각 변경이 여러 곳(설정 저장, 메인 스위치, 부팅 복원)에서 일어나므로 게이트 로직 분산을 막음. `alarmActive = alarmEnabled && repeatDays.isNotEmpty()` 한 줄로 판정, false면 알람+자동실패 모두 cancel.
- **요일 저장은 비트마스크 Int** — 이유: Set<Int>(Calendar 요일값)을 DataStore에 단순/가역 저장. 키 없으면 평일 기본, mask 0(반복 없음)은 그대로 보존(기본값으로 되돌리지 않음).
- **마스터 스위치 단일 출처를 DataStore로** — 이유: 기존 메인 로컬 `rememberSaveable` 상태라 재시작·재진입 시 실제 스케줄과 불일치. `settings.alarmEnabled`에 바인딩.
- **요일 UI는 프리셋+개별** — 이유: 사용자 선택. 평일/매일/주말 빠른 버튼 + 월~일 7칩 토글.

**코드/프로젝트 변화:**
- `domain/model/UserSettings.kt`: `alarmEnabled`·`repeatDays`(기본 평일) 필드 + `alarmActive`·`repeatDaysLabel` 파생, `WEEKDAYS`/`EVERYDAY`/`WEEKEND` 상수.
- `data/datastore/UserSettingsDataStore.kt`: `ALARM_ENABLED`·`REPEAT_DAYS` 키, `encodeDays`/`decodeDays`, `saveAlarmEnabled`·`saveRepeatDays` 부분저장.
- `alarm/AlarmScheduler.kt`: 요일 인식 `nextDailyTrigger` 오버로드 + `applyAlarm` 게이트, `scheduleDailyAlarm`/`scheduleDailyMissionFail`이 `repeatDays` 인자로 변경(2-arg 제거).
- `alarm/AlarmReceiver.kt`·`MissionFailReceiver.kt`: 재등록을 `alarmActive`일 때만, 요일 반영. 부팅 복원은 `applyAlarm`로 일원화.
- `ui/SettingsViewModel.kt`·`ui/main/MainViewModel.kt`: `setAlarmEnabled`/`setRepeatDays` 추가, `MainViewModel`에 `AlarmScheduler` 주입. 저장 경로가 `applyAlarm` 호출.
- `ui/MainActivity.kt`: 마스터 스위치를 `settings.alarmEnabled`에 바인딩(로컬상태 제거), 미션카드 요일점·스위치 부제를 `repeatDays`로 구동.
- `ui/SettingsScreen.kt`: "요일 반복" 행 → `RepeatDaysDialog`(프리셋+7칩), 라벨은 `repeatDaysLabel`.
- (검증 한계) kotlinc/Android SDK 없어 풀빌드 대신 요일 스킵·비트마스크 왕복·프리셋·빈집합→미등록·오늘포함을 동치 파이썬 로직으로 통과. 실기기 빌드·동작은 Android Studio에서.

**[TODO 리스트 변경]:**
- 해결: `0. v2 UI 미구현`의 "알람 ON/OFF 마스터 스위치 실제 동작", "요일 반복 설정 + 스케줄 반영"
- 추가: 없음

---

## [2026-06-20] Claude Design v2 적용 — 게이미피케이션 폐기, 기능 위주 UI로 재설계

**방향/목표:** 앱의 시각/UX를 "게임형(포인트·레벨·배지·스트릭)"에서 "실용·정보 중심"으로 전환. Claude Design으로 받은 v2 시안(정보 카드형·신호등·그룹 리스트)을 main의 Compose로 옮기되, 신규 기능은 이번엔 UI만 올리고 로직은 다음으로 분리.

**결정 사항:**
- **게이미피케이션 디자인은 폐기가 아니라 분기 보존** — 이유: 한 번 만든 작업을 버리지 않되 main 방향과 섞지 않기 위해. 게임형 리디자인 전체는 `feature/gamification` 브랜치(커밋 250303d)에 두고, main은 v2로 새로 감. (그 커밋에서 Theme.kt가 마운트 lock 문제로 잘려 커밋된 건 사용자가 재커밋으로 복구)
- **시안 색을 M3 슬롯에 욱여넣지 않고 AppColors(CompositionLocal)로 분리** — 이유: 신호등(success/amber/danger)·surface 단계·outline-soft 등이 M3 기본 슬롯에 없음. 라이트/다크 토큰을 그대로 보존하려고 `LocalAppColors` 도입.
- **신규 기능은 UI-first** — 이유: 사용자 선택. 알람 ON/OFF·요일반복·TTS·알람음·진동·지하철추가는 화면만 올리고 동작은 placeholder. 단 **막차 배지·신호등·이번 달 성공 수는 기존 데이터에서 실제 계산**해 진짜로 동작.
- **달력에서 streak 헤더 제거(옵션화)** — 이유: 정보성 방향에 맞춰 "연속 성공 N일" 경쟁 표현을 빼고 월 기록+범례만. `showStreakHeader` 파라미터로 토글.

**코드/프로젝트 변화:**
- `ui/theme/AppColors.kt` 신규 + `Theme.kt` v2 색으로 갱신, `LocalAppColors` 제공.
- `ui/MainActivity.kt`: 메인을 정보 카드형으로 전면 교체(상단바·알람 ON/OFF 스위치·오늘의 미션 카드·도착 CTA·이번 달 성공·달력·최근기록). `onStartTimeAttack` 추가.
- `ui/timeattack/TimeAttackScreen.kt`: 신호등 히어로(잔여시간 기반 여유/곧출발/임박/지남)·실시간 도착+막차 배지·TTS 카드·새로고침으로 재작성. 기존 VM API 유지.
- `ui/SettingsScreen.kt`: 그룹 리스트(알람/출근경로/음성안내)로 재작성. 시각 검증·저장·rememberSaveable 보존.
- `ui/main/MissionCalendar.kt`: `showStreakHeader` 옵션 + 범례 추가. `MainViewModel`: `monthSuccessCount` 파생.
- `NavGraph`: 메인 → 타임어택 연결.
- (검증 한계) 사용자 실기기 빌드 통과 확인(`rememberSaveable` import 누락 1건 수정 후). 막차/신호등 정확도는 도착정보 파싱에 의존 — 실사용 점검은 다음.

**[TODO 리스트 변경]:**
- 해결: 없음 (방향 전환이라 기존 항목 닫힘보다 재정렬)
- 추가: `TODO.md`에 신규 섹션 `0. v2 UI 미구현`(알람 ON/OFF 동작·요일반복·TTS·알람음·진동·홈 다음버스 미리보기) + 기능 "타임어택 신호등 정밀화". 방향 메모(게임 제외, v2)·"미사용 위치 권한 기능확정"으로 갱신. 완료된 "Git 리모트 연결" 제거.

---

## [2026-06-17] 최근 기록 다중선택 삭제(편집) 기능 추가

**방향/목표:** 홈화면 "최근 기록"을 읽기 전용에서 사용자가 잘못된 기록을 직접 정리할 수 있게 전환. 필드 인라인 수정이 아니라, 다중선택 후 일괄 삭제하는 편집 모드로 구현.

**결정 사항:**
- **편집 범위를 "필드 수정"이 아닌 "다중선택 삭제"로 한정** — 이유: 사용자 요청. "편집 버튼 → 항목별 체크박스 → 선택 개수만큼 'n개 삭제'로 버튼 변형 → 일괄 삭제" 흐름을 명시적으로 지정함. 성공/실패·시각 인라인 편집은 범위에서 제외.
- **체크 0개일 때 버튼은 '취소'로 표시** — 이유: 사용자 명세에 없던 편집 모드 탈출 경로가 필요. 0개 선택 상태에서 빠져나갈 방법을 '취소'로 제공.
- **삭제 후 streak만 수동 재계산(`refreshStreak()`)** — 이유: 성공/총합 카운트와 달력은 Room `Flow` 기반이라 자동 갱신되지만, streak은 suspend 1회성 계산이라 삭제 직후 명시 호출 필요.
- **편집 모드에서 체크박스를 24dp Box로 감싸 행 높이 고정** — 이유: `Checkbox` 기본 최소 터치영역(48dp)이 행을 키워 편집 ON 시 아이템 크기가 변하는 문제. Box(24dp)+Checkbox(20dp)로 footprint를 이모지 줄 높이에 맞춰 고정.

**코드/프로젝트 변화:**
- `MissionDao`: `deleteByIds(ids: List<Long>): Int` 쿼리 추가. `MissionRepository`: `deleteRecords(ids)` 래퍼 추가.
- `MainViewModel`: `MainUiState`에 `isEditMode`/`selectedIds` 추가. `toggleEditMode`/`toggleSelection`/`deleteSelected`(삭제→편집 종료→streak 재계산) 핸들러 추가.
- `MainActivity`: 최근 기록 헤더에 편집/취소/n개 삭제 버튼(`when`으로 상태 분기). `MissionRecordItem`에 `isEditMode`/`isSelected`/`onToggleSelect` 파라미터 추가 — 편집 모드 시 행 전체 `clickable` + 고정 크기 체크박스. `items`에 `key = { it.id }` 부여. `clickable` import 추가.
- (검증 한계) 샌드박스에 Android SDK 부재로 Gradle 빌드 미수행. import·참조 정합성만 확인. 실기기 빌드/동작은 다음 단계.

**[TODO 리스트 변경]:**
- 해결: 기능 "최근 기록 편집 기능"
- 추가: 없음

---

## [2026-06-17] 홈화면 미션 기록 달력 도입 — 레벨 대시보드를 달력으로 대체

**방향/목표:** 홈화면의 통계 표현을 "루키 레벨 + 성공률/총기록 대시보드"에서 "월별 미션 기록을 한눈에 보는 달력"으로 전환. 사용자가 과거 출근 성공/실패 이력을 날짜 단위로 확인하게 한다.

**결정 사항:**
- **레벨 대시보드(StatsCard) 전면 제거, 통계는 "현재 연속 성공 일수"만 달력 상단에 유지** — 이유: 사용자 요청. 성공률·총기록 수치는 달력이 같은 정보를 시각적으로 더 잘 전달하므로 중복. 다만 `UserLevel` enum 자체는 결과 화면(MissionResultScreen)에서 계속 쓰여 보존.
- **달력은 `java.time`(YearMonth/LocalDate)로 구현** — 이유: minSdk 26이라 desugaring 없이 사용 가능. 기존 Repository의 `java.util.Calendar`와 무관하게 UI 레이어에서 독립 처리.
- **미래 달로는 이동 불가(다음 달 버튼 비활성)** — 이유: 기록이 있을 수 없는 미래 탐색은 의미 없음. 과거 데이터 전부 열람은 좌측 방향키로 무제한 허용.
- **성공/실패는 셀 배경색만으로 구분(✓/✕ 마크 제거)** — 이유: 사용자 요청. 초록/빨강 배경으로 충분히 식별되어 마크는 시각적 노이즈.
- **달력 데이터는 `getAllRecords()`(전체) 사용** — 이유: 기존 홈은 `getRecentRecords(7)`만 봤으나 달력은 과거 전체가 필요. 최근 기록 리스트는 기존 7건 유지하고 달력 아래에 그대로 둠.

**코드/프로젝트 변화:**
- `ui/main/MissionCalendar.kt` 신규: 연속 성공 헤더 + 월 이동 헤더 + 일~토 그리드. `resultByDate` 맵으로 날짜별 성공 우선 집계, 미래 달 버튼 비활성.
- `MainViewModel`: `MainUiState`에서 `successRate/totalCount/successCount` 제거하고 `allRecords` 추가. `observeStats`가 `getAllRecords()`를 관찰하도록 변경(`getRecentRecords(7)`는 유지).
- `MainActivity`: 통계 카드 item을 `MissionCalendar` 호출로 교체. `StatsCard`/`StatItem` 컴포저블과 `UserLevel`·`roundToInt` import 삭제. 최근 기록 리스트·빈 상태 안내는 유지.
- (검증 한계) 샌드박스에 Android SDK 부재로 실제 Gradle 빌드 미수행. 제거 심볼 잔존 없음·신규 심볼 연결 등 참조/import 정합성만 확인. 실기기 빌드/동작 확인은 다음 단계.

**[TODO 리스트 변경]:**
- 해결: 기능 "홈화면 미션 기록 달력"
- 추가: 없음

---

## [2026-06-15] 버스선택 지도 진입 시 현위치 자동 이동 + 타임어택 첫 조회 stale settings 수정

**방향/목표:** 두 진입 지점의 "첫 화면이 잘못된 상태로 떴다가 늦게 정상화되는" 체감 버그를 제거. (1) 버스선택 지도가 기본 좌표에 머무는 문제, (2) 알람→타임어택 진입 직후 도착정보 대신 "설정 안됨" 오류가 깜빡이는 문제.

**결정 사항:**
- **버스선택 지도는 진입 시 1회 현위치로 자동 이동한다** — 이유: 기존엔 `onMapReady`가 `DEFAULT_CENTER`(서울시청)만 로드하고 현위치 이동은 "내 주변" FAB 수동 조작에만 의존. 사용자가 매번 손으로 눌러야 했음. 자동 이동을 추가하되 기존 FAB는 수동 재호출용으로 유지.
- **자동 이동은 권한 분기를 그대로 재사용한다** — 이유: 권한 있으면 즉시 `moveToMyLocation`, 없으면 기존 `locationPermissionLauncher`로 요청 후 허용 시 이동. 별도 권한 로직 추가 없이 1회성 트리거(`didAutoLocate`)만 얹음.
- **타임어택 첫 조회는 StateFlow 캐시값이 아니라 DataStore 실제값을 읽는다** — 이유: `fetchArrivals()`가 `settings.value`를 읽었는데, `stateIn`의 initialValue가 기본 `UserSettings()`(hasMissionTarget=false)라 진입 직후 첫 조회가 "설정에서 출근 경로를 먼저 탐색" 오류를 띄우고, 다음 폴링(10초 후)에야 정상화되는 레이스였음. `dataStore.userSettings.first()`로 매 조회마다 실제 저장값을 직접 읽어 레이스를 원천 제거.

**코드/프로젝트 변화:**
- `BusSelectScreen`: `didAutoLocate` 플래그 + `LaunchedEffect(kakaoMap)` 신설 — 지도 준비되면 1회 권한 확인 후 현위치 이동(또는 권한 요청). 기존 FAB·DEFAULT_CENTER 초기 로드는 그대로(현위치 실패 시 폴백 역할).
- `TimeAttackViewModel.fetchArrivals`: `val s = settings.value` → `val s = dataStore.userSettings.first()`. `startPolling`의 선행 `first()` 대기는 유지(무해).
- (검증 한계) 샌드박스에 Android SDK 부재로 실제 빌드 미수행. import·참조 정합성만 확인. 실기기 동작 확인은 다음 단계.

**[TODO 리스트 변경]:**
- 해결: 없음
- 추가: 없음 (기존 "미사용 위치 권한 정리" TODO는 이번 변경으로 위치 권한이 버스선택 자동 이동에 실제 사용되므로, "제거 또는 기능 확정" 중 **기능 확정** 쪽으로 기운다 — 항목 문구는 유지)

---

## [2026-06-15] 알람 매일 반복 결함 수정 — 발생 시점 자동 재등록으로 전환 (0순위 해결)

**방향/목표:** "한 번 울리면 끝"이던 1회성 알람을, 매일 같은 시각에 반복되도록 만든다. 핵심은 `setAlarmClock`/`setExact`가 1회성이라는 제약을 "발생 시점에 다음날을 다시 등록"하는 패턴으로 우회하는 것.

**결정 사항:**
- **재등록을 "해제(dismiss) 시점"이 아니라 "알람 발생(fire) 시점"에 한다** — 이유: 사용자가 알람을 끄지 않거나 앱이 강제종료돼도, 브로드캐스트 리시버가 이미 실행되는 fire 시점에 등록하면 다음날이 보장됨.
- **알람과 자동실패의 재등록을 분리한다** — 이유: 알람 fire 시점에 자동실패까지 같이 다음날로 옮기면 *오늘분 자동실패*(같은 request code)가 덮여 사라짐. 그래서 자동실패는 `MissionFailReceiver`가 자기 발생 시점에 다음날을 재등록하도록 분리.
- **다음 발생 시각 계산을 `AlarmScheduler`로 일원화** — 이유: 동일 계산이 SettingsViewModel·AlarmReceiver(boot)에 복붙돼 있어, 반복 등록 지점이 늘면 불일치 위험. `nextDailyTrigger()` + `scheduleDailyAlarm()`/`scheduleDailyMissionFail()`로 모음.
- **부팅 복원에 자동실패도 추가** — 이유: 기존 boot 복원은 알람만 살렸음. 재부팅 후 자동실패가 비는 구멍을 메움.

**코드/프로젝트 변화:**
- `AlarmScheduler`: `nextDailyTrigger(h,m)`(이미 지난 시각이면 내일 보정) + `scheduleDailyAlarm`/`scheduleDailyMissionFail` 헬퍼 신설. 기존 `scheduleAt`/`scheduleMissionFailAt`은 유지.
- `AlarmReceiver.fireAlarm`: 서비스 시작 후 `goAsync` + DataStore 실제값 읽어 **다음날 알람 재등록**. `restoreAlarmAfterBoot`는 헬퍼 사용 + 자동실패 복원 추가. 중복 Calendar 로직·import 제거.
- `MissionFailReceiver`: 실패 기록(성공이면 스킵) 처리 후 **다음날 자동실패 재등록** 추가.
- `SettingsViewModel`: 중복 `nextTriggerMillis` 제거, 헬퍼 호출로 단순화.
- (확인된 함정 — 코드/git로 알기 어려움) `cancelMissionFail()`은 정의만 있고 **호출처가 없음(dead code)**. 덕분에 성공한 날에도 자동실패 알람이 그대로 울려 DB 성공 기록을 보고 스킵 → 다음날 재등록까지 이어져 반복 체인이 안 끊김. 즉 **현재 반복 체인은 이 dead code 상태에 의존**. 향후 "성공 시 자동실패 취소"를 도입하면 그 시점에서 다음날 재등록을 따로 넣어야 체인이 유지됨.
- (리스크) 정확한 알람 권한이 중간에 해제되면 `scheduleAt`/`scheduleMissionFailAt`가 조용히 등록을 건너뛰어 **반복 전체가 무음 중단**됨. 1회성이던 과거와 달리 이제 반복 전체가 권한 유지에 의존 → 기존 "권한 온보딩" TODO와 직접 연결.
- (검증 한계) 샌드박스에 Android SDK·JDK17 부재로 실제 Gradle 빌드 미수행. 코드 정합성(참조·import·중복 제거)만 확인. 실기 빌드/동작 확인은 다음 단계.

**[TODO 리스트 변경]:**
- 해결: 기능 "(0순위) 알람 매일 반복"
- 추가: 기타 작업 "성공 시 자동실패 취소 도입 시, 다음날 자동실패 재등록 동반 필수 (반복 체인이 현재 cancelMissionFail dead-code에 의존)"

---

## [2026-06-15] 설정 시각 편집이 버스/위치 선택 후 초기화되던 버그 수정

**방향/목표:** 설정 화면에서 알람/목표 시각을 바꾼 뒤(저장 전) "출근 버스 다시 선택"을 다녀오면 편집한 시각이 이전 저장값으로 되돌아가는 버그를 코드에서 규명·수정.

**결정 사항:**
- 시각 즉시저장(A안)이 아니라 **명시적 저장 유지 + 부분 저장 분리(B안)** 채택 — 이유: 즉시저장은 "저장 및 알람 등록" 버튼에 묶인 유효성 검사(알람<목표)와 알람 재등록을 편집 도중에 트리거해버려 UX가 깨짐. 시각은 저장 버튼을 눌러야만 영속화되어야 함.
- 버스/위치 저장을 `settings.value.copy(...) → saveSettings` 전체쓰기에서 **시각 키를 건드리지 않는 부분 저장**으로 전환 — 이유: 기존 방식은 미저장 옛 시각이 담긴 `settings.value`를 통째로 다시 써서 시각을 덮어씀.
- 설정 화면의 시각 state를 `remember` → **`rememberSaveable`** 로 변경 — 이유: 버스 화면 이동 시 설정 composable이 dispose되어 `remember` 편집값이 소실되고 복귀 시 DataStore 값으로 재초기화됨(사용자 증상이 이 dispose를 입증). NavHost는 백스택에 남은 화면의 saveable 상태를 보관하므로 왕복에도 편집값 보존.

**코드/프로젝트 변화:**
- `UserSettingsDataStore`: 시각 키 미변경 부분 저장 메서드 3종 추가 — `saveMissionTarget`, `saveHomeLocation`, `saveWorkLocation`.
- `SettingsViewModel`: 위 3종을 감싸 노출(알람 재등록 없음).
- `NavGraph`: 버스 선택·집·회사 저장을 부분 저장 호출로 교체(옛 `settings.value.copy → saveSettings` 제거).
- `SettingsScreen`: 알람/목표 시각 4개 state를 `rememberSaveable(저장된 시각값 key)` 로 변경. 저장 버튼·picker 로직은 그대로.
- (확인) Compose BOM 2024.09.00(runtime 1.7.x)에서 `rememberSaveable { mutableIntStateOf() }` 정식 지원, ViewModel은 SETTINGS 백스택 엔트리에 스코프되어 왕복 중 유지됨.

**[TODO 리스트 변경]:**
- 해결: 없음 (이번 세션 내에서 발견·수정한 버그 — 기존 `TODO.md` 항목 아님)
- 추가: 없음

---

## [2026-06-13] TODO.md 백로그 신설 + 세션로그 TODO 운영모델 전환 (1.0 릴리즈 목표 수립)

**방향/목표:** "한 바퀴 도는" 현재 앱을 Play Store 1.0으로 끌고 가기 위해, 출시에 부족한 부분을 외부(사용자·스토어) 관점에서 도출하고 살아있는 백로그를 세션로그에서 분리해 `TODO.md`로 단일화.

**결정 사항:**
- **살아있는 TODO를 `TODO.md`로 분리**, 세션로그는 변경 이력만 — 이유: 그간 "맨 위 항목 TODO만 살아있다"는 모델이 세션마다 TODO를 복붙·유실시켰고, 백로그 단일 출처가 없었음.
- **1.0 범위 = 버스 + 지하철 모두 포함**(사용자 결정) — 이유: 지하철 미션은 모델·타임어택은 이미 대응돼 있어 직접선택 화면만 추가하면 되는 수준이라 1.0에 넣을 가치.
- 출시 부족분을 **기능 / UI / 제출 게이트** 세 축 + 기타로 분류 — 이유: 코드 작업(기능·UI)과 Play Console 행정(서명·정책·권한선언)이 성격이 달라 섞으면 우선순위가 흐려짐.

**코드/프로젝트 변화:**
- `TODO.md` 신설 — 1.0 범위(확정) + 4개 카테고리(기능/UI/제출 게이트/기타 작업) 백로그.
- `CLAUDE.md` "세션 운영" 개정 3곳: 세션 시작 시 TODO.md 우선 확인, 요약 카드의 "다음 세션 TODO" → `[TODO 리스트 변경]`(해결/추가), TODO 관리 정책을 TODO.md 단일출처 모델로 재작성.
- (코드 점검으로 드러난 사실) **알람이 매일 반복되지 않음** — `AlarmScheduler.scheduleAt`이 1회성이고 발생 후 다음날 재등록 로직 부재. 1.0의 0순위 결함으로 등록.
- (정정) 앱 아이콘은 기본값 아님 — 커스텀 적응형 아이콘(파란 배경 + 떠오르는 해 + 버스, 모노크롬 포함) 이미 적용됨 확인.

**[TODO 리스트 변경]:**
- 해결: 없음 (이전 세션의 살아있던 TODO들은 닫힌 게 아니라 `TODO.md`로 이관됨 — 지하철 직접선택, 예외처리 정리, Git 리모트 연결 등)
- 추가: `TODO.md` 전체 신규 등록. 기능(알람 매일 반복[0순위]·지하철 직접선택·권한 온보딩·예외처리 표준화·미사용 위치권한 정리·API 한도 정책·엣지케이스), UI(앱이름 한글화·스플래시·온보딩 화면·설정 완성도·상태별 UI·다듬기), 제출 게이트(릴리즈 서명·개인정보처리방침·민감권한 선언서·FGS 타입 재검토·스토어 등록정보), 기타(Git 리모트·OkHttp 로깅 환원)

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
- `MainActiv
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

