package com.fortsumter.movabletracks.debug;

import com.fortsumter.movabletracks.Config;
import com.fortsumter.movabletracks.MovableTracks;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class StationDebugHandler {

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

        // Case 1: clicked directly on a station block
        if (clickedBe instanceof StationBlockEntity station) {
            debugSingleStation(event, station, clickedPos, "DIRECT_STATION_CLICK");
            return;
        }

        // Case 2: clicked on a track - find any station whose assembly box includes this rail
        if (clickedState.getBlock() instanceof TrackBlock) {
            boolean found = false;

            var areaMap = StationBlockEntity.assemblyAreas.get(event.getLevel());

            for (var entry : areaMap.entrySet()) {
                BlockPos stationPos = entry.getKey();
                BoundingBox box = entry.getValue();

                if (box == null || !box.isInside(clickedPos)) {
                    continue;
                }

                BlockEntity be = event.getLevel().getBlockEntity(stationPos);
                if (!(be instanceof StationBlockEntity station)) {
                    continue;
                }

                debugSingleStation(event, station, clickedPos, "TRACK_CLICK_INSIDE_ASSEMBLY");
                found = true;
            }

            if (!found) {
                String msg =
                        "StationDebug"
                                + " | mode=TRACK_CLICK_NO_STATION_FOUND"
                                + " | clickedTrack=" + clickedPos.toShortString()
                                + " | trackState=" + clickedState;

                event.getEntity().sendSystemMessage(Component.literal(msg));
                MovableTracks.LOGGER.info(msg);
            }
        }
    }

    private void debugSingleStation(PlayerInteractEvent.RightClickBlock event,
                                    StationBlockEntity station,
                                    BlockPos clickedPos,
                                    String mode) {
        station.refreshAssemblyInfo();

        GlobalStation globalStation = station.getStation();
        BoundingBox assemblyBox = StationBlockEntity.assemblyAreas
                .get(event.getLevel())
                .get(station.getBlockPos());

        boolean hasValidTrack = station.edgePoint.hasValidTrack();
        boolean hasGlobalStation = globalStation != null;
        boolean hasPresentTrain = hasGlobalStation && globalStation.getPresentTrain() != null;
        boolean clickedInsideAssemblyBox = assemblyBox != null && assemblyBox.isInside(clickedPos);

        String msg =
                "StationDebug"
                        + " | mode=" + mode
                        + " | stationPos=" + station.getBlockPos().toShortString()
                        + " | clickedPos=" + clickedPos.toShortString()
                        + " | hasValidTrack=" + hasValidTrack
                        + " | hasGlobalStation=" + hasGlobalStation
                        + " | hasPresentTrain=" + hasPresentTrain
                        + " | clickedInsideAssemblyBox=" + clickedInsideAssemblyBox
                        + " | assemblyBox=" + assemblyBox
                        + " | assemblyDirection=" + station.getAssemblyDirection();

        event.getEntity().sendSystemMessage(Component.literal(msg));
        MovableTracks.LOGGER.info(msg);
    }
}
