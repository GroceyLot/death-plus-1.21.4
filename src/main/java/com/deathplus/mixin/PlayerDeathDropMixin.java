package com.deathplus.mixin;

import com.deathplus.ConfigLoader;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerDeathDropMixin {

	@Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
	private void onPlayerDeath(CallbackInfo ci) {
		if (!ConfigLoader.enableOneBlockDrops) {
			return;
		}
		PlayerEntity player = (PlayerEntity) (Object) this;
		World world = player.getWorld();

		if (!world.isClient && world instanceof ServerWorld) {
			ServerWorld serverWorld = (ServerWorld) world;
			BlockPos deathPos = player.getBlockPos();

			// Drop all inventory items directly at the player's death position
			for (ItemStack stack : player.getInventory().main) {
				dropItem(serverWorld, deathPos, stack);
			}
			for (ItemStack stack : player.getInventory().armor) {
				dropItem(serverWorld, deathPos, stack);
			}
			for (ItemStack stack : player.getInventory().offHand) {
				dropItem(serverWorld, deathPos, stack);
			}

			// Clear the player's inventory
			player.getInventory().clear();

			// Cancel the default inventory dropping behavior
			ci.cancel();
		}
	}

	private void dropItem(ServerWorld world, BlockPos pos, ItemStack stack) {
		if (!stack.isEmpty()) {
			ItemEntity itemEntity = new ItemEntity(
					world,
					pos.getX() + 0.5,
					pos.getY() + 0.5,
					pos.getZ() + 0.5,
					stack
			);

			// This line ensures that the item doesn't get a random spread velocity.
			itemEntity.setVelocity(0, 0, 0);

			world.spawnEntity(itemEntity);
		}
	}

}
