package com.fortsumter.movabletracks.movement;

import com.fortsumter.movabletracks.movement.client.TrackWheelAnimationFix;

import net.neoforged.bus.api.IEventBus;

public final class MovementModule {
    private static boolean clientRegistered;

    private MovementModule() {}

    public static void register(IEventBus modBus) {
        TrackMovementRules.register(modBus);
        TrackTrainAttachmentManager.register();
    }

    public static void registerClient() {
        if (clientRegistered) {
            return;
        }

        clientRegistered = true;
        TrackWheelAnimationFix.register();
    }
}
