package com.fortsumter.movabletracks.movement.client;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.fortsumter.movabletracks.Config;
import com.fortsumter.movabletracks.MovableTracks;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.createmod.catnip.animation.LerpedFloat;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class TrackWheelAnimationFix {
    private static final double EPSILON = 1.0E-4;
    private static final Map<UUID, FrozenWheelAngles> FROZEN_WHEEL_ANGLES = new HashMap<>();
    private static final @Nullable Field WHEEL_ANGLE_FIELD = resolveWheelAngleField();
    private static boolean registered;

    private TrackWheelAnimationFix() {}

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        NeoForge.EVENT_BUS.addListener(TrackWheelAnimationFix::onEntityTick);
        NeoForge.EVENT_BUS.addListener(TrackWheelAnimationFix::onEntityLeave);
    }

    private static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof CarriageContraptionEntity carriageEntity)
                || !carriageEntity.level().isClientSide()) {
            return;
        }

        Carriage carriage = carriageEntity.getCarriage();
        if (carriage == null) {
            FROZEN_WHEEL_ANGLES.remove(carriageEntity.getUUID());
            return;
        }

        boolean carriedByMovingTracks = ClientMovingTrackState.isCarriedByMovingTracks(carriageEntity, carriage);
        if (!carriedByMovingTracks) {
            FROZEN_WHEEL_ANGLES.remove(carriageEntity.getUUID());
            return;
        }

        if (!Config.freezeCarriedWheelAnimation() || !shouldFreezeWheelAnimation(carriage)) {
            FROZEN_WHEEL_ANGLES.remove(carriageEntity.getUUID());
            return;
        }

        FrozenWheelAngles frozenAngles = FROZEN_WHEEL_ANGLES.computeIfAbsent(carriageEntity.getUUID(),
                $ -> captureAngles(carriage));
        if (frozenAngles == null) {
            return;
        }

        applyFrozenAngles(carriage, frozenAngles);
    }

    private static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof CarriageContraptionEntity carriageEntity) {
            FROZEN_WHEEL_ANGLES.remove(carriageEntity.getUUID());
        }
    }

    private static boolean shouldFreezeWheelAnimation(Carriage carriage) {
        return carriage.train.graph == null
                && Math.abs(carriage.train.speed) < EPSILON
                && Math.abs(carriage.train.targetSpeed) < EPSILON;
    }

    @Nullable
    private static FrozenWheelAngles captureAngles(Carriage carriage) {
        Float leading = readWheelAngle(carriage.bogeys.getFirst());
        Float trailing = carriage.isOnTwoBogeys() ? readWheelAngle(carriage.bogeys.getSecond()) : null;

        if (leading == null && trailing == null) {
            return null;
        }

        return new FrozenWheelAngles(leading, trailing);
    }

    private static void applyFrozenAngles(Carriage carriage, FrozenWheelAngles frozenAngles) {
        writeWheelAngle(carriage.bogeys.getFirst(), frozenAngles.leading());
        if (carriage.isOnTwoBogeys()) {
            writeWheelAngle(carriage.bogeys.getSecond(), frozenAngles.trailing());
        }
    }

    @Nullable
    private static Float readWheelAngle(CarriageBogey bogey) {
        if (WHEEL_ANGLE_FIELD == null) {
            return null;
        }

        try {
            LerpedFloat wheelAngle = (LerpedFloat) WHEEL_ANGLE_FIELD.get(bogey);
            return wheelAngle == null ? null : wheelAngle.getValue();
        } catch (ReflectiveOperationException | ClassCastException exception) {
            MovableTracks.LOGGER.error("Failed to read Create wheel angle state", exception);
            return null;
        }
    }

    private static void writeWheelAngle(CarriageBogey bogey, @Nullable Float frozenAngle) {
        if (WHEEL_ANGLE_FIELD == null || frozenAngle == null) {
            return;
        }

        try {
            LerpedFloat wheelAngle = (LerpedFloat) WHEEL_ANGLE_FIELD.get(bogey);
            if (wheelAngle != null) {
                wheelAngle.setValue(frozenAngle);
            }
        } catch (ReflectiveOperationException | ClassCastException exception) {
            MovableTracks.LOGGER.error("Failed to write Create wheel angle state", exception);
        }
    }

    @Nullable
    private static Field resolveWheelAngleField() {
        try {
            Field field = CarriageBogey.class.getDeclaredField("wheelAngle");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            MovableTracks.LOGGER.error("Failed to access Create wheel angle field", exception);
            return null;
        }
    }

    private record FrozenWheelAngles(@Nullable Float leading, @Nullable Float trailing) {}
}
