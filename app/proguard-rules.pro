# JARVIS Voice Agent ProGuard & R8 Optimization Rules
# Updated namespace: com.aistudio.jarvis.voiceagent

-keepattributes SourceFile,LineNumberTable,*Annotation*

# ─── Application & Entry Points ────────────────────────────────────────────────
-keep class com.aistudio.jarvis.voiceagent.JarvisApplication { *; }
-keep class com.aistudio.jarvis.voiceagent.MainActivity { *; }

# ─── ViewModel ─────────────────────────────────────────────────────────────────
-keep class com.aistudio.jarvis.voiceagent.viewmodel.JarvisViewModel {
    public <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ─── Room Database — entities, DAOs, and generated databases ──────────────────
-keep class com.aistudio.jarvis.voiceagent.data.db.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract ** **Dao();
}
-dontwarn androidx.room.paging.**

# ─── Moshi JSON models & generated adapters ───────────────────────────────────
-keep class com.aistudio.jarvis.voiceagent.model.** { *; }
-keep class com.aistudio.jarvis.voiceagent.data.ai.** { *; }
-keep class com.aistudio.jarvis.voiceagent.data.backend.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep @com.squareup.moshi.JsonClass class * { *; }

# ─── Tools — all tool implementations must survive shrinking ──────────────────
-keep class com.aistudio.jarvis.voiceagent.tools.** { *; }
-keepclassmembers class com.aistudio.jarvis.voiceagent.tools.** { *; }

# ─── Services — NotificationListenerService must not be stripped ──────────────
-keep class com.aistudio.jarvis.voiceagent.data.service.** { *; }
-keep class * extends android.service.notification.NotificationListenerService { *; }

# ─── UI / Compose Screens ─────────────────────────────────────────────────────
-keep class com.aistudio.jarvis.voiceagent.ui.** { *; }

# ─── Repository & Data Layer ──────────────────────────────────────────────────
-keep class com.aistudio.jarvis.voiceagent.data.repository.** { *; }

# ─── Coroutines ───────────────────────────────────────────────────────────────
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ─── OkHttp / Retrofit / Moshi ────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ─── Kotlin Reflect ───────────────────────────────────────────────────────────
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# ─── Google Play Services (Location) ──────────────────────────────────────────
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ─── Speech / TTS (Android system, never obfuscate) ──────────────────────────
-keep class android.speech.** { *; }
-keep class android.speech.tts.** { *; }
