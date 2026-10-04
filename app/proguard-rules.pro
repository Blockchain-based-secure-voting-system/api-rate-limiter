# ============================================================================
# ProGuard / R8 Rules for NT14 Gateway
# Defense-in-depth: Aggressive obfuscation, dead-code stripping, log elimination
# ============================================================================

# Obfuscation & Bytecode Hardening
-repackageclasses 'com.cutm.nt14.obf'
-allowaccessmodification
-keepparameternames

# Strip debug and verbose logging from release APKs (prevents token/endpoint leakage)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Preserve Reflection / Serialization Models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room Database Preservations
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}

# Dagger / Hilt Injections
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# Coroutines & Serialization
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# OkHttp & Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# PolyLance & Local Entities Keep
-keep class com.cutm.nt14.data.local.entity.** { *; }
-keep class com.cutm.nt14.data.remote.model.** { *; }
-keep class com.cutm.nt14.security.** { *; }
