# AimShot Engine Proguard Rules
-keep class com.alpha10.aimshot.model.** { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
