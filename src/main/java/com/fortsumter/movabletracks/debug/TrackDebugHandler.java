package com.fortsumter.movabletracks.debug;

import com.fortsumter.movabletracks.Config;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.trains.track.TrackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class TrackDebugHandler {

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

        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);

        if (!(state.getBlock() instanceof TrackBlock)) {
            return;
        }

        boolean movementAllowed = BlockMovementChecks.isMovementAllowed(state, event.getLevel(), pos);
        boolean hasBlockEntity = event.getLevel().getBlockEntity(pos) != null;

        String message = "TrackDebug"
                + " | pos=" + pos.toShortString()
                + " | state=" + state
                + " | hasBE=" + hasBlockEntity
                + " | movementAllowed=" + movementAllowed;

        event.getEntity().sendSystemMessage(Component.literal(message));
    }
}
