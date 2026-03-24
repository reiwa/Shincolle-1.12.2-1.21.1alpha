package com.lulan.shincolle.utility;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.intermod.MetamorphHelper;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.Enums;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

@SideOnly(Side.CLIENT)
public class RenderHelper {
    private RenderHelper() {}

    public static final Random rand = new Random();

    public static void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height, double zLevel) {
        final float uScale = 0.00390625F;
        final float vScale = 0.00390625F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, (double)y + height, zLevel).tex(textureX * uScale, (textureY + height) * vScale).endVertex();
        bufferbuilder.pos((double)x + width, (double)y + height, zLevel).tex((textureX + width) * uScale, (textureY + height) * vScale).endVertex();
        bufferbuilder.pos((double)x + width, y, zLevel).tex((textureX + width) * uScale, textureY * vScale).endVertex();
        bufferbuilder.pos(x, y, zLevel).tex(textureX * uScale, textureY * vScale).endVertex();
        tessellator.draw();
    }

    @SideOnly(Side.CLIENT)
    public static void renderItemInFirstPerson(AbstractClientPlayer player, float ptick, @Nullable ItemStack stack) {
        if (player.isInvisible()) {
            return;
        }
        EnumHandSide handSide = player.getPrimaryHand();
        boolean isRightHand = handSide == EnumHandSide.RIGHT;
        float side = isRightHand ? 1.0F : -1.0F;
        Minecraft.getMinecraft().getTextureManager().bindTexture(player.getLocationSkin());
        GlStateManager.pushMatrix();
        GlStateManager.translate(side * 0.64000005F, -0.6F, -0.71999997F);
        GlStateManager.rotate(side * 45.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(side * -1.0F, 3.6F, 3.5F);
        GlStateManager.rotate(side * 120.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(200.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(side * -135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(side * 5.6F, 0.0F, 0.0F);
        if (stack != null && !stack.isEmpty() && Minecraft.getMinecraft().gameSettings.keyBindUseItem.isKeyDown()) {
            switch (stack.getMetadata()) {
                case 3:
                    GlStateManager.translate(1.3F, 4.0F, 0.0F);
                    GlStateManager.scale(3.0F, 3.0F, 3.0F);
                    GlStateManager.rotate(MathHelper.cos((player.ticksExisted + ptick) * 0.125F) * -20.0F - 60.0F, 0.0F, 0.0F, 1.0F);
                    break;
                case 4:
                    GlStateManager.rotate(70.0F, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(-20.0F, 0.0F, 0.0F, 1.0F);
                    GlStateManager.translate(-2.0F, 16.0F, 10.0F);
                    GlStateManager.scale(12.0F, 12.0F, 12.0F);
                    GlStateManager.rotate(MathHelper.cos((player.ticksExisted + ptick) * 0.1F) * -15.0F + 20.0F, 1.0F, 0.0F, 0.0F);
                    break;
                default:
                    GlStateManager.translate(13.5F, 12.5F, 2.5F);
                    GlStateManager.scale(9.0F, 9.0F, 9.0F);
                    GlStateManager.rotate(MathHelper.cos((player.ticksExisted + ptick) * 0.2F) * -15.0F - 20.0F, 1.0F, 1.0F, 0.0F);
                    break;
            }
        }
        Render<? extends Entity> renderer = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(player);
        RenderPlayer renderplayer;
        if (renderer instanceof RenderPlayer) {
            renderplayer = (RenderPlayer) renderer;
        } else {
            renderplayer = new RenderPlayer(Minecraft.getMinecraft().getRenderManager());
        }
        GlStateManager.disableCull();
        if (isRightHand) {
            renderplayer.renderRightArm(player);
        } else {
            renderplayer.renderLeftArm(player);
        }
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
    }

    public static void drawPlayerSkillIcon() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.skipRenderWorld || mc.player == null) {
            return;
        }
        EntityPlayer player = mc.player;
        BasicEntityShip ship = null;
        boolean isMorph = false;
        if (player.getRidingEntity() instanceof BasicEntityMount && ((BasicEntityMount) player.getRidingEntity()).getHostEntity() instanceof BasicEntityShip) {
            ship = (BasicEntityShip) ((BasicEntityMount) player.getRidingEntity()).getHostEntity();
        } else if (!player.getPassengers().isEmpty() && player.getPassengers().get(0) instanceof BasicEntityShip) {
            ship = (BasicEntityShip) player.getPassengers().get(0);
        } else if (CommonProxy.activeMetamorph && ConfigHandler.enableMetamorphSkill && ClientProxy.showMorphSkills) {
            BasicEntityShip morphShip = MetamorphHelper.getShipMorphEntity(player);
            if (morphShip != null) {
                ship = morphShip;
                isMorph = true;
            }
        }
        if (ship == null) {
            return;
        }
        boolean[] drawBtn = new boolean[5];
        int[] drawCD = new int[5];
        int[] drawCDMax = new int[5];
        int[] drawAirNum = new int[2];
        drawBtn[0] = ship.getStateFlag(13);
        drawBtn[1] = ship.getStateFlag(14);
        drawBtn[2] = ship.getStateFlag(15);
        drawBtn[3] = ship.getStateFlag(16);
        drawBtn[4] = isMorph;
        drawCD[0] = ship.getStateTimer(16);
        drawCD[1] = ship.getStateTimer(17);
        drawCD[2] = ship.getStateTimer(18);
        drawCD[3] = ship.getStateTimer(19);
        drawCD[4] = ship.getStateTimer(20);
        drawCDMax[0] = CombatHelper.getAttackDelay(ship.getAttrs().getAttackSpeed(), 1);
        drawCDMax[1] = CombatHelper.getAttackDelay(ship.getAttrs().getAttackSpeed(), 2);
        drawCDMax[2] = CombatHelper.getAttackDelay(ship.getAttrs().getAttackSpeed(), 3);
        drawCDMax[3] = CombatHelper.getAttackDelay(ship.getAttrs().getAttackSpeed(), 4);
        drawCDMax[4] = CombatHelper.getAttackDelay(ship.getAttrs().getAttackSpeed(), 0);
        if (ship instanceof BasicEntityShipCV) {
            drawAirNum[0] = ((BasicEntityShipCV) ship).getNumAircraftLight();
            drawAirNum[1] = ((BasicEntityShipCV) ship).getNumAircraftHeavy();
        }
        GlStateManager.pushMatrix();
        try {
            FontRenderer fr = mc.fontRenderer;
            GameSettings keySet = mc.gameSettings;
            ScaledResolution sr = new ScaledResolution(mc);
            int width = sr.getScaledWidth();
            int height = sr.getScaledHeight();
            int px = (int) (width * ConfigHandler.posHUD[0]);
            int py = (int) (height * ConfigHandler.posHUD[1]);
            for (int k = 0; k < 5; ++k) {
                if (!drawBtn[k]) continue;
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                mc.getTextureManager().bindTexture(ClientProxy.TextureGuiHUD);
                int px2 = px - 40 + k * 21;
                int len = (int) ((float) drawCD[k] / (float) drawCDMax[k] * 18.0F);
                RenderHelper.drawTexturedModalRect(px2, py, k * 18, 0, 18, 18, 0.0);
                RenderHelper.drawTexturedModalRect(px2, py + 18 - len, 0, 36 - len, 18, len, 0.0);
                if (len > 0) {
                    fr.drawStringWithShadow(String.format("%.1f", drawCD[k] * 0.05F), (float)px2 + 5, (float)py + 18, Enums.EnumColors.YELLOW.getValue());
                }
                if (keySet.keyBindsHotbar[k].isKeyDown()) {
                    Gui.drawRect(px2, py, px2 + 18, py + 18, 0x3FFFFFFF);
                }
                if (k == 2) {
                    fr.drawStringWithShadow(String.valueOf(drawAirNum[0]), (float)px2 + 7, (float)py - 8, Enums.EnumColors.GREEN.getValue());
                } else if (k == 3) {
                    fr.drawStringWithShadow(String.valueOf(drawAirNum[1]), (float)px2 + 7, (float)py - 8, Enums.EnumColors.CYAN.getValue());
                }
            }
            mc.getTextureManager().bindTexture(ClientProxy.TextureGuiHUD);
            RenderHelper.drawTexturedModalRect(width / 2, height / 2 - 4, 0, 7, 1, 9, 0.0);
            RenderHelper.drawTexturedModalRect(width / 2 - 4, height / 2, 7, 0, 9, 1, 0.0);
            mc.getTextureManager().bindTexture(Gui.ICONS);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        } catch (Exception e) {
            LogHelper.info("EXCEPTION: render game overlay fail: " + e);
            e.printStackTrace();
        } finally {
            GlStateManager.popMatrix();
        }
    }
}