package com.lootbeams.mixin;

import net.minecraft.world.entity.item.ItemEntity;

public interface LootBeamStateAttachment {
	ItemEntity lootbeams$getItemEntity();
	void lootbeams$setItemEntity(ItemEntity entity);
}
