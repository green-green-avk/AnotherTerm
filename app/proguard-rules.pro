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

-keepclassmembers !abstract !interface **
 extends green_green_avk.anotherterm.backends.BackendModule {
    <init>();
    green_green_avk.anotherterm.backends.BackendModule$Meta meta;
}

-keepclassmembers, allowobfuscation class ** {
    @green_green_avk.anotherterm.backends.BackendModule$ExportedUIMethod <methods>;
}

-keepclassmembers class ** {
    @green_green_avk.anotherterm.utils.Settings$Param <fields>;
}

-keep class green_green_avk.anotherterm.wlterm.protocol.** {
    *;
}

#-dontshrink
#-dontoptimize
#-dontobfuscate
#-dontpreverify
-ignorewarnings

#-printseeds build/seeds.txt
#-printconfiguration build/full-r8-config.txt
