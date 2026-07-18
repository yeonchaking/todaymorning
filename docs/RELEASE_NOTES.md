# 릴리즈 노트 (오늘도출근)

## v1.4 (versionCode 5) — 2026-07-18

### 내부 (코드 변경 상세)

- **미션 설정 지도가 안 뜨던 문제 수정.** 카카오맵 인증 401(MapAuthException). 릴리즈 빌드는 원본 앱 키가 아니라 테스트 앱 키(dd5f19)를 쓰고 있었고, 콘솔엔 `com.yeon.todaymorning.debug` 패키지 + 디버그 키 해시만 등록돼 있어, Play 서명된 `com.yeon.todaymorning` 빌드가 인증에 실패.
- `build.gradle.kts`: release 빌드가 원본 앱 키(6b5f709)를 쓰도록 `KAKAO_NATIVE_APP_KEY`를 buildType별로 분리(debug=테스트 앱, release=원본 앱). `local.properties`에 `KAKAO_NATIVE_APP_KEY_RELEASE` 추가.
- 카카오 콘솔(원본 앱)에 패키지 `com.yeon.todaymorning` + Play 앱 서명 키 해시 + 업로드 키 해시 등록.
- `versionCode` 2→3, `versionName` 1.1→1.2.

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
