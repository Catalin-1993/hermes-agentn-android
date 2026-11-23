# Hermes Android app-specific ProGuard rules.
# Additional keep rules can be added here in later tasks.

# sherpa-onnx is called from native code: its Kotlin classes and fields must keep their names.
-keep class com.k2fsa.sherpa.onnx.** { *; }

# The page recorder of "Teach me" calls back into the app through this interface.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
