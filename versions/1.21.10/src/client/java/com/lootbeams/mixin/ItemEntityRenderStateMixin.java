package com.lootbeams.mixin;

import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements LootBeamStateAttachment {
	@Unique
	private ItemEntity lootbeams$entity;

	@Override
	public ItemEntity lootbeams$getItemEntity() {
		return this.lootbeams$entity;
	}

	@Override
	public void lootbeams$setItemEntity(ItemEntity entity) {
		this.lootbeams$entity = entity;
	}
}
