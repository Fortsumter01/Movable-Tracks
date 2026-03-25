package com.fortsumter.movabletracks.movement;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.fortsumter.movabletracks.MovableTracks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainStatus;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import static com.fortsumter.movabletracks.movement.TrackTrainAttachmentManager.REATTACH_SUPPRESSION_TICKS;

final class TrainAttachmentController {
    private static final @Nullable Field QUEUED_STATUS_MESSAGES_FIELD = resolveQueuedStatusMessagesField();
    private static boolean loggedQueuedStatusAccessFailure;

    private TrainAttachmentController() {}

    static void beginAttachment(ServerLevel level, Train train, AbstractContraptionEntity contraption,
                                AttachedTrainState snapshot) {
        if (train.graph != null) {
            train.leaveStation();
            train.detachFromTracks();
        }

        train.targetSpeed = 0;
        train.speed = 0;
        suppressDerailedState(train);
        train.navigation.cancelNavigation();
        train.navigation.waitingForSignal = null;
        train.occupiedSignalBlocks.clear();
        train.reservedSignalBlocks.clear();
        train.occupiedObservers.clear();
        train.cancelStall();

        applyAttachment(level, train, contraption, snapshot);
        MovableTracks.LOGGER.info("Attached train {} to moving track contraption {}", train.id, contraption.getUUID());
    }

    static void applyAttachment(ServerLevel level, Train train, AbstractContraptionEntity contraption,
                                AttachedTrainState snapshot) {
        train.targetSpeed = 0;
        train.speed = 0;
        suppressDerailedState(train);

        for (int i = 0; i < snapshot.carriages().size() && i < train.carriages.size(); i++) {
            Carriage carriage = train.carriages.get(i);
            DimensionalCarriageEntity dimensional = carriage.getDimensional(level);
            AttachedTrainState.CarriageSnapshot stored = snapshot.carriages().get(i);

            dimensional.positionAnchor = contraption.toGlobalVector(stored.positionAnchor(), 1);
            dimensional.rotationAnchors.setFirst(contraption.toGlobalVector(stored.leadingAnchor(), 1));
            dimensional.rotationAnchors.setSecond(contraption.toGlobalVector(stored.trailingAnchor(), 1));

            carriage.alignEntity(level);
            carriage.forEachPresentEntity(entity -> entity.syncCarriage());
        }
    }

    static void suppressDerailedState(Train train) {
        train.derailed = false;
        train.migrationCooldown = REATTACH_SUPPRESSION_TICKS;
        train.status.trackOK();
        clearQueuedStatusMessages(train);
    }

    private static void clearQueuedStatusMessages(Train train) {
        if (QUEUED_STATUS_MESSAGES_FIELD == null) {
            return;
        }

        try {
            Object queuedMessages = QUEUED_STATUS_MESSAGES_FIELD.get(train.status);
            if (queuedMessages instanceof List<?> messages && !messages.isEmpty()) {
                messages.clear();
            }
        } catch (ReflectiveOperationException exception) {
            if (!loggedQueuedStatusAccessFailure) {
                loggedQueuedStatusAccessFailure = true;
                MovableTracks.LOGGER.error("Failed to clear queued Create train status messages.", exception);
            }
        }
    }

    @Nullable
    private static Field resolveQueuedStatusMessagesField() {
        try {
            Field field = TrainStatus.class.getDeclaredField("queued");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            MovableTracks.LOGGER.error("Failed to access queued Create train status messages.", exception);
            return null;
        }
    }

    @Nullable
    static AttachedTrainState createSnapshot(Train train, ServerLevel level, AbstractContraptionEntity contraption) {
        List<AttachedTrainState.CarriageSnapshot> carriages = new ArrayList<>();

        for (Carriage carriage : train.carriages) {
            DimensionalCarriageEntity dimensional = carriage.getDimensionalIfPresent(level.dimension());
            if (dimensional == null || dimensional.positionAnchor == null || dimensional.rotationAnchors.either(Objects::isNull)) {
                return null;
            }
            if (!isAnchorNearTrack(contraption, dimensional.rotationAnchors.getFirst())
                    || !isAnchorNearTrack(contraption, dimensional.rotationAnchors.getSecond())) {
                return null;
            }

            carriages.add(new AttachedTrainState.CarriageSnapshot(
                    contraption.toLocalVector(dimensional.positionAnchor, 1),
                    contraption.toLocalVector(dimensional.rotationAnchors.getFirst(), 1),
                    contraption.toLocalVector(dimensional.rotationAnchors.getSecond(), 1)
            ));
        }

        if (carriages.isEmpty()) {
            return null;
        }

        return new AttachedTrainState(train.id, level.dimension(), contraption.getUUID(), carriages);
    }

    private static boolean isAnchorNearTrack(AbstractContraptionEntity contraption, Vec3 worldAnchor) {
        Vec3 local = contraption.toLocalVector(worldAnchor, 1);
        BlockPos origin = BlockPos.containing(local);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos candidate = origin.offset(dx, dy, dz);
                    if (!TrackTrainAttachmentManager.isTrackBlock(contraption, candidate)) {
                        continue;
                    }

                    Vec3 bottomCenter = contraption.toGlobalVector(Vec3.atBottomCenterOf(candidate), 1);
                    Vec3 center = contraption.toGlobalVector(Vec3.atCenterOf(candidate), 1);
                    double distance = Math.min(bottomCenter.distanceToSqr(worldAnchor), center.distanceToSqr(worldAnchor));
                    if (distance <= TrackTrainAttachmentManager.TRACK_ANCHOR_MAX_DISTANCE_SQR) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
