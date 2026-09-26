package com.lootbeams;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
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
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LootBeamRenderer {

	private static final Identifier LOOT_BEAM_TEXTURE = Identifier.fromNamespaceAndPath(LootBeamsClient.MODID, "textures/entity/loot_beam.png");

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
		if (distSq < 1.0D) {
			beamAlpha *= Math.max(0.25f, (float) Math.sqrt(distSq));
		}
		final float finalAlpha = beamAlpha;

		float beamRadius = 0.05f * (float) LootBeamConfig.INSTANCE.beam_radius;
		float glowRadius = beamRadius + (beamRadius * 0.2f);
		float yOffset = (float) LootBeamConfig.INSTANCE.beam_y_offset;

		Color color = getItemColor(item);
		float animTime = (float) Math.floorMod(item.level().getGameTime(), 40L);

		renderSegmentedBeam(
				poseStack,
				queue,
				color,
				finalAlpha,
				animTime,
				yOffset,
				beamRadius,
				glowRadius,
				(float) LootBeamConfig.INSTANCE.beam_height
		);

		if (LootBeamConfig.INSTANCE.render_nametags) {
			renderNameTag(poseStack, queue, camera, item, color);
		}
	}

	private static int toArgb(int a, int r, int g, int b) {
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	private static void renderSegmentedBeam(
			PoseStack poseStack,
			SubmitNodeCollector queue,
			Color color,
			float finalAlpha,
			float animTime,
			float yOffset,
			float innerRadius,
			float glowRadius,
			float heightScale
	) {
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();

		float y0 = 0.5F;
		float y1 = y0 + 0.10F;
		float y2 = y0 + Math.max(0.5F, (float) (1.4D * heightScale));

		int c0 = toArgb(0, r, g, b);
		int c1 = toArgb((int) (finalAlpha * 255), r, g, b);
		int c2 = toArgb((int) (finalAlpha * 255), r, g, b);

		float anim = -animTime;
		float vOffset = Mth.frac(anim * 0.2F - (float) Mth.floor(anim * 0.1F));
		float vBase = -1.0F + vOffset;

		poseStack.pushPose();
		poseStack.translate(0.0D, yOffset, 0.0D);

		// 1. Inner solid beam (rotating diamond)
		float innerTexScale = 0.5F / innerRadius;
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, animTime * 2.25F - 45.0F);

		float inC1x = 0.0F, inC1z = innerRadius;
		float inC2x = innerRadius, inC2z = 0.0F;
		float inC3x = 0.0F, inC3z = -innerRadius;
		float inC4x = -innerRadius, inC4z = 0.0F;

		queue.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(LOOT_BEAM_TEXTURE, false), (pose, builder) -> {
			renderBeamPrism(pose, builder, inC1x, inC1z, inC2x, inC2z, inC3x, inC3z, inC4x, inC4z, y0, y1, c0, c1, vBase, innerTexScale);
			renderBeamPrism(pose, builder, inC1x, inC1z, inC2x, inC2z, inC3x, inC3z, inC4x, inC4z, y1, y2, c1, c2, vBase, innerTexScale);
		});
		poseStack.popPose();

		// 2. Outer translucent glow beam (axis-aligned square)
		float glowTexScale = 1.0F;
		float glC1x = -glowRadius, glC1z = -glowRadius;
		float glC2x = glowRadius, glC2z = -glowRadius;
		float glC3x = glowRadius, glC3z = glowRadius;
		float glC4x = -glowRadius, glC4z = glowRadius;

		queue.submitCustomGeometry(poseStack, RenderTypes.beaconBeam(LOOT_BEAM_TEXTURE, true), (pose, builder) -> {
			renderBeamPrism(pose, builder, glC1x, glC1z, glC2x, glC2z, glC3x, glC3z, glC4x, glC4z, y0, y1, c0, c1, vBase, glowTexScale);
			renderBeamPrism(pose, builder, glC1x, glC1z, glC2x, glC2z, glC3x, glC3z, glC4x, glC4z, y1, y2, c1, c2, vBase, glowTexScale);
		});

		poseStack.popPose();
	}

	private static void renderBeamPrism(
			PoseStack.Pose pose,
			VertexConsumer builder,
			float x1, float z1,
			float x2, float z2,
			float x3, float z3,
			float x4, float z4,
			float minY, float maxY,
			int bottomColor, int topColor,
			float vBase, float texScale
	) {
		float u1 = 0.0F;
		float u2 = 1.0F;
		float v1 = minY * texScale + vBase;
		float v2 = maxY * texScale + vBase;

		renderQuad(pose, builder, bottomColor, topColor, minY, maxY, x1, z1, x2, z2, u1, u2, v1, v2);
		renderQuad(pose, builder, bottomColor, topColor, minY, maxY, x3, z3, x4, z4, u1, u2, v1, v2);
		renderQuad(pose, builder, bottomColor, topColor, minY, maxY, x2, z2, x3, z3, u1, u2, v1, v2);
		renderQuad(pose, builder, bottomColor, topColor, minY, maxY, x4, z4, x1, z1, u1, u2, v1, v2);
	}

	private static void renderQuad(
			PoseStack.Pose pose,
			VertexConsumer builder,
			int bottomColor, int topColor,
			float minY, float maxY,
			float x1, float z1,
			float x2, float z2,
			float u1, float u2,
			float v1, float v2
	) {
		addVertex(pose, builder, topColor, maxY, x1, z1, u2, v2);
		addVertex(pose, builder, bottomColor, minY, x1, z1, u2, v1);
		addVertex(pose, builder, bottomColor, minY, x2, z2, u1, v1);
		addVertex(pose, builder, topColor, maxY, x2, z2, u1, v2);
	}

	private static void addVertex(
			PoseStack.Pose pose,
			VertexConsumer builder,
			int color,
			float y, float x, float z,
			float u, float v
	) {
		builder.addVertex(pose, x, y, z)
				.setColor(color)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(15728880)
				.setNormal(pose, 0.0F, 1.0F, 0.0F);
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
			Component textComponent = Component.literal(itemName).withStyle(Style.EMPTY.withColor(color.getRGB() & 0xFFFFFF));
			queue.submitNameTag(poseStack, pos, 0, textComponent, !LootBeamConfig.INSTANCE.borders, 15728880, camera);
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

			PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
			if (potionContents != null) {
				int potionColor = potionContents.getColor();
				if (potionColor != -1) {
					return new Color(potionColor);
				}
			}

			DyedItemColor dyedColor = stack.get(DataComponents.DYED_COLOR);
			if (dyedColor != null) {
				return new Color(dyedColor.rgb());
			}

			if (LootBeamConfig.INSTANCE.render_name_color) {
				Color nameColor = getRawColor(stack.getHoverName());
				if (!nameColor.equals(Color.WHITE)) {
					return nameColor;
				}
			}

			if (LootBeamConfig.INSTANCE.render_rarity_color && stack.getRarity() != Rarity.COMMON) {
				var formatting = stack.getRarity().color();
				if (formatting != null) {
					var textColor = TextColor.fromLegacyFormat(formatting);
					if (textColor != null) {
						return new Color(textColor.getValue());
					}
				}
			}

			Color materialColor = getMaterialColor(stack.getItem());
			if (!materialColor.equals(Color.WHITE)) {
				return materialColor;
			}

			return Color.WHITE;
		} catch (Exception e) {
			LootBeamsClient.CRASH_BLACKLIST.add(stack);
			return Color.WHITE;
		}
	}

	private static Color getMaterialColor(Item item) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		String path = id.getPath();

		if (path.contains("netherite")) return new Color(0x655E65);
		if (path.contains("diamond")) return new Color(0x4AEDD9);
		if (path.contains("emerald")) return new Color(0x17DD62);
		if (path.contains("gold") || path.contains("gilded")) return new Color(0xFDF55F);
		if (path.contains("copper")) return new Color(0xE77C56);
		if (path.contains("amethyst")) return new Color(0xC78BFA);
		if (path.contains("redstone")) return new Color(0xFF2200);
		if (path.contains("lapis")) return new Color(0x254FC7);
		if (path.contains("iron")) return new Color(0xD8D8D8);
		if (path.contains("ender_pearl") || path.contains("eye_of_ender")) return new Color(0x1B8272);
		if (path.contains("echo_shard") || path.contains("sculk")) return new Color(0x056B7A);
		if (path.contains("blaze")) return new Color(0xFFAA00);
		if (path.contains("slime")) return new Color(0x7AC764);
		if (path.contains("prismarine")) return new Color(0x5B9C8E);
		if (path.contains("glowstone")) return new Color(0xFFBC5E);
		if (path.contains("quartz")) return new Color(0xEAE5DE);
		if (path.contains("coal") || path.contains("charcoal")) return new Color(0x383838);
		if (path.contains("totem")) return new Color(0xE2B024);
		if (path.contains("apple")) return new Color(0xE82323);

		return Color.WHITE;
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


	private static boolean isLookingAt(LocalPlayer player, Entity target, double accuracy) {
		Vec3 difference = new Vec3(target.getX() - player.getX(), target.getEyeY() - player.getEyeY(), target.getZ() - player.getZ());
		double length = difference.length();
		if (length == 0) return true;
		double dot = player.getViewVector(1.0F).normalize().dot(difference.normalize());
		return dot > 1.0D - accuracy / length && !target.isInvisible();
	}
}
