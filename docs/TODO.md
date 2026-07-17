# TODO — 오늘도출근 (1.0 릴리즈 목표)

> **이 파일이 살아있는 백로그의 단일 출처(single source of truth)다.**
> 세션로그(`SESSION_LOG.md`)는 백로그를 보관하지 않는다. 각 세션은 종료 시
> 그 세션에서 **해결한 항목**과 **새로 추가한 항목**만 `[TODO 리스트 변경]`으로 기록하고,
> 그 변경을 이 파일에 반영한다. 운영 규칙은 `CLAUDE.md` "세션 운영" 참고.
>
> 목표: 아래 항목들을 채워 **Play Store 1.0 릴리즈**에 도달한다.

---

## 🎯 1.0 방향 (확정)

- **1.0은 버스 중심.** "지하철 미션 직접선택 화면"은 P2로 이동(2026-07-10 결정) — 지하철은 모델·타임어택 대응까지만 1.0에 포함.
- **릴리즈 확정 작업 11개는 `구글 플레이 콘솔 업로드 준비(오늘도출근).md` 최상단 체크리스트 참고.**
- **게임 요소 제외, 기능·실용 위주.** 포인트/레벨/배지/스트릭 강조형(게이미피케이션) 디자인은
  보류하고 `feature/gamification` 브랜치에 보존. main은 Claude Design v2(정보 카드형·신호등·그룹리스트)로 진행.
- 현재 v2 **UI는 구현됨**(메인/타임어택/설정). 아래 [0]은 그 UI에 **표시만 되고 실제 동작이 없는** 부분.

---

## 0. v2 UI 미구현 (화면엔 있으나 동작 없음 — 표시용 placeholder)

> 현재 화면에 노출돼 있어 사용자가 동작한다고 오해할 수 있는 항목. 우선 정리 대상.

- [x] ~~**TTS 음성 안내 (핵심)**~~ — 해결(2026-06-21). 안드로이드 내장 `TextToSpeech`(한국어) 연동. **버스 도착 기준**으로 동작: 리스트의 **모든 차편** 각각이 도착초 10·5·3분을 하향 통과할 때 발화("651번 버스가 5분후 도착 예정입니다", `[3번째 전]` 꼬리표 제거). 발화 시 짧은 진동 동반(폴링당 1회). `ttsEnabled`/`ttsTimings` DataStore 저장, 설정 음성안내 그룹 토글·시점칩 즉시 저장. 발화 문장은 `TtsManager.sentenceFor` 한 곳, 트리거는 `TimeAttackViewModel.maybeAnnounce`(fetchArrivals 10초 폴링에 연동). 한국어 TTS 미설치 기기는 조용히 무음 fallback. **단, 화면 꺼지면 멈춤 + 디버그 토스트 잔존 → 아래 신규 TODO 참조.**
- [x] ~~**알람음 선택**~~ — 해결(2026-06-21). 설정 알람음 행 → 선택 다이얼로그(기본/내장 라디오 + 미리듣기 + 휴대폰 ringtone picker + "최근 선택한 알람"). `AlarmSounds` registry로 내장음원 확장 구조 준비(현재 빈 목록), `AlarmRingService`가 선택값 분기 재생. 음원 합성은 보류.
- [x] ~~**진동 on/off 저장·제어**~~ — 해결(2026-06-21). 단순 토글 대신 **진동 패턴 선택** 방식으로 구현. 설정 진동 행 → 다이얼로그(진동없음/짧게0.5s/기본1s/길게5s/두번씩 라디오 + 탭 시 실제 진동 미리보기 + 확인 commit). `VibrationPatterns` registry로 패턴 확장 구조, `vibrationPatternId` DataStore 저장, `AlarmRingService`가 선택 패턴 재생(OFF면 스킵).
- [x] ~~**홈 다음버스 실시간 미리보기**~~ — 해결(2026-06-20). 홈 "실시간 도착 정보 확인하기" 버튼 → 다이얼로그로 실제 도착정보 표시(노선/N분 후, 여러 건 화살표). 인라인 미리보기 대신 on-demand 조회 방식. 미션 미설정 시 버튼 숨김.

---

## 1. 기능 (Functional)

- [ ] **지하철 미션 직접선택 화면** `[P2]` — 1.0에서 제외(2026-07-10 결정). 현재 버스만 UI 완성. 지하철은 모델·타임어택만 대응이라 직접선택 화면 신규 추가 필요(역명 검색 → 호선 선택). 설정 "지하철 추가" 행과 연결.
- [x] ~~**막차/첫차 계산 → 실시간 도착정보 반영**~~ — 해결(2026-07-12). **첫차/막차 시각 계산은 도입 안 함**(기점 기준 시각이라 사용자 정류장 통과 시각과 오차 — 리스크 대비 이득 없다고 결정). 대신 도착 API arrmsg의 "운행종료"/"출발대기" 신호를 세어(`ArrivalResult.endedCount/waitingCount`) 도착 0건 시 "지금은 운행 시간이 아니에요 (운행 종료 또는 첫차 전)" / "차량이 아직 출발 전이에요" 안내 — 오류·예외 표준화에 흡수 구현. ⚠️ 남은 가정: 심야 시간대에 API가 실제로 "운행종료" arrmsg를 주는지 실측 1회 필요(안 주면 "도착 예정인 차량이 없어요"로 표시됨).
- [x] ~~**오류·예외 처리 표준화**~~ — 해결(2026-07-12). `ApiErrorMapper.kt`(`TransitException` + `Throwable.toUserMessage()`) 신설, `TransitRepository` 도착조회 2함수가 예외를 삼키지 않고 던지도록 변경 + `ArrivalResult` 반환. `MissionEngine.fetchArrivals`/`MainViewModel.openArrivalDialog` 양쪽 동일 기준 문구 분기. 당일 서울버스 API 503 전면 장애 때 "(503)" 문구로 실전 검증됨.
- [x] ~~**위치 권한 사용 근거 문서화**~~ — 해결(2026-07-12). 개인정보처리방침(`docs/privacy.html`) 3장에 사용 근거 명시(정류장 선택 화면에서만 사용, 저장·서버 전송 없음, 거부해도 이용 가능). 권한 선언서 쪽 문구는 `구글 플레이 콘솔 업로드 준비(오늘도출근).md` 5번 표 초안 그대로 제출하면 됨 — 별도 작업 없음.
- [x] ~~**API 키·한도 정책 확정**~~ — 해결(2026-07-06). 릴리즈 빌드에 공공 API 키 내장(`BUS_API_KEY`/`SUBWAY_API_KEY`) + 하루 트래픽 한도 존재(개발계정 기준 기능별 일일 1,000건, 모든 사용자가 앱에 내장된 같은 키를 공유하는 구조라 클로즈드 테스트만으로도 한도 초과 우려 있었음). **data.go.kr 운영계정 전환 + 트래픽 증설 완료**로 해소. 사용자가 더 늘면 서버 캐싱 프록시 도입을 장기 확장 포인트로 남겨둠(상세는 `구글 플레이 콘솔 업로드 준비(오늘도출근).md`).
- [x] ~~**엣지케이스 (자정 넘김 롤오버 버그)**~~ — 해결(2026-07-12). `MissionEngine.startCountdown()`에 롤오버 추가: 목표가 지금보다 `MIDNIGHT_ROLLOVER_THRESHOLD_MS`(3시간) 이상 과거로 계산되면 +24h. 3시간인 이유(목표 직후 재진입하는 의도된 케이스와의 분리)는 코드 주석에 기록. 나머지 두 케이스(미션 미설정 알람·목표 직전 진입)는 기존 확인대로 변경 불필요.
- [ ] **타임어택 신호등 정밀화** `[P3]` — 현재 "남은 시간"만으로 여유/곧출발/임박 판정(`TimeAttackScreen`의 `signal` when절). "다음 버스가 목표 시각 내 도착 가능한지"까지 반영하면 더 정확(이미 계산돼 있는 `lastBoardableSeconds`를 신호등 색상에도 연결). 2026-07-06 세션: P3(우선순위 낮음)로 보류.

---

## 2. UI

- [x] ~~**앱 이름 한글화**~~ — 해결(2026-07-06). `strings.xml`의 `app_name` "todaymorning"(영문 기본값) → "오늘도출근".
- [x] ~~**스플래시 화면**~~ — 해결(2026-07-06). `androidx.core:core-splashscreen` 도입. `res/drawable/splash_icon.xml`(런처 아이콘과 내용은 같지만 독립 리소스) + `Theme.Todaymorning.Splash` 테마(라이트/다크 배경색 분리) + `MainActivity.installSplashScreen()`. **나중에 로고만 바꾸려면 `splash_icon.xml` 이 한 파일만 교체하면 됨**(테마·코드 변경 불필요).
- [x] ~~**온보딩/첫 실행 화면**~~ — 해결(2026-07-12). `IntroScreen` 소개 3장(타임어택/실시간 도착/기록 습관, 페이저+애니메이션 인디케이터+건너뛰기) + DataStore `hasSeenIntro`(1회성 앱 상태라 UserSettings 모델과 분리) + NavGraph 시작 분기(알람 진입 > 소개 > 권한 게이트 > 메인 — 알람이 소개에 가로막히지 않게) + MainActivity 스플래시 유지 게이트(깜빡임 방지) + 메인 미션 미설정 유도 카드. **부수 변경: 미션 설정 진입 경로 교체** — 우상단 설정(톱니) 버튼은 주석 보류(향후 앱 자체 설정 페이지용), "오늘의 미션" 카드 탭 → "미션 설정" 화면(타이틀도 변경).
- [x] ~~**상태별 UI 일관성 점검**~~ — 해결(2026-07-06). `RouteSelectScreen`/`BusSelectScreen`(노선 바텀시트)/`LocationPickerScreen`(주소검색 다이얼로그)이 로딩·빈·에러 상태에서 M3 기본 색(`MaterialTheme.colorScheme`)을 쓰고 있어 `TimeAttackScreen`/`MainActivity`(ArrivalDialog)의 `AppTheme.colors` 톤과 어긋나 있었음. 공통 컴포저블 `ui/common/StateViews.kt`(`SectionLoading`/`InlineLoading`/`EmptyStateText`/`ErrorStateText`) 신설해 세 화면에 적용, 전부 `AppTheme.colors` 기반으로 통일.
- [x] ~~**플로팅 위젯 주석처리 (기능 비활성화)**~~ — 해결(2026-07-12). 순수 주석처리(기능 플래그 대신 — 사용자 결정): Manifest `SYSTEM_ALERT_WINDOW` 제거, `MissionOverlay.kt` 전체 주석, `MissionService`/`TimeAttackScreen` 위젯 로직 주석(알림 갱신은 유지). 전 파일 공통 마커 `// 1.0 릴리즈: 플로팅 위젯 비활성화`로 검색 한 번에 복원 지점 확인 가능. DataStore 키·ViewModel setter는 P2 재활성화 대비 보존.
- [ ] **플로팅 위젯 재활성화 + 우리 앱이 전면일 때 숨기기** `[P2]` — 재도입 시 "오버레이가 미션 화면 위에도 겹쳐 뜨는" 문제(전면 숨김) 함께 처리.
- [ ] **다듬기** `[P2]` — 폰트 스케일·접근성(contentDescription), 결과 화면 시각 정리. (Jua/Pretendard 등 폰트는 현재 시스템 폰트 — 적용 시 다운로더블/번들.) 다크모드 점검은 완료(2026-07-10 실기기 확인, 깨짐 없음).

---

## 3. 제출 게이트 (Play Store)

> 기능/UI와 별개로, 없으면 업로드 자체가 막히는 항목.

- [x] ~~**릴리즈 서명 keystore** 생성 + `isMinifyEnabled`/난독화 확인~~ — 해결(2026-07-12). `upload-keystore.jks`(alias `release`) 생성 + `build.gradle.kts` signingConfig(keystore.properties 기반, 없으면 서명 생략 fallback) + `.gitignore` 키 파일 제외. minify는 기존 설정 온전함 확인, 서명 릴리즈 APK 빌드 성공. 상세는 `구글 플레이 콘솔 업로드 준비(오늘도출근).md` 2번.
- [x] ~~**개인정보처리방침 URL**~~ — 해결(2026-07-12). `docs/privacy.html` 신설(미수집 중심 구성: 개인정보 수집·전송 없음 + 기기 내 저장 데이터 표 + 위치 미저장/미전송 + 공공 API 호출 설명 + 권한 표) 후 앱 repo GitHub Pages(main `/docs` 소스)로 호스팅. **URL: `https://yeonchaking.github.io/todaymorning/privacy.html`** — Play Console 앱 콘텐츠 폼에 입력만 하면 됨(스토어 등록 시 함께).
- [x] ~~**민감 권한 선언서**~~ — 해결(2026-07-12). `민감권한_선언서(오늘도출근).md` 신설: 콘솔 앱 콘텐츠 폼에 붙여넣을 FGS mediaPlayback/dataSync·FSI 항목별 제출 문구(한/영) + 시연 영상 A(알람음)/B(미션 도착정보) 촬영 가이드. 조사 결과 정확한 알람은 별도 폼 없음(정책 심사만), 위치는 포그라운드 전용이라 선언 폼 대상 아님. 콘솔 실제 입력·영상 촬영은 AAB 업로드 시점에(스토어 등록과 함께).
- [x] ~~**`foregroundServiceType=mediaPlayback`/`dataSync` 적절성 재검토**~~ — 결정(2026-07-06). 둘 다 **유지**하기로 함: `AlarmRingService`(mediaPlayback)는 실제로 알람음을 재생하므로 정의에 부합, `MissionService`(dataSync)도 "서버에서 주기적으로 데이터를 가져오는" 공식 용도와 일치하고 Android 15의 하루 6시간 제한도 출퇴근 미션 실사용 패턴(하루 수십 분)에 문제없음. 코드 변경 없음 — Play Console 제출 시 사유 문구만 명확히 쓰면 됨(초안은 `구글 플레이 콘솔 업로드 준비(오늘도출근).md` 참고).
- [x] ~~**`SYSTEM_ALERT_WINDOW` 권한 Play 사유 제출**~~ — 불필요(2026-07-10). 플로팅 위젯 1.0 제외(주석처리)로 권한 자체를 제거 → 선언 대상 아님. 위젯 재도입(P2) 시 부활.
- [ ] **스토어 등록정보** — 스크린샷, 설명, 피처 그래픽.

---

## 4. 기타 작업

> 간단한 작업·테스트·나중에 하기로 정한 것.

- [ ] **`SCHEDULE_EXACT_ALARM`에 `maxSdkVersion="32"` 부여** — 현재 Manifest에 `SCHEDULE_EXACT_ALARM`·`USE_EXACT_ALARM`이 둘 다 무조건 선언돼 있음(2026-07-12 선언서 작업 중 발견). API 33+는 `USE_EXACT_ALARM`이 커버(자동 부여)하므로 표준 패턴은 `SCHEDULE_EXACT_ALARM`에 `android:maxSdkVersion="32"`. 동작엔 문제없으나 심사 지적 가능성. 한 줄 수정.
- [ ] **OkHttp 로그 serviceKey 마스킹** — 릴리즈의 BASIC 로깅도 URL은 찍는데 공공 API 키가 URL 쿼리 파라미터라 릴리즈 로그캣에 키가 그대로 노출됨(2026-07-12 로그 분석 중 발견 — "릴리즈엔 키 안 찍힘"이라던 기존 판단은 절반만 맞았음). 로깅 인터셉터에서 `serviceKey` 값 마스킹.
- [x] ~~OkHttp 로깅 레벨 BODY → BASIC 환원~~ — 해결(2026-07-06). `NetworkModule.buildOkHttpClient()`에서 `BuildConfig.DEBUG` 분기로 디버그는 BODY 유지, 릴리즈는 BASIC(URL·상태코드만) — API 키·응답 본문이 릴리즈 로그캣에 안 찍힘.
- [x] ~~**🔊 안내 토스트 정리**~~ — 결정(2026-07-06). `MissionEngine.maybeAnnounce`의 `🔊 N분 전 안내` 토스트는 실제 TTS 발화와 같은 조건 블록에서 동시에 뜨는 것으로 확인 — 음성 안내가 나오는 순간 화면에도 같은 문구가 잠깐 뜨는 용도라 그대로 유지하기로 함. 코드 변경 없음.
- [x] ~~**성공 시 자동실패 취소 도입 시 주의**~~ — 이동(2026-07-10). 작업이 아닌 주의사항이라 `잠재리스크(오늘도출근).md`로 이동.
- [x] ~~**목표 시각 경과 시 TTS/폴링/서비스가 조기 종료되는 버그**~~ — 해결(2026-07-06). 위 항목을 논의하다 발견: `MissionEngine.startCountdown()`이 `remaining <= 0`이 되는 즉시 `markFinished()`를 불러 `_finished=true`로 만들었고, 이게 `startPolling()` 루프(도착 폴링·TTS 트리거)와 `MissionService.watchFinished()`(서비스 자체 종료)를 동시에 멈췄다. 이 앱은 목표 시각을 살짝 넘겨 도착하는 차편도 "타면 성공"으로 인정해 하단 액션을 성공/실패 수동 선택으로 바꾸는 설계인데(`TimeAttackScreen`), 정작 그 순간 도착정보 갱신·TTS·백그라운드 서비스가 죽어버려 사용자가 결정하는 동안 안내가 끊겼음. 수정: 목표 시각 경과는 카운트다운만 멈추고(`markFinished()` 호출 제거), 실제 종료는 `onBoardingSuccess()`/`onMissionFail()`(사용자가 직접 성공/실패를 고르는 시점)에서만 일어나도록 함.
