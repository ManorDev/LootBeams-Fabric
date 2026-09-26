package com.lootbeams;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LootBeamRenderer {

	private static final Identifier LOOT_BEAM_TEXTURE = Identifier.of(LootBeamsClient.MODID, "textures/entity/loot_beam.png");
	private static final RenderLayer LOOT_BEAM_RENDER_LAYER = RenderLayer.getBeaconBeam(LOOT_BEAM_TEXTURE, true);

	public static void renderLootBeam(MatrixStack matrixStack, OrderedRenderCommandQueue queue, CameraRenderState camera, ItemEntity item) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;

		double distSq = player.squaredDistanceTo(item);
		if (distSq > LootBeamConfig.INSTANCE.render_distance * LootBeamConfig.INSTANCE.render_distance) {
			return;
		}

		boolean shouldRender = false;
		if (LootBeamConfig.INSTANCE.all_items) {
			shouldRender = true;
		} else {
			ItemStack stack = item.getStack();
			if (LootBeamConfig.INSTANCE.only_equipment) {
				if (stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES) || stack.isIn(ItemTags.PICKAXES)
						|| stack.isIn(ItemTags.SHOVELS) || stack.isIn(ItemTags.HOES)
						|| stack.isIn(ItemTags.TRIMMABLE_ARMOR) || stack.isIn(ItemTags.BOW_ENCHANTABLE)
						|| stack.isIn(ItemTags.CROSSBOW_ENCHANTABLE) || stack.isIn(ItemTags.EQUIPPABLE_ENCHANTABLE)) {
					shouldRender = true;
				}
			}
			if (LootBeamConfig.INSTANCE.only_rare) {
				if (stack.getRarity() != Rarity.COMMON) {
					shouldRender = true;
				}
			}
			if (isItemInRegistryList(LootBeamConfig.INSTANCE.whitelist, stack.getItem())) {
				shouldRender = true;
			}
		}

		if (isItemInRegistryList(LootBeamConfig.INSTANCE.blacklist, item.getStack().getItem())) {
			shouldRender = false;
		}

		if (!shouldRender) return;

		float beamAlpha = (float) LootBeamConfig.INSTANCE.beam_alpha;
		if (distSq < 2.0D) {
			beamAlpha *= (float) distSq;
		}
		if (beamAlpha <= 0.15f) {
			return;
		}
		final float finalAlpha = beamAlpha;

		float beamRadius = 0.05f * (float) LootBeamConfig.INSTANCE.beam_radius;
		float glowRadius = beamRadius + (beamRadius * 0.2f);
		float beamHeight = (float) LootBeamConfig.INSTANCE.beam_height;
		float yOffset = (float) LootBeamConfig.INSTANCE.beam_y_offset;

		Color color = getItemColor(item);
		float R = color.getRed() / 255f;
		float G = color.getGreen() / 255f;
		float B = color.getBlue() / 255f;

		long worldtime = item.getEntityWorld().getTime();
		float rotation = (float) Math.floorMod(worldtime, 40L);

		queue.submitCustom(matrixStack, LOOT_BEAM_RENDER_LAYER, (entry, builder) -> {
			MatrixStack ms = new MatrixStack();
			ms.push();

			// Render main beam
			ms.push();
			ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation * 2.25F - 45.0F));
			ms.translate(0, yOffset, 0);
			ms.translate(0, 1, 0);
			ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
			renderPart(ms, builder, R, G, B, finalAlpha, beamHeight, 0.0F, beamRadius, beamRadius, 0.0F, -beamRadius, 0.0F, 0.0F, -beamRadius);
			ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-180));
			renderPart(ms, builder, R, G, B, finalAlpha, beamHeight, 0.0F, beamRadius, beamRadius, 0.0F, -beamRadius, 0.0F, 0.0F, -beamRadius);
			ms.pop();

			// Render glow around main beam
			ms.translate(0, yOffset, 0);
			ms.translate(0, 1, 0);
			ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
			renderPart(ms, builder, R, G, B, finalAlpha * 0.4f, beamHeight, -glowRadius, -glowRadius, glowRadius, -glowRadius, -beamRadius, glowRadius, glowRadius, glowRadius);
			ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-180));
			renderPart(ms, builder, R, G, B, finalAlpha * 0.4f, beamHeight, -glowRadius, -glowRadius, glowRadius, -glowRadius, -beamRadius, glowRadius, glowRadius, glowRadius);

			ms.pop();
		});

		if (LootBeamConfig.INSTANCE.render_nametags) {
			renderNameTag(matrixStack, queue, camera, item, color);
		}
	}

	private static void renderNameTag(MatrixStack matrixStack, OrderedRenderCommandQueue queue, CameraRenderState camera, ItemEntity item, Color color) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;

		if (player.isSneaking() || (LootBeamConfig.INSTANCE.render_nametags_onlook && isLookingAt(player, item, LootBeamConfig.INSTANCE.nametag_look_sensitivity))) {
			String itemName = StringHelper.stripTextFormat(item.getStack().getName().getString());
			if (LootBeamConfig.INSTANCE.render_stackcount) {
				int count = item.getStack().getCount();
				if (count > 1) {
					itemName = itemName + " x" + count;
				}
			}

			double yOffset = LootBeamConfig.INSTANCE.nametag_y_offset;
			Vec3d pos = new Vec3d(0.0D, Math.min(1.0D, player.squaredDistanceTo(item) * 0.025D) + yOffset, 0.0D);
			queue.submitLabel(matrixStack, pos, 0, Text.literal(itemName), !LootBeamConfig.INSTANCE.borders, 15728880, LootBeamConfig.INSTANCE.render_distance, camera);
		}
	}

	private static Color getItemColor(ItemEntity item) {
		ItemStack stack = item.getStack();
		if (LootBeamsClient.CRASH_BLACKLIST.contains(stack)) {
			return Color.WHITE;
		}

		try {
			Color override = LootBeamConfig.getColorFromItemOverrides(stack.getItem());
			if (override != null) {
				return override;
			}

			NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
			if (customData != null) {
				String hex = customData.copyNbt().getString("lootbeams.color").orElse(null);
				if (hex != null) {
					return Color.decode(hex);
				}
			}

			if (LootBeamConfig.INSTANCE.render_name_color) {
				Color nameColor = getRawColor(stack.getName());
				if (!nameColor.equals(Color.WHITE)) {
					return nameColor;
				}
			}

			if (LootBeamConfig.INSTANCE.render_rarity_color) {
				Formatting fmt = stack.getRarity().getFormatting();
				if (fmt != null && fmt.getColorValue() != null) {
					return new Color(fmt.getColorValue());
				}
			}

			return Color.WHITE;
		} catch (Exception e) {
			LootBeamsClient.CRASH_BLACKLIST.add(stack);
			return Color.WHITE;
		}
	}

	private static Color getRawColor(Text text) {
		List<Style> list = new ArrayList<>();
		text.visit((style, string) -> {
			list.add(style);
			return Optional.empty();
		}, Style.EMPTY);
		if (!list.isEmpty() && list.get(0).getColor() != null) {
			return new Color(list.get(0).getColor().getRgb());
		}
		return Color.WHITE;
	}

	private static boolean isItemInRegistryList(List<String> registryNames, Item item) {
		if (registryNames != null && !registryNames.isEmpty()) {
			Identifier id = Registries.ITEM.getId(item);
			for (String entry : registryNames) {
				if (entry == null || entry.isEmpty()) continue;
				if (!entry.contains(":")) {
					if (id.getNamespace().equals(entry)) return true;
				} else {
					if (id.toString().equals(entry)) return true;
				}
			}
		}
		return false;
	}

	private static void renderPart(MatrixStack stack, VertexConsumer builder, float red, float green, float blue, float alpha, float height, float radius_1, float radius_2, float radius_3, float radius_4, float radius_5, float radius_6, float radius_7, float radius_8) {
		MatrixStack.Entry matrixentry = stack.peek();
		Matrix4f matrixpose = matrixentry.getPositionMatrix();
		renderQuad(matrixpose, matrixentry, builder, red, green, blue, alpha, height, radius_1, radius_2, radius_3, radius_4);
		renderQuad(matrixpose, matrixentry, builder, red, green, blue, alpha, height, radius_7, radius_8, radius_5, radius_6);
		renderQuad(matrixpose, matrixentry, builder, red, green, blue, alpha, height, radius_3, radius_4, radius_7, radius_8);
		renderQuad(matrixpose, matrixentry, builder, red, green, blue, alpha, height, radius_5, radius_6, radius_1, radius_2);
	}

	private static void renderQuad(Matrix4f pose, MatrixStack.Entry entry, VertexConsumer builder, float red, float green, float blue, float alpha, float y, float z1, float texu1, float z, float texu) {
		addVertex(pose, entry, builder, red, green, blue, alpha, y, z1, texu1, 1f, 0f);
		addVertex(pose, entry, builder, red, green, blue, alpha, 0f, z1, texu1, 1f, 1f);
		addVertex(pose, entry, builder, red, green, blue, alpha, 0f, z, texu, 0f, 1f);
		addVertex(pose, entry, builder, red, green, blue, alpha, y, z, texu, 0f, 0f);
	}

	private static void addVertex(Matrix4f pose, MatrixStack.Entry entry, VertexConsumer builder, float red, float green, float blue, float alpha, float y, float x, float z, float texu, float texv) {
		builder.vertex(pose, x, y, z).color(red, green, blue, alpha).texture(texu, texv).overlay(OverlayTexture.DEFAULT_UV).light(15728880).normal(entry, 0.0F, 1.0F, 0.0F);
	}

	private static boolean isLookingAt(ClientPlayerEntity player, Entity target, double accuracy) {
		Vec3d difference = new Vec3d(target.getX() - player.getX(), target.getEyeY() - player.getEyeY(), target.getZ() - player.getZ());
		double length = difference.length();
		if (length == 0) return true;
		double dot = player.getRotationVec(1.0F).normalize().dotProduct(difference.normalize());
		return dot > 1.0D - accuracy / length && !target.isInvisible();
	}
}
