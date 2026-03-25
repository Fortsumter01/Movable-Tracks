package com.fortsumter.movabletracks.mixin.client;

import java.lang.ref.WeakReference;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.fortsumter.movabletracks.movement.client.ClientMovingTrackState;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.TrainRelocator;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(TrainRelocator.class)
abstract class TrainRelocatorMixin {
    @Shadow
    static WeakReference<CarriageContraptionEntity> hoveredEntity;

    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private static void movabletracks$hideMovingTrackDerailedHint(List<Component> tooltip, boolean shiftKeyDown,
                                                                  CallbackInfoReturnable<Boolean> cir) {
        CarriageContraptionEntity carriageEntity = hoveredEntity.get();
        if (carriageEntity == null) {
            return;
        }

        Carriage carriage = carriageEntity.getCarriage();
        if (carriage == null || !carriage.train.derailed) {
            return;
        }

        if (ClientMovingTrackState.isCarriedByMovingTracks(carriageEntity, carriage)) {
            cir.setReturnValue(false);
        }
    }
}
