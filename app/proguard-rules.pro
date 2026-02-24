# SceneSense ProGuard rules

# Keep JNI native methods (LlamaModel)
-keepclasseswithmembernames class com.example.visionai.inference.LlamaModel {
    native <methods>;
}
-keep class com.example.visionai.inference.LlamaModel { *; }

# Keep data classes used by StateFlow (prevent R8 from removing fields)
-keep class com.example.visionai.UiState { *; }
-keep class com.example.visionai.ChatMessage { *; }
-keepclassmembers class com.example.visionai.** {
    public <fields>;
}

# ML Kit Translation
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Coroutines
-dontwarn kotlinx.coroutines.**

# CameraX
-keep class androidx.camera.** { *; }

# Keep enums
-keepclassmembers enum * { *; }
