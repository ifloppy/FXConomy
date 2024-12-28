package me.yic.xconomy.task;

import me.yic.xconomy.AdapterManager;
import me.yic.xconomy.data.DataCon;

public class RunBaltop {
    public static void runstart() {
        AdapterManager.PLUGIN.runTaskLaterAsynchronously(() -> {
            try {
                DataCon.baltop();
            } catch (Exception ignored) {
            }
        }, 100L);
    }

    public static void stop() {
        // No-op for Fabric
    }
} 