package com.fortsumter.movabletracks.movement.client;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.track.ITrackBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ClientMovingTrackState {
    private static final double TRACK_ANCHOR_MAX_DISTANCE_SQR = 2.25d;
    private static final double CONTRAPTION_SEARCH_INFLATION = 3.0d;

    private ClientMovingTrackState() {}

    public static boolean isCarriedByMovingTracks(CarriageContraptionEntity carriageEntity, Carriage carriage) {
        if (carriage.train.graph != null) {
            return false;
        }

        DimensionalCarriageEntity dimensional = carriage.getDimensionalIfPresent(carriageEntity.level().dimension());
        if (dimensional == null) {
            return false;
        }

        AABB searchBox = buildSearchBox(dimensional.positionAnchor, dimensional.rotationAnchors.getFirst(),
            dimensional.rotationAnchors.getSecond());
        if (searchBox == null) {
            return false;
        }

        List<AbstractContraptionEntity> nearbyContraptions = carriageEntity.level()
            .getEntitiesOfClass(AbstractContraptionEntity.class, searchBox.inflate(CONTRAPTION_SEARCH_INFLATION),
                contraption -> contraption != carriageEntity && carriesTracks(contraption));

        for (AbstractContraptionEntity contraption : nearbyContraptions) {
            boolean leadingOnTrack = isAnchorNearTrack(contraption, dimensional.rotationAnchors.getFirst());
            boolean trailingOnTrack = isAnchorNearTrack(contraption, dimensional.rotationAnchors.getSecond());
            boolean positionOnTrack = isAnchorNearTrack(contraption, dimensional.positionAnchor);

            if ((leadingOnTrack && trailingOnTrack) || (positionOnTrack && (leadingOnTrack || trailingOnTrack))) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private static AABB buildSearchBox(@Nullable Vec3 first, @Nullable Vec3 second, @Nullable Vec3 third) {
        AABB searchBox = null;

        for (Vec3 anchor : new Vec3[] {first, second, third}) {
            if (anchor == null) {
                continue;
            }

            AABB anchorBox = new AABB(anchor, anchor);
            searchBox = searchBox == null ? anchorBox : searchBox.minmax(anchorBox);
        }

        return searchBox;
    }

    private static boolean carriesTracks(AbstractContraptionEntity contraption) {
        Contraption data = contraption.getContraption();
        if (data == null) {
            return false;
        }

        for (StructureBlockInfo info : data.getBlocks().values()) {
            if (info.state().getBlock() instanceof ITrackBlock) {
                return true;
            }
        }

        return false;
    }

    private static boolean isAnchorNearTrack(AbstractContraptionEntity contraption, @Nullable Vec3 worldAnchor) {
        if (worldAnchor == null) {
            return false;
        }

        Vec3 local = contraption.toLocalVector(worldAnchor, 1);
        BlockPos origin = BlockPos.containing(local);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (!isTrackBlock(contraption, candidate)) {
                        continue;
                    }

                    Vec3 bottomCenter = contraption.toGlobalVector(Vec3.atBottomCenterOf(candidate), 1);
                    Vec3 center = contraption.toGlobalVector(Vec3.atCenterOf(candidate), 1);
                    double distance = Math.min(bottomCenter.distanceToSqr(worldAnchor), center.distanceToSqr(worldAnchor));
                    if (distance <= TRACK_ANCHOR_MAX_DISTANCE_SQR) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isTrackBlock(AbstractContraptionEntity contraption, BlockPos localPos) {
        Contraption data = contraption.getContraption();
        if (data == null) {
            return false;
        }

        StructureBlockInfo info = data.getBlocks().get(localPos);
        return info != null && info.state().getBlock() instanceof ITrackBlock;
    }
}
