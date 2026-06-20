# SESSION_LOG — 오늘도출근

> "세션 종료" 시 1회만, 이 파일 **맨 위에** 항목을 추가한다 (최신 순).
> **살아있는 TODO는 `TODO.md`에만 있다.** 세션로그의 `[TODO 리스트 변경]`은 그 세션이 백로그를 어떻게 바꿨는지의 이력일 뿐이다.
> 오래된 항목은 `SESSION_LOG_ARCHIVE.md`로 이동(최근 5개만 유지). 작성 규칙은 `CLAUDE.md` "세션 운영" 참고.

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
