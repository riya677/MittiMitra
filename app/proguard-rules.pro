# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers and reflection metadata for release crash diagnosis.
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,Exceptions,*Annotation*
-renamesourcefileattribute SourceFile

# ============================================
# Retrofit & OkHttp
# ============================================
-keepattributes Signature,InnerClasses,EnclosingMethod,Exceptions,*Annotation*

-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ============================================
# Gson
# ============================================
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }

# Official Gson R8/ProGuard rules (Gson 2.10+ — prevents TypeToken IllegalStateException)
# R8 strips generic signatures from anonymous TypeToken subclasses; these two rules stop that.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep fields annotated with @SerializedName so R8 doesn't null them out
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep model/entity classes for JSON serialization
-keep class com.mittimitra.backend.** { *; }
-keep class com.mittimitra.network.** { *; }
-keep class com.mittimitra.database.entity.** { *; }
-keep class com.mittimitra.ChatMessage { *; }

# ============================================
# Room Database
# ============================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ============================================
# TensorFlow Lite
# ============================================
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# ============================================
# Firebase
# ============================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase ML Model Downloader
-keep class com.google.firebase.ml.** { *; }

# ============================================
# Glide
# ============================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# ============================================
# Guava
# ============================================
-dontwarn com.google.common.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe

# ============================================
# AndroidX Security (EncryptedSharedPreferences)
# ============================================
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ============================================
# App-Specific Classes (for Gson serialization)
# ============================================
-keep class com.mittimitra.MandiActivity$MandiPrice { *; }
-keep class com.mittimitra.utils.** { *; }

# ============================================
# Jsoup (web scraping for scheme data)
# ============================================
-keep public class org.jsoup.** { *; }
-keeppackagenames org.jsoup.parser
-keeppackagenames org.jsoup.nodes

# ============================================
# Facebook Shimmer (loading animation)
# ============================================
-keep class com.facebook.shimmer.** { *; }
-dontwarn com.facebook.shimmer.**

# ============================================
# WorkManager
# ============================================
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ============================================
# DataStore (Preferences)
# ============================================
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ============================================
# Crashlytics — preserve stack traces
# ============================================
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,Exceptions,*Annotation*
-keep public class * extends java.lang.Exception
