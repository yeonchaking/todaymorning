# 오늘도출근 — CLAUDE.md

> 새 Claude 세션이 **이전 컨텍스트 없이도 바로 작업을 이어갈 수 있도록** 작성된 프로젝트 지침서.

---

## 세션 운영

### 세션 시작 시
1. `SESSION_LOG.md`를 읽고 **이전 세션 TODO** 확인
   - 파일이 없으면 "첫 세션"으로 간주하고 작업 시작
2. 인수인계 문서 `오늘도출근_개발_인수인계.md`를 필요 시 참고
3. 사용자가 이번 세션의 목표를 선언하면 작업 시작

### 세션 종료 시 (트리거: **"세션 종료"**)
1. 채팅에 아래 형식의 요약 카드를 출력
2. `SESSION_LOG.md`에 해당 항목을 **맨 위에** 추가 (최신 순 유지)

### 요약 카드 형식
```
## [YYYY-MM-DD] 세션 요약

**목표:** (이번 세션에서 하려 했던 것)

**결정 사항:**
- ...

**작업 결과:**
- ...

**다음 세션 TODO:**
- [ ] ...
```

> SESSION_LOG.md가 너무 길어지면 (세션 10개 이상) 오래된 항목은 `SESSION_LOG_ARCHIVE.md`로 이동하고 최근 5개만 유지한다.

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
