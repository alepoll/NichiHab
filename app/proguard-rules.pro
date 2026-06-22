# NichiHab ProGuard Rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Moshi
-keep class com.nichihab.app.data.remote.dto.** { *; }
-keepclassmembers class com.nichihab.app.data.remote.dto.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
