package com.doibow;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public final class ModItems {
	private ModItems() {}

	public static final RegistryKey<Item> DOIBOW_KEY =
			RegistryKey.of(RegistryKeys.ITEM, Identifier.of(DoiBowMod.MOD_ID, "doibow"));

	public static final Item DOIBOW = Registry.register(
			Registries.ITEM,
			DOIBOW_KEY,
			new DoiBowItem(new Item.Settings()
					.registryKey(DOIBOW_KEY) // required since 1.21.2
					.maxCount(1)
					.rarity(Rarity.EPIC)
					.enchantable(1)));

	/** Forces class loading so the item is registered during mod init. */
	public static void register() {}
}
