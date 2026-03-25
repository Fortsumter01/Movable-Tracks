package com.fortsumter.movabletracks;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue FREEZE_CARRIED_WHEEL_ANIMATION = BUILDER
            .comment("Freeze train wheel animation while a stationary train is being carried by moving tracks.")
            .define("freezeCarriedWheelAnimation", true);

    public static final ModConfigSpec.BooleanValue ENABLE_DEBUG_TOOLS = BUILDER
            .comment("Enable shift-right-click debug helpers for tracks and stations.")
            .define("enableDebugTools", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}

    public static boolean freezeCarriedWheelAnimation() {
        return FREEZE_CARRIED_WHEEL_ANIMATION.get();
    }

    public static boolean enableDebugTools() {
        return ENABLE_DEBUG_TOOLS.get();
    }
}
