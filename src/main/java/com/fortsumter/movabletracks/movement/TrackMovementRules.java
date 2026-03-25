package com.fortsumter.movabletracks.movement;

import com.fortsumter.movabletracks.MovableTracks;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.content.trains.track.ITrackBlock;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class TrackMovementRules {
    private static boolean registered;

    private TrackMovementRules() {}

    public static void register(IEventBus modBus) {
        if (registered) {
            return;
        }

        registered = true;
        modBus.addListener(TrackMovementRules::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> BlockMovementChecks.registerMovementAllowedCheck((state, level, pos) -> {
            if (state.getBlock() instanceof ITrackBlock) {
                return CheckResult.SUCCESS;
            }
            return CheckResult.PASS;
        }));
        event.enqueueWork(() -> MovableTracks.LOGGER.info("Registered Create track movement rules."));
    }
}
