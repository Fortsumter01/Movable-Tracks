package com.fortsumter.movabletracks.movement;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class AttachedTrainState {
    private final UUID trainId;
    private final ResourceKey<Level> dimension;
    private final UUID contraptionId;
    private final List<CarriageSnapshot> carriages;
    private int releaseAttempts;

    AttachedTrainState(UUID trainId, ResourceKey<Level> dimension, UUID contraptionId, List<CarriageSnapshot> carriages) {
        this.trainId = trainId;
        this.dimension = dimension;
        this.contraptionId = contraptionId;
        this.carriages = List.copyOf(carriages);
    }

    UUID trainId() {
        return trainId;
    }

    ResourceKey<Level> dimension() {
        return dimension;
    }

    UUID contraptionId() {
        return contraptionId;
    }

    List<CarriageSnapshot> carriages() {
        return carriages;
    }

    void resetReleaseAttempts() {
        releaseAttempts = 0;
    }

    int incrementReleaseAttempts() {
        return ++releaseAttempts;
    }

    record CarriageSnapshot(Vec3 positionAnchor, Vec3 leadingAnchor, Vec3 trailingAnchor) {}

    record ReleaseChoice(BlockPos pos, Vec3 look, double score) {}

    record CurrentCarriageState(boolean twoBogeys, @Nullable Vec3 positionAnchor, @Nullable Vec3 leadingAnchor,
                                @Nullable Vec3 trailingAnchor) {
        static CurrentCarriageState single(Vec3 positionAnchor) {
            return new CurrentCarriageState(false, positionAnchor, null, null);
        }

        static CurrentCarriageState doubleBogey(Vec3 leadingAnchor, Vec3 trailingAnchor) {
            return new CurrentCarriageState(true, null, leadingAnchor, trailingAnchor);
        }

        int pointCount() {
            return twoBogeys ? 4 : 2;
        }
    }
}
