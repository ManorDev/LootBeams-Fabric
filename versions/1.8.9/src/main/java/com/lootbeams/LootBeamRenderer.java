package com.lootbeams;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.List;

public class LootBeamRenderer {

	private static final Identifier LOOT_BEAM_TEXTURE = new Identifier("lootbeams", "textures/entity/loot_beam.png");

	public static void renderLootBeam(ItemEntity item, double x, double y, double z, float tickDelta) {
		if (!LootBeamConfig.INSTANCE.enabled) return;
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null) return;

		double distSq = player.squaredDistanceTo(item);
		if (distSq > LootBeamConfig.INSTANCE.render_distance * LootBeamConfig.INSTANCE.render_distance) {
			return;
		}

		boolean shouldRender = false;
		if (LootBeamConfig.INSTANCE.all_items) {
			shouldRender = true;
		} else {
			ItemStack stack = item.getItemStack();
			if (stack != null) {
				if (LootBeamConfig.INSTANCE.only_equipment) {
					Item it = stack.getItem();
					if (it instanceof ArmorItem || it instanceof SwordItem || it instanceof ToolItem
							|| it instanceof BowItem || it instanceof FishingRodItem || it instanceof ShearsItem) {
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
		}

		if (item.getItemStack() != null && isItemInRegistryList(LootBeamConfig.INSTANCE.blacklist, item.getItemStack().getItem())) {
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
		long worldtime = item.world != null ? item.world.getLastUpdateTime() : 0L;
		float animTime = (float) Math.floorMod(worldtime, 40L) + tickDelta;

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) x, (float) y, (float) z);

		client.getTextureManager().bindTexture(LOOT_BEAM_TEXTURE);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 10497.0F);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 10497.0F);

		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.depthMask(false);
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);

		renderSegmentedBeam(
				color,
				finalAlpha,
				animTime,
				yOffset,
				beamRadius,
				glowRadius,
				(float) LootBeamConfig.INSTANCE.beam_height
		);

		GlStateManager.enableLighting();
		GlStateManager.enableCull();
		GlStateManager.disableBlend();
		GlStateManager.depthMask(true);
		GlStateManager.popMatrix();

		if (LootBeamConfig.INSTANCE.render_nametags) {
			renderNameTag(item, x, y, z, color);
		}
	}

	private static int toArgb(int a, int r, int g, int b) {
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	private static void renderSegmentedBeam(
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
		float vOffset = (float) MathHelper.fractionalPart((double) anim * 0.2D - (double) MathHelper.floor((double) anim * 0.1D));
		float vBase = -1.0F + vOffset;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();

		GlStateManager.pushMatrix();
		GlStateManager.translate(0.0F, yOffset, 0.0F);

		// 1. Inner solid beam (rotating diamond)
		float innerTexScale = 0.5F / innerRadius;
		GlStateManager.pushMatrix();
		GlStateManager.rotate(animTime * 2.25F - 45.0F, 0.0F, 1.0F, 0.0F);

		float inC1x = 0.0F, inC1z = innerRadius;
		float inC2x = innerRadius, inC2z = 0.0F;
		float inC3x = 0.0F, inC3z = -innerRadius;
		float inC4x = -innerRadius, inC4z = 0.0F;

		buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
		renderBeamPrism(buffer, inC1x, inC1z, inC2x, inC2z, inC3x, inC3z, inC4x, inC4z, y0, y1, c0, c1, vBase, innerTexScale);
		renderBeamPrism(buffer, inC1x, inC1z, inC2x, inC2z, inC3x, inC3z, inC4x, inC4z, y1, y2, c1, c2, vBase, innerTexScale);
		tessellator.draw();
		GlStateManager.popMatrix();

		// 2. Outer translucent glow beam (axis-aligned square)
		float glowTexScale = 1.0F;
		float glC1x = -glowRadius, glC1z = -glowRadius;
		float glC2x = glowRadius, glC2z = -glowRadius;
		float glC3x = glowRadius, glC3z = glowRadius;
		float glC4x = -glowRadius, glC4z = glowRadius;

		buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
		renderBeamPrism(buffer, glC1x, glC1z, glC2x, glC2z, glC3x, glC3z, glC4x, glC4z, y0, y1, c0, c1, vBase, glowTexScale);
		renderBeamPrism(buffer, glC1x, glC1z, glC2x, glC2z, glC3x, glC3z, glC4x, glC4z, y1, y2, c1, c2, vBase, glowTexScale);
		tessellator.draw();

		GlStateManager.popMatrix();
	}

	private static void renderBeamPrism(
			BufferBuilder buffer,
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

		renderQuad(buffer, bottomColor, topColor, minY, maxY, x1, z1, x2, z2, u1, u2, v1, v2);
		renderQuad(buffer, bottomColor, topColor, minY, maxY, x3, z3, x4, z4, u1, u2, v1, v2);
		renderQuad(buffer, bottomColor, topColor, minY, maxY, x2, z2, x3, z3, u1, u2, v1, v2);
		renderQuad(buffer, bottomColor, topColor, minY, maxY, x4, z4, x1, z1, u1, u2, v1, v2);
	}

	private static void renderQuad(
			BufferBuilder buffer,
			int bottomColor, int topColor,
			float minY, float maxY,
			float x1, float z1,
			float x2, float z2,
			float u1, float u2,
			float v1, float v2
	) {
		addVertex(buffer, topColor, maxY, x1, z1, u2, v2);
		addVertex(buffer, bottomColor, minY, x1, z1, u2, v1);
		addVertex(buffer, bottomColor, minY, x2, z2, u1, v1);
		addVertex(buffer, topColor, maxY, x2, z2, u1, v2);
	}

	private static void addVertex(
			BufferBuilder buffer,
			int argb,
			float y, float x, float z,
			float u, float v
	) {
		int a = (argb >> 24) & 0xFF;
		int r = (argb >> 16) & 0xFF;
		int g = (argb >> 8) & 0xFF;
		int b = argb & 0xFF;
		buffer.vertex(x, y, z).texture(u, v).color(r, g, b, a).next();
	}

	private static void renderNameTag(
			ItemEntity item,
			double x, double y, double z,
			Color color
	) {
		MinecraftClient client = MinecraftClient.getInstance();
		Entity cameraEntity = client.getCameraEntity();
		if (cameraEntity == null) cameraEntity = client.player;
		if (cameraEntity == null) return;

		double distSq = item.squaredDistanceTo(cameraEntity);
		if (distSq > LootBeamConfig.INSTANCE.render_distance * LootBeamConfig.INSTANCE.render_distance) return;

		if (LootBeamConfig.INSTANCE.render_nametags_onlook) {
			Vec3d cameraLook = cameraEntity.getRotationVector(1.0F);
			Vec3d toItem = new Vec3d(item.x - cameraEntity.x, (item.y + item.height / 2.0) - (cameraEntity.y + cameraEntity.getEyeHeight()), item.z - cameraEntity.z);
			double length = toItem.length();
			if (length > 0.001) {
				toItem = toItem.normalize();
				double dot = cameraLook.dotProduct(toItem);
				if (dot < 1.0 - LootBeamConfig.INSTANCE.nametag_look_sensitivity) {
					return;
				}
			}
		}

		ItemStack stack = item.getItemStack();
		if (stack == null) return;

		String text = stack.hasCustomName() ? stack.getCustomName() : stack.toHoverableText().asFormattedString();
		if (LootBeamConfig.INSTANCE.render_stackcount && stack.count > 1) {
			text = text + " x" + stack.count;
		}

		EntityRenderDispatcher dispatcher = client.getEntityRenderManager();
		TextRenderer textRenderer = client.textRenderer;

		float scale = 0.025F * (float) LootBeamConfig.INSTANCE.nametag_scale;

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) x, (float) y + item.height + (float) LootBeamConfig.INSTANCE.nametag_y_offset, (float) z);
		GlStateManager.rotate(-dispatcher.yaw, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotate(dispatcher.pitch, 1.0F, 0.0F, 0.0F);
		GlStateManager.scale(-scale, -scale, scale);

		GlStateManager.disableLighting();
		GlStateManager.depthMask(false);
		GlStateManager.disableDepthTest();
		GlStateManager.enableBlend();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);

		int strWidth = textRenderer.getStringWidth(text);
		int halfWidth = strWidth / 2;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		GlStateManager.disableTexture();
		buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
		int bgAlpha = (int) (LootBeamConfig.INSTANCE.nametag_background_alpha * 255);
		buffer.vertex(-halfWidth - 2, -2, 0.0).color(0, 0, 0, bgAlpha).next();
		buffer.vertex(-halfWidth - 2, 9, 0.0).color(0, 0, 0, bgAlpha).next();
		buffer.vertex(halfWidth + 2, 9, 0.0).color(0, 0, 0, bgAlpha).next();
		buffer.vertex(halfWidth + 2, -2, 0.0).color(0, 0, 0, bgAlpha).next();
		tessellator.draw();

		if (LootBeamConfig.INSTANCE.borders) {
			GL11.glLineWidth(1.0F);
			buffer.begin(GL11.GL_LINE_LOOP, VertexFormats.POSITION_COLOR);
			buffer.vertex(-halfWidth - 2, -2, 0.0).color(color.getRed(), color.getGreen(), color.getBlue(), 255).next();
			buffer.vertex(-halfWidth - 2, 9, 0.0).color(color.getRed(), color.getGreen(), color.getBlue(), 255).next();
			buffer.vertex(halfWidth + 2, 9, 0.0).color(color.getRed(), color.getGreen(), color.getBlue(), 255).next();
			buffer.vertex(halfWidth + 2, -2, 0.0).color(color.getRed(), color.getGreen(), color.getBlue(), 255).next();
			tessellator.draw();
		}

		GlStateManager.enableTexture();
		int textAlpha = (int) (LootBeamConfig.INSTANCE.nametag_text_alpha * 255);
		int textColor = (textAlpha << 24) | (color.getRGB() & 0xFFFFFF);
		textRenderer.draw(text, -halfWidth, 0, textColor, false);

		GlStateManager.enableDepthTest();
		GlStateManager.depthMask(true);
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	private static Color getItemColor(ItemEntity item) {
		ItemStack stack = item.getItemStack();
		if (stack == null) return Color.WHITE;

		// 1. Config override
		Color override = LootBeamConfig.getColorFromItemOverrides(stack.getItem());
		if (override != null) return override;

		// 2. Custom NBT color
		if (stack.hasNbt() && stack.getNbt().contains("lootbeams.color")) {
			try {
				return Color.decode(stack.getNbt().getString("lootbeams.color"));
			} catch (Exception ignored) {}
		}

		// 3. Potion item
		if (stack.getItem() instanceof PotionItem) {
			PotionItem potion = (PotionItem) stack.getItem();
			int pColor = potion.getColor(stack.getData());
			if (pColor != 0) {
				return new Color(pColor);
			}
		}

		// 4. Dyed leather armor
		if (stack.getItem() instanceof ArmorItem) {
			ArmorItem armor = (ArmorItem) stack.getItem();
			if (armor.hasColor(stack)) {
				return new Color(armor.getColor(stack));
			}
		}

		// 5. Rarity colors
		Rarity rarity = stack.getRarity();
		if (rarity != null && rarity != Rarity.COMMON) {
			if (rarity == Rarity.UNCOMMON) return new Color(0xFFFF55);
			if (rarity == Rarity.RARE) return new Color(0x55FFFF);
			if (rarity == Rarity.EPIC) return new Color(0xFF55FF);
		}

		// 6. Material keyword matching
		Identifier id = Item.REGISTRY.getIdentifier(stack.getItem());
		if (id != null) {
			String name = id.getPath().toLowerCase();
			if (name.contains("diamond")) return new Color(0x4AEDD9);
			if (name.contains("emerald")) return new Color(0x17DD62);
			if (name.contains("gold") || name.contains("gilded")) return new Color(0xFDF55F);
			if (name.contains("iron")) return new Color(0xD8D8D8);
			if (name.contains("redstone")) return new Color(0xFF2200);
			if (name.contains("lapis")) return new Color(0x254FC7);
			if (name.contains("coal") || name.contains("charcoal")) return new Color(0x383838);
			if (name.contains("quartz")) return new Color(0xEAE5DE);
			if (name.contains("prismarine")) return new Color(0x5B9C8E);
			if (name.contains("glowstone")) return new Color(0xFFBC5E);
			if (name.contains("slime")) return new Color(0x7AC764);
			if (name.contains("blaze")) return new Color(0xFFAA00);
			if (name.contains("ender")) return new Color(0x1B8272);
			if (name.contains("apple")) return new Color(0xE82323);
		}

		return Color.WHITE;
	}

	private static boolean isItemInRegistryList(List<String> list, Item item) {
		if (list == null || list.isEmpty() || item == null) return false;
		Identifier id = Item.REGISTRY.getIdentifier(item);
		if (id == null) return false;
		String idStr = id.toString();
		for (String entry : list) {
			if (entry == null || entry.isEmpty()) continue;
			if (entry.equals(idStr)) return true;
			if (!entry.contains(":") && entry.equals(id.getNamespace())) return true;
		}
		return false;
	}
}
