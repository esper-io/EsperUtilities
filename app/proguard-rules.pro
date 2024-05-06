# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Native methods
# https://www.guardsquare.com/en/products/proguard/manual/examples#native
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# App
-keep class io.esper.android.files.** implements androidx.appcompat.view.CollapsibleActionView { *; }
-keep class io.esper.android.files.provider.common.ByteString { *; }
-keep class io.esper.android.files.provider.linux.syscall.** { *; }
-keepnames class * extends java.lang.Exception
# For Class.getEnumConstants()
-keepclassmembers enum * {
    public static **[] values();
}
-keepnames class io.esper.android.files.** implements android.os.Parcelable

# Apache FtpServer
-keepclassmembers class * implements org.apache.mina.core.service.IoProcessor {
    public <init>(java.util.concurrent.ExecutorService);
    public <init>(java.util.concurrent.Executor);
    public <init>();
}

# Bouncy Castle
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }

# SMBJ
-dontwarn javax.el.**
-dontwarn org.ietf.jgss.**
-dontwarn sun.security.x509.X509Key

# SMBJ-RPC
-dontwarn java.rmi.UnmarshalException

# Retrofit
-dontwarn retrofit2.**
-dontwarn org.codehaus.mojo.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes EnclosingMethod
-keepclasseswithmembers class * {
    @retrofit2.* <methods>;
}
-keepclasseswithmembers interface * {
    @retrofit2.* <methods>;
}
-dontnote retrofit2.Platform
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
-dontwarn retrofit2.Platform$Java8
-keepattributes Signature
-keepattributes Exceptions

#GSON ----
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.examples.android.model.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers enum * { *; }
-keep class net.mreunionlabs.wob.model.request.** { *; }
-keep class net.mreunionlabs.wob.model.response.** { *; }
-keep class net.mreunionlabs.wob.model.gson.** { *; }

# android-downloader
-keep public class * implements com.ixuea.android.downloader.db.DownloadDBController

# pdf-viewer
-keep class com.shockwave.**
