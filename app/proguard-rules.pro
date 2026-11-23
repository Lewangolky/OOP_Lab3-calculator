# logback-android instantiates appenders and encoders from assets/logback.xml by reflection.
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }
-dontwarn ch.qos.logback.core.net.*
-dontwarn javax.mail.**
