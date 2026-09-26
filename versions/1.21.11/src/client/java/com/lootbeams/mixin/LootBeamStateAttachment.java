package com.lootbeams.mixin;

import net.minecraft.entity.ItemEntity;

public interface LootBeamStateAttachment {
	ItemEntity lootbeams$getItemEntity();
	void lootbeams$setItemEntity(ItemEntity entity);
}
