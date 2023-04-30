CHANGELOG
=========

Derived release (Another Term) forked at <https://github.com/felHR85/UsbSerial/commit/7ad6c9f6880e93a29498e479d3e871d5b03d188b>
--------------------------------------
...

Release 6.1.1 (planned in <https://github.com/felHR85/UsbSerial>)
--------------------------------------

- `jcenter()` has been expired: => `mavenCentral()`
- Gradle 6.7.1 / AGP 4.2.1
- FTDI: Fix baudrate setting on multiport devices
- Find the proper control index to use for CDC devices with multiple devices classes present.
  This fixes the problem which prevented the library from working with STM32 Nucleo boards, among
  others.
- ftdi: Fix `bcdDevice` endian

Release 6.1.0
--------------------------------------

- Added 1228800 and 2000000 baud rates to CH34xx driver.
- Microchip pid/vid correclty determined.
- FTDI sync method back to previous implementation.
- setBreak method implemented in CP210x devices.
- Added chunked stream methods.

Release 6.0.6
--------------------------------------

- Added custom baud rates in FTDI devices.
- Added setBreak method. Currently only working in FTDI devices.

Release 6.0.5
--------------------------------------

- Solved issue with CDC.
- Added new pair of VID/PID pairs for CP2102.
- Threads closing in a safer way.

Release 6.0.4
--------------------------------------

- Proguard rules.
- FTDI driver improved again.

Release 6.0.3
--------------------------------------

- VID/PID pairs are sorted and searched in a faster way.
- FTDI driver improved.

Release 6.0.2
--------------------------------------

- Solved issue when disconnecting multiple serial ports.

Release 6.0.1
--------------------------------------

- Internal serial buffer now uses [Okio](https://github.com/square/okio). This erases the 16kb write
  limitation from previous versions and reduces memory footprint.
- Improved CP2102 driver and added more VID/PID pairs.
- Added
  a [utility class for handling the common problem of split received information in some chipsets](https://github.com/felHR85/UsbSerial/blob/master/usbserial/src/main/java/com/felhr/utils/ProtocolBuffer.java)
  .
