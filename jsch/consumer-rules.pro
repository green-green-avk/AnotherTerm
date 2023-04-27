-keepclassmembers !abstract !interface ** extends com.jcraft.jsch.Channel {
    <init>();
}

-keep !abstract !interface
 com.jcraft.jsch.bc.**,
 com.jcraft.jsch.jce.**,
 com.jcraft.jsch.jcraft.**,
 com.jcraft.jsch.Cipher*,
 com.jcraft.jsch.DH*,
 com.jcraft.jsch.UserAuth*
