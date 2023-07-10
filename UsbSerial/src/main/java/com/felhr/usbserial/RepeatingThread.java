package com.felhr.usbserial;

abstract class RepeatingThread extends Thread {
    private volatile boolean cancelled = false;

    public void cancel() {
        cancelled = true;
        interrupt();
    }

    public void run() {
        try {
            while (!cancelled && !isInterrupted()) {
                onRepeat();
            }
        } catch (final InterruptedException ignored) {
        }
    }

    protected abstract void onRepeat() throws InterruptedException;
}
