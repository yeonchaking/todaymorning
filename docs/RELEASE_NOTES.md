# 릴리즈 노트 (오늘도출근)

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
