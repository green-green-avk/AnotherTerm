# AnotherTerm

Local pty, USB serial port, SSH and Telnet terminal client for Android.

* <https://play.google.com/store/apps/details?id=green_green_avk.anotherterm>
* <https://github.com/green-green-avk/AnotherTerm/wiki>


## Highlights

* Local pty, USB serial port (UART), SSH and Telnet support in the same application;

* Adequate screen input methods as long as a terminal requires specific keyboard functions.

* Ability to set fixed screen columns and/or rows numbers.


## Features

* Minimum supported Android version is 4.0 Ice Cream Sandwich.

* Supported USB UART devices: Generic USB CDC, CP210X, FTDI, PL2303, CH34x, CP2130 SPI-USB.

* Local Linux pty is supported. Feel free to use PRoot with some Linux environment:
<https://github.com/green-green-avk/AnotherTerm/wiki/Installing-Linux-under-PRoot>.

* Shell tool to interact with the Android environment is also present.
   - Content exchange between other applications and own files / pipes has been implemented.
   - It also works in chrooted environments (PRoot at least).
   - USB serial port dongle access from the command line is also implemented.
   - **libusb** support on nonrooted Android —
<https://github.com/green-green-avk/AnotherTerm/wiki/Installing-libusb-for-nonrooted-Android>.
   - Custom plugins to access the Android environment and own API to create them as separate APKs —
<https://github.com/green-green-avk/AnotherTerm/wiki/Shell-plugins>.

* Telnet (no encryption).

* Supported SSH features: zlib compression, port forwarding, password and public key authentication.

* No MoSH, sorry.

* Builtin screen keyboard and mouse.

* Different charsets and customizable key mapping support.


## 3rd party components

* USB UART: <https://github.com/felHR85/UsbSerial>
* SSH: <http://www.jcraft.com/jsch/>
* Console font: <https://www.fontsquirrel.com/fonts/dejavu-sans-mono>


## Cryptography usage note

The only part of this application that touches on cryptography is the SSH client used to
communicate with remote systems. Currently this is provided by the JSch library.
It does not directly implement any encryption,
but instead relies on Java Cryptography Extension (JCE) and its providers that are a part of Android.

This application is self-classified under ECCN 5D992 with the encryption authorization
type identifier MMKT.


## Disclaimer

It's unlawful to provide this application to or use by the prohibited entities listed in
<https://www.bis.doc.gov/index.php/policy-guidance/country-guidance/sanctioned-destinations>
and
<https://www.bis.doc.gov/index.php/policy-guidance/lists-of-parties-of-concern>.

The author is not responsible for any actions of any parties and their consequences
related to the aforementioned regulation.
