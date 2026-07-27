# 릴리즈 노트 (오늘도출근)

## v1.6 (versionCode 7) — 2026-07-27 · 내부 테스트

### 내부 (코드 변경 상세)

- **미션 설정 저장 검증 추가.** 미션 설정 화면에서 저장 시 알람 시각 검증(알람 < 목표)만 하고 출근 경로는 비어 있어도 저장됐음. `SettingsScreen` 저장 버튼에 경로 검증을 추가해, 정류장·역(`missionTransitType`/`missionStopId`) 또는 노선(`missionRoutes`)이 비어 있으면 저장을 막고 무엇이 비었는지 스낵바로 안내한다.
- **세션 최대시간 초과 시 자동 종료.** 목표 시각이 지나도 사용자가 성공/실패를 고르지 않으면 세션이 무한정 열려 폴링·포그라운드 서비스가 계속 유지됐음. `MissionEngine`에 워치독 코루틴(`startSessionTimeout`)을 추가해 목표 시각 + 30분까지도 미선택이면 실패로 자동 마감한다(`onMissionFail`). 카운트다운 표시 로직은 건드리지 않도록 별도 코루틴으로 감시하며, 목표 시각 계산은 기존 롤오버 로직을 `targetMillis` 헬퍼로 추출해 재사용한다. 최대시간은 30분 고정(추후 설정화 예정).
- `versionCode` 6 → 7, `versionName` 1.5 → 1.6.

### 확인 필요 (릴리즈 빌드 검증 항목)

- 경로 미설정 상태로 저장 시 스낵바 안내가 뜨고 저장이 막히는지.
- 목표 시각 경과 후 30분이 지나면 세션이 실패로 자동 마감되고 서비스가 내려가는지.

### 외부 (플레이스토어 콘솔 입력용)

```
미션 설정 저장 시 출근 경로 확인을 추가하고, 목표 시각 이후 세션을 자동으로 정리하도록 수정했습니다.
```

## v1.5 (versionCode 6) — 2026-07-21 · 내부 테스트

> 비공개 테스트(vc5) 진행 중 별도 내부 테스트 트랙으로 올리는 빌드.
> 사용자 체감 기능 변화는 없고, 심사 리스크·로그 노출 두 건을 정리한 정비 빌드다.

### 내부 (코드 변경 상세)

- **`SCHEDULE_EXACT_ALARM` 권한 선언 정리.** Manifest에 `SCHEDULE_EXACT_ALARM`과 `USE_EXACT_ALARM`이 조건 없이 함께 선언돼 있었음. API 33+는 `USE_EXACT_ALARM`이 커버하므로 표준 패턴대로 `SCHEDULE_EXACT_ALARM`에 `android:maxSdkVersion="32"` 부여. 동작 변화는 없고 심사 지적 소지를 제거하는 목적.
- **API 인증키 로그 마스킹.** 릴리즈 로깅 레벨이 BASIC이어도 URL은 기록되는데, 공공 API 인증키가 URL에 실려 로그캣에 그대로 노출되고 있었음(2026-07-12 발견). `NetworkModule`의 `HttpLoggingInterceptor`에 커스텀 `Logger`를 주입해 출력 문자열만 치환: 버스·기타 공공 API의 `serviceKey=` 쿼리 파라미터, 지하철 API의 경로 내 키(`/api/subway/{key}/json/`) 둘 다 `***` 처리. 실제 요청 URL에는 영향 없으며 디버그·릴리즈 양쪽에 적용.
- `versionCode` 5 → 6, `versionName` 1.4 → 1.5.
- 문서: `docs/TODO.md`에 `Phase 4 — 날씨 기능 (1.0 이후)` 섹션 신설(아이디어 단계, 데이터 소스·표시 위치·위치 기준 미확정).

### 확인 필요 (릴리즈 빌드 검증 항목)

- 알람 스케줄링이 릴리즈 빌드에서 정상 동작하는지 — 이번에 Manifest 권한 선언을 건드렸으므로.
- 로그캣에서 `serviceKey=***` 및 지하철 경로 키가 실제로 가려지는지.

### 외부 (플레이스토어 콘솔 입력용)

```
권한 선언 방식을 정리하고 내부 로그 처리를 개선했습니다.
```

## v1.4 (versionCode 5) — 2026-07-18

### 내부 (코드 변경 상세)

- **미션 설정 지도가 안 뜨던 문제 수정.** 카카오맵 인증 401(MapAuthException). 릴리즈 빌드는 원본 앱 키가 아니라 테스트 앱 키(dd5f19)를 쓰고 있었고, 콘솔엔 `com.yeon.todaymorning.debug` 패키지 + 디버그 키 해시만 등록돼 있어, Play 서명된 `com.yeon.todaymorning` 빌드가 인증에 실패.
- `build.gradle.kts`: release 빌드가 원본 앱 키(6b5f709)를 쓰도록 `KAKAO_NATIVE_APP_KEY`를 buildType별로 분리(debug=테스트 앱, release=원본 앱). `local.properties`에 `KAKAO_NATIVE_APP_KEY_RELEASE` 추가.
- 카카오 콘솔(원본 앱)에 패키지 `com.yeon.todaymorning` + Play 앱 서명 키 해시 + 업로드 키 해시 등록.
- `versionCode` 2→5, `versionName` 1.1→1.4 (지도 수정 내부 테스트 재업로드 과정에서 vc3·4 소진 — Play 업로드는 versionCode 중복 불가 — 최종 5로 커밋).

### 외부 (플레이스토어 콘솔 입력용)

```
미션 설정 화면에서 지도가 표시되지 않던 문제를 수정했습니다.
```

## v1.1 (versionCode 2) — 2026-07-14

### 내부 (코드 변경 상세)

- **릴리즈 빌드 실행 즉시 크래시 수정.** R8 full-mode 난독화가 `UserSettingsDataStore`의 `object : TypeToken<List<MissionRoute>>() {}` 익명 서브클래스의 제네릭 시그니처를 제거해, DI(`DataStoreModule.provideUserSettingsDataStore`) 생성 단계에서 `IllegalStateException: TypeToken must be created with a type argument`로 앱 시작 시 죽던 문제. 디버그 빌드(난독화 off)에서는 재현되지 않아 내부 테스트 첫 설치에서 발견.
- `app/proguard-rules.pro`에 Gson keep 규칙 추가: `-keepattributes Signature`, `TypeToken` 및 그 서브클래스 keep(`allowobfuscation,allowshrinking`), `@SerializedName` 필드 보존. 앱 전체의 모든 TypeToken 사용처를 커버.
- 리스크 전수 스캔 결과 실제 크래시 지점은 TypeToken 한 곳뿐이며, Retrofit DTO(`data.api.dto.**`)·도메인 모델(`domain.model.**`)·enum은 기존 keep 규칙으로 이미 안전함을 확인.
- `versionName` 1.0 → 1.1, `versionCode` 1 → 2.

### 외부 (플레이스토어 콘솔 입력용)

```
앱 실행 안정성을 개선했습니다. 더 쾌적하게 오늘도출근을 시작해 보세요!
```

## v1.0 (versionCode 1) — 2026-07-12

### 내부 (코드 변경 상세)

- 최초 릴리즈

### 외부 (플레이스토어 콘솔 입력용)

```
오늘도출근 첫 출시! 매일 아침 출근을 타임어택 게임으로 만들어 보세요.
```
