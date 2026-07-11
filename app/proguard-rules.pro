# ---- Cooknivo ProGuard / R8 rules ----
# Kept intentionally conservative for release stability.

# Kotlinx Serialization ----------------------------------------------------
# Keep @Serializable classes and their generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.cooknivo.app.model.**$$serializer { *; }
-keep class com.cooknivo.app.model.** { *; }

# Keep Companion objects that expose serializer()
-keepclassmembers class com.cooknivo.app.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.cooknivo.app.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coroutines ---------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep enum values used by serialization
-keepclassmembers enum com.cooknivo.app.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Compose is handled by its own consumer rules; nothing extra required.
