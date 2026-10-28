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

# WebView & JavascriptInterface
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# UI Screen ViewModels & State classes
-keep class com.ott.platform.ui.screens.** { *; }
-keepclassmembers class com.ott.platform.ui.screens.** { *; }

# Facebook SDK
-keep class com.facebook.** { *; }
-keepclassmembers class com.facebook.** { *; }
-dontwarn com.facebook.**


