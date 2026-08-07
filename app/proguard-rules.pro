# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.chicagoist.justgerman.**$$serializer { *; }
-keepclassmembers class com.chicagoist.justgerman.** {
    *** Companion;
}
-keepclasseswithmembers class com.chicagoist.justgerman.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }

# Keep data models
-keep class com.chicagoist.justgerman.data.model.** { *; }

# ExoPlayer
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**
