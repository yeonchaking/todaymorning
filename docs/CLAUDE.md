# 오늘도출근 — CLAUDE.md

> 새 Claude 세션이 **이전 컨텍스트 없이도 바로 작업을 이어갈 수 있도록** 작성된 프로젝트 지침서.

---

## 세션 운영

### 작업 착수 원칙 (계획 승인 후 개발) — 반드시 지킬 것
- 사용자가 작업을 요청해도 **바로 코드/파일을 건드리지 않는다.**
- 먼저 ① **요구사항을 어떻게 이해했는지**, ② **무엇을(기술/파일) 어떻게 개발할지 계획**을 정리해 제시한다.
- 사용자가 **명시적으로 승인("ㅇㅋ"/"시작해" 등)한 뒤에야** 실제 개발(파일 생성·수정·빌드)을 시작한다.
- 단순 조회·설명·질문 답변은 예외(바로 답해도 됨). 파일을 바꾸거나 만드는 작업만 이 게이트를 적용한다.

### 세션 로그의 목적 (가장 먼저 읽을 것)
세션 로그는 **개발자의 일기가 아니라, 프로젝트·코드를 주체로 한 기록**이다.
"나는 ~했다"가 아니라 **"코드/프로젝트가 어떤 방향으로, 어떻게 바뀌었고, 무엇이 막혀서 다음으로 넘어갔는가"**를 남긴다.
모든 항목은 이 질문에 답해야 한다: *나중에 이 프로젝트를 이어받는 사람이, 코드와 git만으로는 알 수 없는 무엇을 여기서 얻는가?*

### 세션 시작 시
1. **`TODO.md`를 읽는다 — 이것이 살아있는 백로그의 단일 출처다.** 할 일은 전부 여기 있다.
2. `SESSION_LOG.md`를 읽는다. 세션로그는 **이력**이다 — 각 항목의 `[TODO 리스트 변경]`은 그 세션이 백로그를 어떻게 바꿨는지의 기록일 뿐, 살아있는 할 일 목록이 아니다.
   - 두 파일이 없으면 "첫 세션"으로 간주하고 작업 시작
3. 인수인계 문서 `오늘도출근_개발_인수인계.md`를 필요 시 참고
4. 사용자가 이번 세션의 목표를 선언하면 작업 시작

### 세션 종료 시 (트리거: **"세션 종료"**)
- 세션 로그는 **오직 "세션 종료" 시 단 1회만 작성한다. 작업 도중에는 쓰지 않는다.**
1. 채팅에 아래 형식의 요약 카드를 출력
2. `SESSION_LOG.md`에 해당 항목을 **맨 위에** 추가 (최신 순 유지)
3. **`TODO.md`를 갱신한다** — 이번 세션에서 해결한 항목은 제거(또는 완료 표시), 새로 추가하기로 한 항목은 적절한 카테고리에 삽입. 요약 카드의 `[TODO 리스트 변경]` 내용과 `TODO.md`의 실제 상태가 일치해야 한다.

### 요약 카드 형식
```
## [YYYY-MM-DD] (이번 세션이 프로젝트에 남긴 것 — 한 줄 제목)

**방향/목표:** (이번 세션에서 코드를 어느 방향으로 끌고 가려 했나)

**결정 사항:** (각 결정에 반드시 **왜**를 붙인다)
- ... — 이유: ...

**코드/프로젝트 변화:** (무엇이 어떻게 달라졌나 — "내가 했다"가 아니라 "코드가 이렇게 됐다")
- ...

**[TODO 리스트 변경]:** (이 세션이 `TODO.md` 백로그를 어떻게 바꿨나 — 실제 `TODO.md`에도 반영할 것)
- 해결: (이번 세션에 닫은 TODO. 없으면 `- 없음`)
- 추가: (새로 등록한 TODO + 카테고리. 없으면 `- 없음`)
```

### TODO 자격 기준
- **넣는다:** 이번 작업의 한계·문제 때문에 다음으로 넘기는 **진짜 작업**. (예: "지하철 직접선택 화면 신규 추가", "예외처리 공통 매핑 유틸")
- **넣지 않는다:**
  - 빌드/실기기/E2E 등 **당연히 거쳐야 할 검증** (메모리 `no-test-todos`)
  - 커밋 예정·커밋 방법·index.lock 해제 같은 **작업 메커니즘**
  - "언젠가 하면 좋은" 막연한 일반 개선

### TODO 해석/관리 정책
- **살아있는 TODO는 `TODO.md`에만 있다.** 세션로그의 `[TODO 리스트 변경]`은 이력이므로 거기서 할 일을 끌어다 쓰지 않는다.
- `TODO.md`는 카테고리(기능 / UI / 제출 게이트 / 기타 작업)로 구성한다. 새 TODO는 맞는 카테고리에 넣는다.
- TODO 착수 전 **현재 코드와 대조해 유효성을 확인**한다 (시간이 지나 stale 가능).
- 세션 종료 시 `[TODO 리스트 변경]`(해결/추가)과 `TODO.md`의 실제 상태를 **반드시 일치**시킨다.

> SESSION_LOG.md가 너무 길어지면 (세션 10개 이상) 오래된 항목은 `SESSION_LOG_ARCHIVE.md`로 이동하고 최근 5개만 유지한다.

---

## 릴리즈 노트 작성 규칙 (`RELEASE_NOTES.md`)

- **릴리즈 버전을 만들 때마다** `RELEASE_NOTES.md` **맨 위에** 새 버전 항목을 추가한다 (최신순 유지).
- `RELEASE_NOTES.md`에는 **오로지 릴리즈 노트만** 넣는다 — 작성법 설명, 현재 앱 기능 나열 금지 (작성법은 이 문서가 단일 출처).
- 각 버전 항목은 **내부/외부 2개 섹션**으로 나눈다:

```
## vX.Y (versionCode N) — YYYY-MM-DD

### 내부 (코드 변경 상세)
- 코드 관점의 변경사항을 자세히

### 외부 (플레이스토어 콘솔 입력용)
- 유저 체감 표현만. 콘솔 릴리즈 노트 칸에 그대로 복붙할 문구
```

- **내부**: 기술적으로 상세히 — 어떤 파일/클래스/로직이 왜·어떻게 바뀌었는지.
- **외부**: 유저 언어로만. 동일 기능 내 내부 로직만 수정했다면 "ㅇㅇ 기능 안정성 개선" 수준으로 뭉뚱그린다. 클래스명·기술 용어 금지. **500자 이내** (Play Console 언어당 한도).

---

## 앱 개요

**오늘도출근**은 매일 출근 습관을 게임처럼 만드는 알람 + 타임어택 앱이다.

**핵심 흐름:**
1. 사용자가 설정한 알람 시각이 되면 **타임어택 화면**이 열린다
2. 화면에는 **실시간 버스/지하철 도착 정보**가 표시된다
3. 사용자가 **목표 시각 전에** "탑승 완료" 버튼을 누르면 **성공** 기록
4. 목표 시각을 초과하면 **자동 실패** 처리
5. 성공/실패 기록이 누적되어 스트릭·레벨 등 **게이미피케이션** 요소로 발전

---

## 프로젝트 기본 정보

| 항목 | 내용 |
|---|---|
| 패키지명 | `com.yeon.todaymorning` |
| 프로젝트 경로 | `C:\Users\AM11D\AndroidStudioProjects\todaymorning` |
| 인수인계 문서 | `오늘도출근_개발_인수인계.md` (프로젝트 루트) |
| 설계 문서 | `C:\Users\AM11D\Documents\NockChaWang\기상미션 앱.md` |
| 개발 계획서 | `C:\Users\AM11D\Documents\NockChaWang\기상미션 앱 개발계획서.md` |
| 참고 코드 | `C:\Users\AM11D\Documents\NockChaWang\TodayCommute_Phase1\` |
| 옵시디언 노트 폴더 | `C:\Users\AM11D\Documents\NockChaWang` |

> ⚠️ 참고 코드의 패키지명은 `com.yeon.todaycommute` (오류). 실제 패키지는 **`com.yeon.todaymorning`** — 모든 파일에 이 패키지를 사용할 것.

---

## 기술 스택

| 레이어 | 기술 |
|---|---|
| 언어 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt 2.59.2 + KSP |
| DB | Room 2.8.4 |
| 설정 저장 | DataStore Preferences 1.1.1 |
| 네트워크 | Retrofit 2.11.0 + OkHttp 4.12.0 |
| 알람 | AlarmManager (`setAlarmClock`) |
| 애니메이션 | Lottie Compose |
| 아키텍처 | MVVM (ViewModel + Repository) |

**버전:**
- AGP: `9.0.1`
- Kotlin: `2.0.21` (AGP 9 built-in, `kotlin.android` 플러그인 별도 추가 금지)
- KSP: `2.0.21-1.0.26`
- Gradle: `9.2.1`

---

## 전체 Phase 로드맵

| Phase | 목표 | 상태 |
|---|---|---|
| **Phase 1** | 알람 울림 + MainActivity 열림 (Gradle, Room, DataStore, AlarmManager) | ✅ 완료 |
| **Phase 2** | 설정 화면 — 알람·목표 시각·대중교통 저장 (SettingsScreen, SettingsViewModel, NavGraph) | 🔄 진행 중 |
| Phase 3 | 타임어택 화면 — 실시간 버스/지하철 표시 (Retrofit API 연동, TimeAttackScreen) | |
| Phase 4 | 탑승 성공/실패 기록 + 메인 화면 통계 (MissionRepository, MainScreen) | |
| Phase 5 | 게이미피케이션 + 추천 로직 (Lottie 애니메이션, 스트릭, 레벨) | |
| Phase 6 | 안정화 + 배포 (엣지케이스, ProGuard, Play Store) | |

---

## 소스 구조

```
app/src/main/java/com/yeon/todaymorning/
├── TodayCommuteApp.kt          # @HiltAndroidApp
├── alarm/
│   ├── AlarmScheduler.kt       # AlarmManager 래퍼
│   ├── AlarmReceiver.kt        # BroadcastReceiver
│   └── MissionFailReceiver.kt  # 목표 시각 초과 시 자동 실패 (Phase 2: ViewModel 연동 예정)
├── data/
│   ├── api/
│   │   ├── BusApiService.kt
│   │   ├── SubwayApiService.kt
│   │   └── dto/
│   ├── datastore/
│   │   └── UserSettingsDataStore.kt
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── MissionDao.kt
│   │   └── MissionRecord.kt
│   └── repository/
│       └── TransitRepository.kt
├── di/
│   ├── DatabaseModule.kt
│   ├── DataStoreModule.kt
│   └── NetworkModule.kt
├── domain/model/
│   ├── MissionState.kt         # sealed class
│   ├── TransitArrival.kt       # 버스/지하철 통합 도착 정보
│   └── UserSettings.kt         # data class + TransitType enum
└── ui/
    ├── MainActivity.kt
    ├── NavGraph.kt
    ├── SettingsScreen.kt
    ├── SettingsViewModel.kt
    ├── timeattack/
    │   ├── TimeAttackScreen.kt
    │   └── TimeAttackViewModel.kt
    └── theme/
        ├── Color.kt             # 블루-화이트 팔레트
        ├── Theme.kt             # TodayCommuteTheme
        └── Type.kt              # TodayCommuteTypography
```

---

## 주요 주의사항 (AGP 9.0.1)

### `kotlin.android` 플러그인 — 추가하면 안 됨
AGP 9.0.1은 Kotlin을 내장으로 처리한다. 루트·app `build.gradle.kts` 모두에서 `kotlin.android`를 추가하면 **"kotlin extension 중복 오류"** 발생.

### `kotlinOptions` 사용 불가 → `kotlin { jvmToolchain(17) }` 사용
`kotlin.android` 플러그인이 없으면 `kotlinOptions { jvmTarget = "17" }` 사용 불가. 대신:
```kotlin
kotlin {
    jvmToolchain(17)
}
```

### KSP sourceSets 충돌 → `gradle.properties` 설정 필수
```
android.disallowKotlinSourceSets=false
```

### 버전 고정
- Hilt: **2.59.2 이상** (미만은 AGP 9에서 `Android BaseExtension not found` 오류)
- KSP: **2.0.21-1.0.26** (`2.0.21-1.0.29`는 존재하지 않음)
- Room: **2.8.4 이상** (미만은 `Continuation` 타입 불일치 오류)

### `compileSdk` 문법 (AGP 9.0.1 신규) — 건드리지 말 것
```kotlin
compileSdk {
    version = release(36) {
        minorApiLevel = 1
    }
}
```

### API 외부 통신 (HTTP 허용)
서울 버스 API(`ws.bus.go.kr`)와 지하철 API(`swopenapi.seoul.go.kr`)는 HTTP만 지원.
`res/xml/network_security_config.xml` 생성 후 Manifest 등록 필수 (완료됨).

### API 키 관리
Phase 3에서 필요. `local.properties`에 플레이스홀더:
```
BUS_API_KEY=
SUBWAY_API_KEY=
```
API 키는 절대 코드·git에 커밋하지 말 것.
