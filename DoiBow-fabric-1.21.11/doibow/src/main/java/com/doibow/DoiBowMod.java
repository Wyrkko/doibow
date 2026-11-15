package com.doibow;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.ItemGroups;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoiBowMod implements ModInitializer {
	public static final String MOD_ID = "doibow";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.register();
		DoiBowEvents.register();

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
				.register(entries -> entries.add(ModItems.DOIBOW));

		LOGGER.info("DoiBow loaded. Use /function doibow:give");
	}
}
