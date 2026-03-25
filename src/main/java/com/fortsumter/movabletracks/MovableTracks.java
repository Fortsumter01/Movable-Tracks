package com.fortsumter.movabletracks;

import com.fortsumter.movabletracks.debug.DebugHooks;
import com.fortsumter.movabletracks.movement.MovementModule;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(MovableTracks.MOD_ID)
public class MovableTracks {
    public static final String MOD_ID = "movabletracks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MovableTracks(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modBus.addListener(MovableTracksClient::onClientSetup);
        MovementModule.register(modBus);
        DebugHooks.register();
        LOGGER.info("Movable Tracks initialized.");
    }
}
