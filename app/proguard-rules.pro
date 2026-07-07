# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Gson - DeepSeek models
-keep class com.vinreader.api.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# DeepSeek API data classes (used by Gson)
-keep class com.vinreader.api.DeepseekRequest { *; }
-keep class com.vinreader.api.DeepseekMessage { *; }
-keep class com.vinreader.api.DeepseekResponse { *; }
-keep class com.vinreader.api.DeepseekChoice { *; }
-keep class com.vinreader.api.DeepseekUsage { *; }
-keep class com.vinreader.api.DeepseekApiError { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
