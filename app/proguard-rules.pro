# Keep Room entities
-keep class com.example.appguard.data.db.entity.** { *; }

# Keep AccessibilityService
-keep class com.example.appguard.service.AppGuardAccessibilityService { *; }

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.example.appguard.**$$serializer { *; }
-keepclassmembers class com.example.appguard.** { *** Companion; }
-keepclasseswithmembers class com.example.appguard.** { kotlinx.serialization.KSerializer serializer(...); }