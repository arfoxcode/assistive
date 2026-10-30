# Assistive Menu Tool — ProGuard / R8 rules

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# AccessibilityService must not be obfuscated (referenced by manifest meta-data)
-keep class arfoxcode.joaohouto.assistivemenutool.AssistiveMenuAccessibilityService { *; }

# Foreground Service must not be obfuscated
-keep class arfoxcode.joaohouto.assistivemenutool.FloatingButtonService { *; }

# MainActivity must not be obfuscated (referenced by PendingIntent)
-keep class arfoxcode.joaohouto.assistivemenutool.MainActivity { *; }

# Keep enum names used for SharedPreferences serialization (MenuAction.entries)
-keepclassmembers enum arfoxcode.joaohouto.assistivemenutool.MenuAction { *; }

# Jetpack Compose
-dontwarn androidx.compose.**
