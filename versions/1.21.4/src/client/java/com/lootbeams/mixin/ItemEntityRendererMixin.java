package com.lootbeams.mixin;

import com.lootbeams.LootBeamRenderer;
import com.lootbeams.util.LootBeamStateAttachment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {

	@Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
	private void onUpdateRenderState(ItemEntity entity, ItemEntityRenderState state, float partialTicks, CallbackInfo ci) {
		((LootBeamStateAttachment) state).lootbeams$setItemEntity(entity);
	}

	@Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
	private void onRender(ItemEntityRenderState state, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
		ItemEntity entity = ((LootBeamStateAttachment) state).lootbeams$getItemEntity();
		if (entity != null) {
			LootBeamRenderer.renderLootBeam(matrixStack, vertexConsumerProvider, entity);
		}
	}
}
