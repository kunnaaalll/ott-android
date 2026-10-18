# ProGuard rules for OTT Platform

# Firebase
-keepattributes Signature
-keepattributes *Annotation*

# Firestore model classes
-keep class com.ott.platform.data.model.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Coil
-dontwarn coil.**

# Keep R8 from stripping interfaces
-keep,allowobfuscation,allowshrinking interface retrofit2.Call

# Core & API models
-keep class com.ott.platform.core.api.** { *; }
-keep class com.ott.platform.core.session.** { *; }

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
