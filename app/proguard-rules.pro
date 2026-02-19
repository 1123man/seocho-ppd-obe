# 프로젝트 ProGuard 규칙
# 기본 Android 최적화 규칙은 getDefaultProguardFile('proguard-android-optimize.txt')에 포함됨

# Hilt
-dontwarn dagger.hilt.**

# kotlinx-serialization (Navigation type-safe route)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.seocho.ppd.obe.**$$serializer { *; }
-keepclassmembers class com.seocho.ppd.obe.** {
    *** Companion;
}
-keepclasseswithmembers class com.seocho.ppd.obe.** {
    kotlinx.serialization.KSerializer serializer(...);
}
