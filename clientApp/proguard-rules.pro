# Kotlin
-keepattributes *Annotation*, InnerClasses
-keep class kotlin.text.RegexOption
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}
# Keep annotations
-keep,allowobfuscation @interface kotlin.Metadata

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler

# Kotlinx Serialization
# Keep @Serializable classes and generated serializers
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class *$$serializer { *; }
-keepnames class kotlinx.serialization.internal.*

# SQLDelight
# Keep generated database implementation and driver classes
-keep class app.cash.sqldelight.** { *; }
-dontwarn java.sql.JDBCType

# Sentry
-keep class io.sentry.** { *; }
