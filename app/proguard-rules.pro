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
