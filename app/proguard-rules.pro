## ProGuard rules for AndroidPhotobooth

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# JavaMail
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn javax.activation.**
-dontwarn javax.mail.**
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-keep class com.sun.mail.** { *; }
-keep class com.sun.activation.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.example.photobooth.**$$serializer { *; }
-keepclassmembers class com.example.photobooth.** { *** Companion; }
-keepclasseswithmembers class com.example.photobooth.** { kotlinx.serialization.KSerializer serializer(...); }

# CameraX
-keep class androidx.camera.** { *; }

# Coil
-dontwarn coil.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# AndroidX Security
-keep class androidx.security.** { *; }

# Keep data classes used by Room/Gson
-keep class com.example.photobooth.data.** { *; }
-keep class com.example.photobooth.settings.** { *; }
