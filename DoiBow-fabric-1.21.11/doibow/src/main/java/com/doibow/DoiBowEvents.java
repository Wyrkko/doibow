package com.doibow;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/**
 * Server-side damage handling for DoiBow arrows plus arrow clean-up.
 * Nothing here touches the client; all damage is processed by the server.
 */
public final class DoiBowEvents {
	private DoiBowEvents() {}

	/** Command tag put on every arrow fired by the DoiBow. */
	public static final String ARROW_TAG = "doibow_arrow";

	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private record Tracked(PersistentProjectileEntity arrow, int expireTick) {}

	private static final List<Tracked> TRACKED = new ArrayList<>();

	static void track(PersistentProjectileEntity arrow, int expireTick) {
		TRACKED.add(new Tracked(arrow, expireTick));
	}

	public static void register() {
		// 1) Make every arrow of the volley count. Vanilla gives a target 10 ticks of
		//    hurt-immunity after a hit, so 20 simultaneous arrows would otherwise deal
		//    the damage of ONE. We clear that timer only for DoiBow arrows. The normal
		//    damage pipeline (armour, enchantments, Totem of Undying) still runs untouched.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!isDoiBowArrow(source)) {
				return true;
			}
			if (DoiBowConfig.SPARE_AFTER_TOTEM && looksLikeFreshTotemPop(entity)) {
				return false;
			}
			entity.timeUntilRegen = 0;
			return true;
		});

		// 2) Extra armour wear per arrow that lands (not when a shield blocked it).
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (blocked || !isDoiBowArrow(source)) {
				return;
			}
			for (EquipmentSlot slot : ARMOR_SLOTS) {
				ItemStack piece = entity.getEquippedStack(slot);
				if (piece.isEmpty() || !piece.isDamageable()) {
					continue;
				}
				boolean netherite = Registries.ITEM.getId(piece.getItem()).getPath().startsWith("netherite_");
				int extra = netherite
						? DoiBowConfig.EXTRA_ARMOR_DURABILITY_NETHERITE
						: DoiBowConfig.EXTRA_ARMOR_DURABILITY;
				piece.damage(extra, entity, slot);
			}
		});

		// 3) Remove arrows that are done (stuck in blocks, etc.) so they never pile up.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int now = server.getTicks();
			TRACKED.removeIf(t -> {
				if (t.arrow().isRemoved()) {
					return true;
				}
				if (now >= t.expireTick()) {
					t.arrow().discard();
					return true;
				}
				return false;
			});
			if (now % 100 == 0) {
				DoiBowItem.pruneCooldowns(now);
			}
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			TRACKED.clear();
			DoiBowItem.clearCooldowns();
		});
	}

	private static boolean isDoiBowArrow(DamageSource source) {
		return source.getSource() instanceof PersistentProjectileEntity projectile
				&& projectile.getCommandTags().contains(ARROW_TAG);
	}

	/**
	 * Vanilla's totem leaves the player on exactly 1 HP with Regeneration, Absorption and
	 * Fire Resistance. If that is the state we find mid-volley, the totem just popped.
	 */
	private static boolean looksLikeFreshTotemPop(LivingEntity entity) {
		return entity.getHealth() <= 1.0F
				&& entity.hasStatusEffect(StatusEffects.REGENERATION)
				&& entity.hasStatusEffect(StatusEffects.ABSORPTION)
				&& entity.hasStatusEffect(StatusEffects.FIRE_RESISTANCE);
	}
}
