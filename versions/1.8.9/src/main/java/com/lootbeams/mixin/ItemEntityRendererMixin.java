package com.lootbeams.mixin;

import com.lootbeams.LootBeamRenderer;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {

	@Inject(method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V", at = @At("TAIL"))
	private void onRender(ItemEntity entity, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
		if (entity != null) {
			LootBeamRenderer.renderLootBeam(entity, x, y, z, tickDelta);
		}
	}
}
