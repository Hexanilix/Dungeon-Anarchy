package org.hexils.dnarch;

import java.util.ArrayList;
import java.util.Collection;

public class PluginThread extends Thread {
    public static Collection<PluginThread> threads = new ArrayList<>();
    protected boolean r = true;

    public PluginThread(Runnable r) { super(r); }

    public void halt() { this.r = false; }
    public static void finish() {
        for (PluginThread t : threads)
            t.halt();
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (PluginThread t : threads)
                t.interrupt();
        }).start();
    }
    public PluginThread() { threads.add(this); }
}
