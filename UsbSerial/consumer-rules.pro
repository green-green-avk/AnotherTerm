# https://square.github.io/okio/#r8-proguard
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

-keep class com.felhr.** {
    *;
}
