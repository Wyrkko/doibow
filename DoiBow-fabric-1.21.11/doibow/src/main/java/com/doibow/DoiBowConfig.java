package com.doibow;

/**
 * All tunables in one place. Edit and rebuild.
 *
 * Rough damage maths (vanilla arrow: ceil(speed * baseDamage), crits add up to +50% +2):
 *   speed 7 * baseDamage 2.0 = 14 per arrow (about 17 average with crit)
 *   x 20 arrows, each hit bypassing the 10-tick hurt cooldown (see DoiBowEvents).
 * That is enough to push a full-health, fully enchanted Netherite player through the
 * Totem of Undying threshold, while Minecraft's own totem code does the actual pop.
 */
public final class DoiBowConfig {
	private DoiBowConfig() {}

	/** Arrows spawned per shot (all in the same tick). */
	public static final int ARROW_COUNT = 20;
	/** Blocks per tick. Vanilla full-charge bow = 3.0. */
	public static final float ARROW_SPEED = 7.0F;
	/** Standard deviation of the aim error per axis. 0.0025 is about 0.14 degrees (very tight). */
	public static final double SPREAD = 0.0025;
	/** Vanilla arrow base damage is 2.0. */
	public static final double ARROW_BASE_DAMAGE = 2.0;

	/** Minimum draw time in ticks before the bow will fire (vanilla full draw = 20). */
	public static final int MIN_CHARGE_TICKS = 15;
	/** Per-player cooldown between shots, in ticks. */
	public static final int COOLDOWN_TICKS = 20;
	/** Arrows still alive (e.g. stuck in a block) after this many ticks are discarded. */
	public static final int ARROW_LIFETIME_TICKS = 80;

	/** Extra durability removed from EACH armour piece per arrow that hits (on top of vanilla wear). */
	public static final int EXTRA_ARMOR_DURABILITY = 4;
	/** Same, but for Netherite pieces. */
	public static final int EXTRA_ARMOR_DURABILITY_NETHERITE = 8;

	/**
	 * If true, once a target has popped a Totem of Undying, the remaining arrows of that
	 * volley are ignored so one shot "pops the totem" instead of also finishing the player
	 * off in the same tick. Set to false for a pure one-shot-kill weapon.
	 */
	public static final boolean SPARE_AFTER_TOTEM = true;

	/** Draw a short energy beam of particles when firing. */
	public static final boolean BEAM_PARTICLES = true;
	public static final int BEAM_STEPS = 20;
	public static final double BEAM_STEP_LENGTH = 2.0;
}
