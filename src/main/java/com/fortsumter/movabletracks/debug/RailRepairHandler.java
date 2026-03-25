package com.fortsumter.movabletracks.debug;

import com.fortsumter.movabletracks.Config;
import com.fortsumter.movabletracks.MovableTracks;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackPropagator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;

public class RailRepairHandler {

    private static final int HORIZONTAL_RADIUS = 16;
    private static final int VERTICAL_RADIUS = 6;

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !Config.enableDebugTools()) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!event.getEntity().isShiftKeyDown()) {
            return;
        }

        if (!event.getEntity().getMainHandItem().isEmpty()) {
            return;
        }

        BlockPos clickedPos = event.getPos();
        BlockState clickedState = event.getLevel().getBlockState(clickedPos);
        BlockEntity clickedBe = event.getLevel().getBlockEntity(clickedPos);

        boolean clickedTrack = clickedState.getBlock() instanceof TrackBlock;
        boolean clickedStation = clickedBe instanceof StationBlockEntity;

        if (!clickedTrack && !clickedStation) {
            return;
        }

        ServerLevel level = (ServerLevel) event.getLevel();
        RepairReport report = repairArea(level, clickedPos);

        String msg = "RailRepair"
                + " | center=" + clickedPos.toShortString()
                + " | tracksRebuilt=" + report.tracksRebuilt
                + " | stationsRefreshed=" + report.stationsRefreshed;

        event.getEntity().sendSystemMessage(Component.literal(msg));
        MovableTracks.LOGGER.info(msg);
    }

    private RepairReport repairArea(ServerLevel level, BlockPos center) {
        Set<BlockPos> tracks = new HashSet<>();
        Set<BlockPos> stations = new HashSet<>();

        BlockPos min = center.offset(-HORIZONTAL_RADIUS, -VERTICAL_RADIUS, -HORIZONTAL_RADIUS);
        BlockPos max = center.offset(HORIZONTAL_RADIUS, VERTICAL_RADIUS, HORIZONTAL_RADIUS);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockPos immutablePos = pos.immutable();
            BlockState state = level.getBlockState(immutablePos);
            BlockEntity be = level.getBlockEntity(immutablePos);

            if (state.getBlock() instanceof TrackBlock) {
                tracks.add(immutablePos);
            }

            if (be instanceof StationBlockEntity) {
                stations.add(immutablePos);
            }
        }

        int rebuilt = 0;
        for (BlockPos trackPos : tracks) {
            BlockState state = level.getBlockState(trackPos);
            TrackPropagator.onRailAdded(level, trackPos, state);
            rebuilt++;
        }

        int refreshed = 0;
        for (BlockPos stationPos : stations) {
            BlockEntity be = level.getBlockEntity(stationPos);
            if (be instanceof StationBlockEntity station) {
                station.refreshAssemblyInfo();
                station.setChanged();
                refreshed++;
            }
        }

        Create.RAILWAYS.markTracksDirty();
        return new RepairReport(rebuilt, refreshed);
    }

    private static class RepairReport {
        final int tracksRebuilt;
        final int stationsRefreshed;

        private RepairReport(int tracksRebuilt, int stationsRefreshed) {
            this.tracksRebuilt = tracksRebuilt;
            this.stationsRefreshed = stationsRefreshed;
        }
    }
}
