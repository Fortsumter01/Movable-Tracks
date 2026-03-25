package com.fortsumter.movabletracks.debug;

import net.neoforged.neoforge.common.NeoForge;

public final class DebugHooks {
    private static boolean registered;

    private DebugHooks() {}

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        NeoForge.EVENT_BUS.register(new TrackDebugHandler());
        NeoForge.EVENT_BUS.register(new StationDebugHandler());
        NeoForge.EVENT_BUS.register(new RailRepairHandler());
    }
}
