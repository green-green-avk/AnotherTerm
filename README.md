# AnotherTerm

Local PTY, USB and Bluetooth serial ports, SSH and Telnet terminal client for Android.

* <https://play.google.com/store/apps/details?id=green_green_avk.anotherterm>
* <https://green-green-avk.github.io/AnotherTerm-docs/>


## Highlights

* Local PTY, USB and Bluetooth serial (UART) ports, SSH and Telnet support in the same application;

* Adequate screen input methods as long as a terminal requires specific keyboard functions.

* Ability to set fixed screen columns and/or rows numbers.


## Features

* Minimum supported Android version is 4.0 Ice Cream Sandwich.

* Supported USB UART devices:
   - Generic USB CDC,
   - CP210X,
   - FTDI,
   - PL2303,
   - CH34x,
   - CP2130 SPI-USB.

* Bluetooth SPP UART devices are supported.

* Local Linux PTY is supported. Feel free to use PRoot with some Linux environment:
<https://green-green-avk.github.io/AnotherTerm-docs/installing-linux-under-proot.html#main_content>.

* Shell tool to interact with the Android environment is also present.
   - Content exchange between other applications and own files / pipes has been implemented.
   - It also works in chrooted environments (**PRoot** at least).
   - USB and Bluetooth serial port dongles access from the command line is also implemented.
   - **libusb** support on nonrooted Android —
<https://green-green-avk.github.io/AnotherTerm-docs/installing-libusb-for-nonrooted-android.html#main_content>.
   - Custom plugins to access the Android environment and own API to create them as separate APKs —
<https://green-green-avk.github.io/AnotherTerm-docs/local-shell-plugins.html#main_content>.

* Telnet (no encryption).

* Supported SSH features:
   - zlib compression;
   - password and public key authentication;
   - port forwarding (can be modified on a running SSH session);
   - starting extra shell channels on a running SSH session.

* No MoSH, sorry.

* Builtin screen keyboard and mouse.

* Hardware buttons mapping.

* Different charsets and customizable key mapping support.


## Dependencies

* USB UART: <https://github.com/felHR85/UsbSerial>
* SSH: <http://www.jcraft.com/jsch/>
* Console font: <https://www.fontsquirrel.com/fonts/dejavu-sans-mono>
* MaterialSeekBarPreference: <https://github.com/MrBIMC/MaterialSeekBarPreference>
* BetterLinkMovementMethod: <https://github.com/saket/Better-Link-Movement-Method>
* Android String XML Reference: <https://github.com/LikeTheSalad/android-string-reference>
* Android Maven Gradle Plugin: <https://github.com/dcendents/android-maven-gradle-plugin>
* Apache Commons: <https://commons.apache.org/>
* The Android Open Source Project (Official Google Android API and support libraries):
  <https://source.android.com/>


## Cryptography usage note

The only part of this application that touches on cryptography is the SSH client used to
communicate with remote systems. Currently this is provided by the JSch library.
It does not directly implement any encryption,
but instead relies on Java Cryptography Extension (JCE)
and its providers that are a part of Android.

This application is self-classified under ECCN 5D992 with the encryption authorization
type identifier MMKT.


## Disclaimer

It's unlawful to provide this application to or use by the prohibited entities listed in
<https://www.bis.doc.gov/index.php/policy-guidance/country-guidance/sanctioned-destinations>
and
<https://www.bis.doc.gov/index.php/policy-guidance/lists-of-parties-of-concern>.

The copyright holder and contributors are not responsible for any actions of any parties
and their consequences related to the aforementioned regulation.
