package com.lootbeams;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LootBeamRenderer {

	private static final Identifier LOOT_BEAM_TEXTURE = Identifier.fromNamespaceAndPath(LootBeamsClient.MODID, "textures/entity/loot_beam.png");
	private static final RenderType LOOT_BEAM_RENDER_TYPE = RenderTypes.beaconBeam(LOOT_BEAM_TEXTURE, true);

	public static void renderLootBeam(PoseStack poseStack, SubmitNodeCollector queue, CameraRenderState camera, ItemEntity item) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;

		double distSq = player.distanceToSqr(item);
		if (distSq > LootBeamConfig.INSTANCE.render_distance * LootBeamConfig.INSTANCE.render_distance) {
			return;
		}

		boolean shouldRender = false;
		if (LootBeamConfig.INSTANCE.all_items) {
			shouldRender = true;
		} else {
			ItemStack stack = item.getItem();
			if (LootBeamConfig.INSTANCE.only_equipment) {
				if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.PICKAXES)
						|| stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)
						|| stack.is(ItemTags.TRIMMABLE_ARMOR) || stack.is(ItemTags.BOW_ENCHANTABLE)
						|| stack.is(ItemTags.CROSSBOW_ENCHANTABLE) || stack.is(ItemTags.EQUIPPABLE_ENCHANTABLE)) {
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

		if (isItemInRegistryList(LootBeamConfig.INSTANCE.blacklist, item.getItem().getItem())) {
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

		long worldtime = item.level().getGameTime();
		float rotation = (float) Math.floorMod(worldtime, 40L);

		queue.submitCustomGeometry(poseStack, LOOT_BEAM_RENDER_TYPE, (pose, builder) -> {
			PoseStack ps = new PoseStack();
			ps.pushPose();

			// Render main beam
			ps.pushPose();
			ps.mulPose(Axis.YP.rotationDegrees(rotation * 2.25F - 45.0F));
			ps.translate(0, yOffset, 0);
			ps.translate(0, 1, 0);
			ps.mulPose(Axis.XP.rotationDegrees(180));
			renderPart(ps, builder, R, G, B, finalAlpha, beamHeight, 0.0F, beamRadius, beamRadius, 0.0F, -beamRadius, 0.0F, 0.0F, -beamRadius);
			ps.mulPose(Axis.XP.rotationDegrees(-180));
			renderPart(ps, builder, R, G, B, finalAlpha, beamHeight, 0.0F, beamRadius, beamRadius, 0.0F, -beamRadius, 0.0F, 0.0F, -beamRadius);
			ps.popPose();

			// Render glow around main beam
			ps.translate(0, yOffset, 0);
			ps.translate(0, 1, 0);
			ps.mulPose(Axis.XP.rotationDegrees(180));
			renderPart(ps, builder, R, G, B, finalAlpha * 0.4f, beamHeight, -glowRadius, -glowRadius, glowRadius, -glowRadius, -beamRadius, glowRadius, glowRadius, glowRadius);
			ps.mulPose(Axis.XP.rotationDegrees(-180));
			renderPart(ps, builder, R, G, B, finalAlpha * 0.4f, beamHeight, -glowRadius, -glowRadius, glowRadius, -glowRadius, -beamRadius, glowRadius, glowRadius, glowRadius);

			ps.popPose();
		});

		if (LootBeamConfig.INSTANCE.render_nametags) {
			renderNameTag(poseStack, queue, camera, item, color);
		}
	}

	private static void renderNameTag(PoseStack poseStack, SubmitNodeCollector queue, CameraRenderState camera, ItemEntity item, Color color) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;

		if (player.isCrouching() || (LootBeamConfig.INSTANCE.render_nametags_onlook && isLookingAt(player, item, LootBeamConfig.INSTANCE.nametag_look_sensitivity))) {
			String itemName = StringUtil.stripColor(item.getItem().getHoverName().getString());
			if (LootBeamConfig.INSTANCE.render_stackcount) {
				int count = item.getItem().getCount();
				if (count > 1) {
					itemName = itemName + " x" + count;
				}
			}

			double yOffset = LootBeamConfig.INSTANCE.nametag_y_offset;
			Vec3 pos = new Vec3(0.0D, Math.min(1.0D, player.distanceToSqr(item) * 0.025D) + yOffset, 0.0D);
			queue.submitNameTag(poseStack, pos, 0, Component.literal(itemName), !LootBeamConfig.INSTANCE.borders, 15728880, LootBeamConfig.INSTANCE.render_distance, camera);
		}
	}

	private static Color getItemColor(ItemEntity item) {
		ItemStack stack = item.getItem();
		if (LootBeamsClient.CRASH_BLACKLIST.contains(stack)) {
			return Color.WHITE;
		}

		try {
			Color override = LootBeamConfig.getColorFromItemOverrides(stack.getItem());
			if (override != null) {
				return override;
			}

			CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
			if (customData != null && !customData.isEmpty()) {
				var tag = customData.copyTag();
				if (tag.contains("lootbeams.color")) {
					return Color.decode(tag.getString("lootbeams.color").orElse(""));
				}
			}

			if (LootBeamConfig.INSTANCE.render_name_color) {
				Color nameColor = getRawColor(stack.getHoverName());
				if (!nameColor.equals(Color.WHITE)) {
					return nameColor;
				}
			}

			if (LootBeamConfig.INSTANCE.render_rarity_color) {
				var formatting = stack.getRarity().color();
				if (formatting != null) {
					var textColor = TextColor.fromLegacyFormat(formatting);
					if (textColor != null) {
						return new Color(textColor.getValue());
					}
				}
			}

			return Color.WHITE;
		} catch (Exception e) {
			LootBeamsClient.CRASH_BLACKLIST.add(stack);
			return Color.WHITE;
		}
	}

	private static Color getRawColor(Component text) {
		List<Style> list = new ArrayList<>();
		text.visit((style, string) -> {
			list.add(style);
			return Optional.empty();
		}, Style.EMPTY);
		if (!list.isEmpty() && list.get(0).getColor() != null) {
			return new Color(list.get(0).getColor().getValue());
		}
		return Color.WHITE;
	}

	private static boolean isItemInRegistryList(List<String> registryNames, Item item) {
		if (registryNames != null && !registryNames.isEmpty()) {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
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

	private static void renderPart(PoseStack stack, VertexConsumer builder, float red, float green, float blue, float alpha, float height, float radius_1, float radius_2, float radius_3, float radius_4, float radius_5, float radius_6, float radius_7, float radius_8) {
		PoseStack.Pose matrixentry = stack.last();
		renderQuad(matrixentry, builder, red, green, blue, alpha, height, radius_1, radius_2, radius_3, radius_4);
		renderQuad(matrixentry, builder, red, green, blue, alpha, height, radius_7, radius_8, radius_5, radius_6);
		renderQuad(matrixentry, builder, red, green, blue, alpha, height, radius_3, radius_4, radius_7, radius_8);
		renderQuad(matrixentry, builder, red, green, blue, alpha, height, radius_5, radius_6, radius_1, radius_2);
	}

	private static void renderQuad(PoseStack.Pose entry, VertexConsumer builder, float red, float green, float blue, float alpha, float y, float z1, float texu1, float z, float texu) {
		addVertex(entry, builder, red, green, blue, alpha, y, z1, texu1, 1f, 0f);
		addVertex(entry, builder, red, green, blue, alpha, 0f, z1, texu1, 1f, 1f);
		addVertex(entry, builder, red, green, blue, alpha, 0f, z, texu, 0f, 1f);
		addVertex(entry, builder, red, green, blue, alpha, y, z, texu, 0f, 0f);
	}

	private static void addVertex(PoseStack.Pose entry, VertexConsumer builder, float red, float green, float blue, float alpha, float y, float x, float z, float texu, float texv) {
		builder.addVertex(entry, x, y, z).setColor(red, green, blue, alpha).setUv(texu, texv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(entry, 0.0F, 1.0F, 0.0F);
	}

	private static boolean isLookingAt(LocalPlayer player, Entity target, double accuracy) {
		Vec3 difference = new Vec3(target.getX() - player.getX(), target.getEyeY() - player.getEyeY(), target.getZ() - player.getZ());
		double length = difference.length();
		if (length == 0) return true;
		double dot = player.getViewVector(1.0F).normalize().dot(difference.normalize());
		return dot > 1.0D - accuracy / length && !target.isInvisible();
	}
}
