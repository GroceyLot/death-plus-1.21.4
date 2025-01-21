package com.deathplus.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerDeathMessageMixin {

    @Shadow @Final public MinecraftServer server;

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        // Broadcast a message to the server
        Text deathMessage = Text.literal("Imagine being so bad, " + player.getName().getString() + "!")
                .formatted(Formatting.RED);
        server.getPlayerManager().broadcast(deathMessage, false);

        // Play a bell sound for all players on the server
        server.getPlayerManager().getPlayerList().forEach(p -> {
            p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
        });
    }
}
