package com.lootbeams;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LootBeamRenderer {

	private static final Identifier LOOT_BEAM_TEXTURE = Identifier.of(LootBeamsClient.MODID, "textures/entity/loot_beam.png");

	public static void renderLootBeam(MatrixStack matrixStack, VertexConsumerProvider buffer, float pticks, long worldtime, ItemEntity item) {
		if (!LootBeamConfig.INSTANCE.enabled) return;
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
				Item it = stack.getItem();
				if (it instanceof ArmorItem || it instanceof SwordItem || it instanceof ToolItem
						|| it instanceof BowItem || it instanceof CrossbowItem || it instanceof ShieldItem
						|| it instanceof TridentItem || it instanceof FishingRodItem) {
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
		if (distSq < 1.0D) {
			beamAlpha *= Math.max(0.25f, (float) Math.sqrt(distSq));
		}
		final float finalAlpha = beamAlpha;

		float beamRadius = 0.05f * (float) LootBeamConfig.INSTANCE.beam_radius;
		float glowRadius = beamRadius + (beamRadius * 0.2f);
		float yOffset = (float) LootBeamConfig.INSTANCE.beam_y_offset;

		Color color = getItemColor(item);

		renderSegmentedBeam(
				matrixStack,
				buffer,
				color,
				finalAlpha,
				pticks,
				worldtime,
				yOffset,
				beamRadius,
				glowRadius,
				(float) LootBeamConfig.INSTANCE.beam_height
		);

		if (LootBeamConfig.INSTANCE.render_nametags) {
			renderNameTag(matrixStack, buffer, item, color);
		}
	}

	private static int toArgb(int a, int r, int g, int b) {
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	private static void renderSegmentedBeam(
			MatrixStack matrixStack,
			VertexConsumerProvider buffer,
			Color color,
			float finalAlpha,
			float pticks,
			long worldtime,
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

		float animTime = (float) Math.floorMod(worldtime, 40L) + pticks;
		float anim = -animTime;
		float vOffset = MathHelper.fractionalPart(anim * 0.2F - (float) MathHelper.floor(anim * 0.1F));
		float vBase = -1.0F + vOffset;

		matrixStack.push();
		matrixStack.translate(0.0D, yOffset, 0.0D);

		// 1. Inner solid beam (rotating diamond)
		float innerTexScale = 0.5F / innerRadius;
		matrixStack.push();
		matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(animTime * 2.25F - 45.0F));

		float inC1x = 0.0F, inC1z = innerRadius;
		float inC2x = innerRadius, inC2z = 0.0F;
		float inC3x = 0.0F, inC3z = -innerRadius;
		float inC4x = -innerRadius, inC4z = 0.0F;

		VertexConsumer innerConsumer = buffer.getBuffer(RenderLayer.getBeaconBeam(LOOT_BEAM_TEXTURE, false));
		MatrixStack.Entry innerEntry = matrixStack.peek();
		renderBeamPrism(innerEntry, innerConsumer, inC1x, inC1z, inC2x, inC2z, inC3x, inC3z, inC4x, inC4z, y0, y1, c0, c1, vBase, innerTexScale);
		renderBeamPrism(innerEntry, innerConsumer, inC1x, inC1z, inC2x, inC2z, inC3x, inC3z, inC4x, inC4z, y1, y2, c1, c2, vBase, innerTexScale);
		matrixStack.pop();

		// 2. Outer translucent glow beam (axis-aligned square)
		float glowTexScale = 1.0F;
		float glC1x = -glowRadius, glC1z = -glowRadius;
		float glC2x = glowRadius, glC2z = -glowRadius;
		float glC3x = glowRadius, glC3z = glowRadius;
		float glC4x = -glowRadius, glC4z = glowRadius;

		VertexConsumer glowConsumer = buffer.getBuffer(RenderLayer.getBeaconBeam(LOOT_BEAM_TEXTURE, true));
		MatrixStack.Entry glowEntry = matrixStack.peek();
		renderBeamPrism(glowEntry, glowConsumer, glC1x, glC1z, glC2x, glC2z, glC3x, glC3z, glC4x, glC4z, y0, y1, c0, c1, vBase, glowTexScale);
		renderBeamPrism(glowEntry, glowConsumer, glC1x, glC1z, glC2x, glC2z, glC3x, glC3z, glC4x, glC4z, y1, y2, c1, c2, vBase, glowTexScale);

		matrixStack.pop();
	}

	private static void renderBeamPrism(
			MatrixStack.Entry entry,
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

		renderQuad(entry, builder, bottomColor, topColor, minY, maxY, x1, z1, x2, z2, u1, u2, v1, v2);
		renderQuad(entry, builder, bottomColor, topColor, minY, maxY, x3, z3, x4, z4, u1, u2, v1, v2);
		renderQuad(entry, builder, bottomColor, topColor, minY, maxY, x2, z2, x3, z3, u1, u2, v1, v2);
		renderQuad(entry, builder, bottomColor, topColor, minY, maxY, x4, z4, x1, z1, u1, u2, v1, v2);
	}

	private static void renderQuad(
			MatrixStack.Entry entry,
			VertexConsumer builder,
			int bottomColor, int topColor,
			float minY, float maxY,
			float x1, float z1,
			float x2, float z2,
			float u1, float u2,
			float v1, float v2
	) {
		addVertex(entry, builder, topColor, maxY, x1, z1, u2, v2);
		addVertex(entry, builder, bottomColor, minY, x1, z1, u2, v1);
		addVertex(entry, builder, bottomColor, minY, x2, z2, u1, v1);
		addVertex(entry, builder, topColor, maxY, x2, z2, u1, v2);
	}

	private static void addVertex(
			MatrixStack.Entry entry,
			VertexConsumer builder,
			int color,
			float y, float x, float z,
			float u, float v
	) {
		builder.vertex(entry, x, y, z)
				.color(color)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(15728880)
				.normal(entry, 0.0F, 1.0F, 0.0F);
	}

	private static void renderNameTag(MatrixStack matrixStack, VertexConsumerProvider buffer, ItemEntity item, Color color) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;

		if (player.isSneaking() || (LootBeamConfig.INSTANCE.render_nametags_onlook && isLookingAt(player, item, LootBeamConfig.INSTANCE.nametag_look_sensitivity))) {
			float foregroundAlpha = (float) LootBeamConfig.INSTANCE.nametag_text_alpha;
			float backgroundAlpha = (float) LootBeamConfig.INSTANCE.nametag_background_alpha;
			double yOffset = LootBeamConfig.INSTANCE.nametag_y_offset;
			int foregroundColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * foregroundAlpha)).getRGB();
			int backgroundColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * backgroundAlpha)).getRGB();

			matrixStack.push();
			matrixStack.translate(0.0D, Math.min(1.0D, player.squaredDistanceTo(item) * 0.025D) + yOffset, 0.0D);
			matrixStack.multiply(MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation());

			float nametagScale = (float) LootBeamConfig.INSTANCE.nametag_scale;
			matrixStack.scale(-0.02F * nametagScale, -0.02F * nametagScale, 0.02F * nametagScale);

			TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
			String itemName = StringHelper.stripTextFormat(item.getStack().getName().getString());
			if (LootBeamConfig.INSTANCE.render_stackcount) {
				int count = item.getStack().getCount();
				if (count > 1) {
					itemName = itemName + " x" + count;
				}
			}

			matrixStack.translate(0, 0, -10);
			renderText(textRenderer, matrixStack, buffer, itemName, foregroundColor, backgroundColor, backgroundAlpha);
			matrixStack.pop();
		}
	}

	private static void renderText(TextRenderer textRenderer, MatrixStack matrixStack, VertexConsumerProvider buffer, String text, int foregroundColor, int backgroundColor, float backgroundAlpha) {
		float x = -textRenderer.getWidth(text) / 2.0F;
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		if (LootBeamConfig.INSTANCE.borders) {
			int bg = new Color(0, 0, 0, (int) (255 * backgroundAlpha)).getRGB();
			textRenderer.draw(Text.literal(text), x + 1, 0, bg, false, matrix, buffer, TextRenderer.TextLayerType.NORMAL, 0, 15728880);
			textRenderer.draw(Text.literal(text), x - 1, 0, bg, false, matrix, buffer, TextRenderer.TextLayerType.NORMAL, 0, 15728880);
			textRenderer.draw(Text.literal(text), x, 1, bg, false, matrix, buffer, TextRenderer.TextLayerType.NORMAL, 0, 15728880);
			textRenderer.draw(Text.literal(text), x, -1, bg, false, matrix, buffer, TextRenderer.TextLayerType.NORMAL, 0, 15728880);

			matrixStack.push();
			matrixStack.translate(0.0D, 0.0D, -0.01D);
			textRenderer.draw(Text.literal(text), x, 0, foregroundColor, false, matrixStack.peek().getPositionMatrix(), buffer, TextRenderer.TextLayerType.NORMAL, 0, 15728880);
			matrixStack.pop();
		} else {
			textRenderer.draw(Text.literal(text), x, 0, foregroundColor, false, matrix, buffer, TextRenderer.TextLayerType.SEE_THROUGH, backgroundColor, 15728880);
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
			if (customData != null && customData.contains("lootbeams.color")) {
				return Color.decode(customData.copyNbt().getString("lootbeams.color"));
			}

			PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
			if (potionContents != null) {
				int potionColor = potionContents.getColor();
				if (potionColor != -1) {
					return new Color(potionColor);
				}
			}

			DyedColorComponent dyedColor = stack.get(DataComponentTypes.DYED_COLOR);
			if (dyedColor != null) {
				return new Color(dyedColor.rgb());
			}

			if (LootBeamConfig.INSTANCE.render_name_color) {
				Color nameColor = getRawColor(stack.getName());
				if (!nameColor.equals(Color.WHITE)) {
					return nameColor;
				}
			}

			if (LootBeamConfig.INSTANCE.render_rarity_color && stack.getRarity() != Rarity.COMMON) {
				Formatting fmt = stack.getRarity().getFormatting();
				if (fmt != null && fmt.getColorValue() != null) {
					return new Color(fmt.getColorValue());
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
		Identifier id = Registries.ITEM.getId(item);
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

	private static boolean isLookingAt(ClientPlayerEntity player, Entity target, double accuracy) {
		Vec3d difference = new Vec3d(target.getX() - player.getX(), target.getEyeY() - player.getEyeY(), target.getZ() - player.getZ());
		double length = difference.length();
		if (length == 0) return true;
		double dot = player.getRotationVec(1.0F).normalize().dotProduct(difference.normalize());
		return dot > 1.0D - accuracy / length && !target.isInvisible();
	}
}
