package com.deathplus.mixin;

import com.deathplus.AiTaunts;
import com.deathplus.ConfigLoader;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerDeathMessageMixin {

    @Shadow @Final public MinecraftServer server;

    @Unique
    private final Random random = new Random();

    @Unique
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void onDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        // Play a bell sound for all players on the server
        if (ConfigLoader.enableBellSound) {
            server.getPlayerManager().getPlayerList().forEach(p ->
                    p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.0F, 1.0F)
            );
        }

        // Broadcast a taunt message in a separate thread
        executor.execute(() -> {
            if (ConfigLoader.useAiTaunts) {
                AiTaunts.taunt(server, player, source);
            } else if (!ConfigLoader.tauntMessages.isEmpty()) {
                Text deathMessage = Text.literal(String.format(ConfigLoader.tauntMessages.get(random.nextInt(ConfigLoader.tauntMessages.size())), player.getName().getString()))
                        .formatted(Formatting.RED);
                server.getPlayerManager().broadcast(deathMessage, false);
            }
        });
    }
}
