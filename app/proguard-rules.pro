# ── 디버그 스택 트레이스 복원용 ────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }

# ── Hilt / Dagger ─────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

# ── Room ──────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Retrofit + OkHttp ─────────────────────────────────────────
-keep class retrofit2.** { *; }
-keepattributes Signature, Exceptions
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Gson (Retrofit 응답 DTO) ───────────────────────────────────
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.yeon.todaymorning.data.api.dto.** { *; }

# TypeToken 익명 서브클래스의 제네릭 시그니처 보존 (R8 full-mode 대응)
# 없으면 UserSettingsDataStore 의 TypeToken<List<MissionRoute>> 가
# "TypeToken must be created with a type argument" 로 앱 시작 시 크래시.
-keepattributes Signature
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── Lottie ────────────────────────────────────────────────────
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ── DataStore ─────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }

# ── ViewModel / Lifecycle ─────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ── BroadcastReceiver (AlarmManager) ─────────────────────────
-keep class com.yeon.todaymorning.alarm.** { *; }

# ── 앱 전체 도메인 모델 ────────────────────────────────────────
-keep class com.yeon.todaymorning.domain.model.** { *; }

# ── 카카오맵 SDK ──────────────────────────────────────────────
-keep class com.kakao.vectormap.** { *; }
-keep interface com.kakao.vectormap.**
-dontwarn com.kakao.vectormap.**

# ── Google Play Services (위치) ───────────────────────────────
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**
