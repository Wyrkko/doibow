package com.doibow;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/**
 * The DoiBow. Extends the vanilla BowItem (so it keeps the bow draw animation, the
 * "needs an arrow" check in use(), and enchantment support) but replaces the release
 * logic with a railgun burst. The vanilla bow is not touched.
 */
public class DoiBowItem extends BowItem {
	/** player UUID -> server tick of last shot (server thread only). */
	private static final Map<UUID, Integer> LAST_SHOT = new HashMap<>();

	public DoiBowItem(Settings settings) {
		super(settings);
	}

	@Override
	public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		int usedTicks = this.getMaxUseTime(stack, user) - remainingUseTicks;
		if (usedTicks < DoiBowConfig.MIN_CHARGE_TICKS) {
			return false;
		}
		// Client side: nothing to simulate. All logic and damage are server-side.
		if (!(world instanceof ServerWorld serverWorld)) {
			return true;
		}
		if (!(user instanceof ServerPlayerEntity player)) {
			return false;
		}

		int now = serverWorld.getServer().getTicks();
		Integer last = LAST_SHOT.get(player.getUuid());
		if (last != null && now - last < DoiBowConfig.COOLDOWN_TICKS) {
			return false;
		}

		boolean creative = player.isInCreativeMode();
		ItemStack ammo = player.getProjectileType(stack);
		if (ammo.isEmpty()) {
			if (!creative) {
				return false;
			}
			ammo = new ItemStack(Items.ARROW);
		}
		ArrowItem arrowItem = ammo.getItem() instanceof ArrowItem a ? a : (ArrowItem) Items.ARROW;
		ItemStack single = ammo.copyWithCount(1);

		// Aim direction from the player's exact look angles.
		double yaw = Math.toRadians(player.getYaw());
		double pitch = Math.toRadians(player.getPitch());
		double dirX = -Math.sin(yaw) * Math.cos(pitch);
		double dirY = -Math.sin(pitch);
		double dirZ = Math.cos(yaw) * Math.cos(pitch);

		// ---- The burst: every arrow is created and spawned inside this one call (= one tick) ----
		for (int i = 0; i < DoiBowConfig.ARROW_COUNT; i++) {
			PersistentProjectileEntity arrow = arrowItem.createArrow(serverWorld, single, player, stack);

			double vx = dirX + serverWorld.random.nextGaussian() * DoiBowConfig.SPREAD;
			double vy = dirY + serverWorld.random.nextGaussian() * DoiBowConfig.SPREAD;
			double vz = dirZ + serverWorld.random.nextGaussian() * DoiBowConfig.SPREAD;
			double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
			double speed = DoiBowConfig.ARROW_SPEED / len;
			arrow.setVelocity(vx * speed, vy * speed, vz * speed);
			arrow.setYaw(player.getYaw());
			arrow.setPitch(player.getPitch());

			arrow.setDamage(DoiBowConfig.ARROW_BASE_DAMAGE);
			arrow.setCritical(true);
			arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED; // no item duplication
			arrow.addCommandTag(DoiBowEvents.ARROW_TAG);

			serverWorld.spawnEntity(arrow); // vanilla entity tracking syncs it to clients
			DoiBowEvents.track(arrow, now + DoiBowConfig.ARROW_LIFETIME_TICKS);
		}

		// One arrow of ammo per shot (not in creative).
		if (!creative) {
			ammo.decrement(1);
		}
		LAST_SHOT.put(player.getUuid(), now);

		fireEffects(serverWorld, player, dirX, dirY, dirZ);
		return true;
	}

	private static void fireEffects(ServerWorld world, ServerPlayerEntity player, double dx, double dy, double dz) {
		double x = player.getX();
		double y = player.getEyeY();
		double z = player.getZ();

		world.playSound(null, x, y, z, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.6F);
		world.playSound(null, x, y, z, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.5F, 0.5F);
		world.playSound(null, x, y, z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.8F, 2.0F);

		if (DoiBowConfig.BEAM_PARTICLES) {
			for (int s = 1; s <= DoiBowConfig.BEAM_STEPS; s++) {
				double d = s * DoiBowConfig.BEAM_STEP_LENGTH;
				double px = x + dx * d;
				double py = y - 0.1 + dy * d;
				double pz = z + dz * d;
				world.spawnParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
				if ((s & 1) == 0) {
					world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 2, 0.1, 0.1, 0.1, 0.0);
				}
			}
		}
	}

	/** Drops cooldown entries that can no longer matter. Called from the server tick. */
	static void pruneCooldowns(int nowTick) {
		LAST_SHOT.values().removeIf(t -> nowTick - t > 200);
	}

	static void clearCooldowns() {
		LAST_SHOT.clear();
	}
}
