package com.fortsumter.movabletracks;

import com.fortsumter.movabletracks.movement.MovementModule;

import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class MovableTracksClient {
    private MovableTracksClient() {}

    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(MovementModule::registerClient);
    }
}
