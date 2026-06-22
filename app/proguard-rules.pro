# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Ktor resolves its HTTP engine via ServiceLoader (META-INF/services). R8 can't see that
# reflective call site, so without an explicit keep it strips AndroidEngineContainer and the
# app crashes instantly on launch with "No element of given type found".
-keep class io.ktor.client.engine.android.* { *; }
-keep class io.ktor.client.HttpClientEngineContainer
-keepclassmembers class io.ktor.client.engine.android.* { *; }

# kotlinx.serialization generated serializers are looked up reflectively too.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.example.oredziednia.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.oredziednia.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.oredziednia.**$$serializer { *; }

# supabase-kt and its crypto dependency also rely on reflection/ServiceLoader internally.
-keep class io.github.jan.supabase.** { *; }
-keep class dev.whyoleg.cryptography.** { *; }

# WorkManager instantiates its generated Room database (WorkDatabase_Impl) via reflection at
# app startup, before any of our own code runs. Without this, R8 strips the no-arg constructor
# and the app crashes immediately on launch with NoSuchMethodException.
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.work.impl.** { *; }