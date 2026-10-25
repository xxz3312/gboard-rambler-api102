-dontwarn io.github.libxposed.annotation.**
-adaptresourcefilecontents META-INF/xposed/java_init.list
# Preserve the small hook/config surface; R8 still shrinks dependencies/resources.
-keep class dev.kaede.pixelboard.** { *; }
-keep class io.github.libxposed.service.** { *; }
-repackageclasses dev.kaede.pixelboard.internal

-keep class org.luckypray.dexkit.** { *; }
