package com.lulan.shincolle.utility;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.crafting.ShipCalc;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipCV;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.item.IShipCombatRation;
import com.lulan.shincolle.item.IShipFoodItem;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;

public class InteractHelper {

    private InteractHelper() {}

    public static boolean interactModernKit(final BasicEntityShip ship, final EntityPlayer player, final ItemStack stack) {
        if (!ship.getAttrs().addAttrsBonusRandom(ship.getRNG())) {
            return false;
        }
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        ship.setStateEmotion(1, 8, true);
        ship.calcShipAttributes(1, true);
        ship.playSound(BasicEntityShip.getCustomSound(4, ship), ConfigHandler.volumeShip, 1.0F);
        return true;
    }

    public static boolean interactPointer(final BasicEntityShip ship, final EntityPlayer player) {
        ship.setAITarget(player);
        if (TeamHelper.checkSameOwner(player, ship) && !ship.getStateFlag(2)) {
            if (ship.getMorale() < 6630.0F) {
                ship.addMorale(ConfigHandler.baseCaressMorale);
            }
            ship.applyEmotesReaction(0);
        } else {
            ship.applyEmotesReaction(1);
        }
        ship.setAITarget(null);
        return true;
    }

    public static boolean interactBucket(final BasicEntityShip ship, final EntityPlayer player, final ItemStack stack) {
        if (ship.getHealth() >= ship.getMaxHealth()) {
            return false;
        }
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        if (ship instanceof BasicEntityShipSmall) {
            ship.heal(ship.getMaxHealth() * 0.1F + 5.0F);
        } else {
            ship.heal(ship.getMaxHealth() * 0.05F + 10.0F);
        }
        if (ship instanceof BasicEntityShipCV) {
            final BasicEntityShipCV shipCV = (BasicEntityShipCV) ship;
            shipCV.setNumAircraftLight(shipCV.getNumAircraftLight() + 1);
            shipCV.setNumAircraftHeavy(shipCV.getNumAircraftHeavy() + 1);
        }
        return true;
    }

    public static boolean interactWeddingRing(final BasicEntityShip ship, final EntityPlayer player, final ItemStack stack) {
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        ship.setStateFlag(1, true);
        final CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
        if (capa != null) {
            capa.setMarriageNum(capa.getMarriageNum() + 1);
        }
        final NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(ship.dimension, ship.posX, ship.posY, ship.posZ, 32.0);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(ship, 3, false), point);
        ship.playSound(BasicEntityShip.getCustomSound(4, ship), ConfigHandler.volumeShip, 1.0F);
        ship.setMorale(16000);
        ship.setStateEmotion(1, 7, true);
        for (int i = 0; i < 3; ++i) {
            ship.getAttrs().addAttrsBonusRandom(ship.getRNG());
        }
        ship.calcShipAttributes(31, true);
        return true;
    }

    public static boolean interactKaitaiHammer(final BasicEntityShip ship, final EntityPlayer player, final ItemStack stack) {
        if (!player.capabilities.isCreativeMode) {
            stack.damageItem(1, player);
            final ItemStack[] items = ShipCalc.getKaitaiItems(ship.getShipClass());
            ship.world.spawnEntity(new EntityItem(ship.world, ship.posX + 0.5, ship.posY + 0.8, ship.posZ + 0.5, items[0]));
            ship.world.spawnEntity(new EntityItem(ship.world, ship.posX + 0.5, ship.posY + 0.8, ship.posZ - 0.5, items[1]));
            ship.world.spawnEntity(new EntityItem(ship.world, ship.posX - 0.5, ship.posY + 0.8, ship.posZ + 0.5, items[2]));
            ship.world.spawnEntity(new EntityItem(ship.world, ship.posX - 0.5, ship.posY + 0.8, ship.posZ - 0.5, items[3]));
            for (int i = 0; i < ship.getCapaShipInventory().getSlots(); ++i) {
                final ItemStack invItem = ship.getCapaShipInventory().getStackInSlot(i);
                if (!invItem.isEmpty()) {
                    final float offsetX = ship.getRNG().nextFloat() * 0.8F + 0.1F;
                    final float offsetY = ship.getRNG().nextFloat() * 0.8F + 0.1F;
                    final float offsetZ = ship.getRNG().nextFloat() * 0.8F + 0.1F;
                    final EntityItem entityItem = new EntityItem(ship.world, ship.posX + offsetX, ship.posY + offsetY, ship.posZ + offsetZ, invItem.copy());
                    if (invItem.hasTagCompound()) {
                        entityItem.getItem().setTagCompound(invItem.getTagCompound().copy());
                    }
                    ship.world.spawnEntity(entityItem);
                }
            }
            ship.playSound(BasicEntityShip.getCustomSound(3, ship), ConfigHandler.volumeShip, getSoundPitch(ship));
        }
        ship.applyParticleEmotion(8);
        EntityHelper.applyShipEmotesAOE(ship.world, ship.posX, ship.posY, ship.posZ, 10.0, 6);
        ship.setDead();
        return true;
    }

    public static float getSoundPitch(final BasicEntityShip ship) {
        return (ship.getRNG().nextFloat() - ship.getRNG().nextFloat()) * 0.1F + 1.0F;
    }

    public static boolean interactOwnerPaper(final BasicEntityShip ship, final EntityPlayer player, final ItemStack itemstack) {
        final NBTTagCompound nbt = itemstack.getTagCompound();
        if (nbt == null) return false;
        final int ida = nbt.getInteger("SignIDA");
        final int idb = nbt.getInteger("SignIDB");
        if (ida <= 0 || idb <= 0) return false;
        final int idtarget = (ida == ship.getPlayerUID()) ? idb : ida;
        final EntityPlayer target = EntityHelper.getEntityPlayerByUID(idtarget);
        if (target != null && CapaTeitoku.getTeitokuCapability(target) != null) {
            ship.setPlayerUID(idtarget);
            ship.setOwnerId(target.getUniqueID());
            ship.ownerName = target.getName();
            LogHelper.debug("DEBUG: change owner: from: pid " + ship.getPlayerUID() + " uuid " + ship.getOwner().getUniqueID());
            LogHelper.debug("DEBUG: change owner: to: pid " + idtarget + " uuid " + target.getUniqueID());
            ship.sendSyncPacket((byte) 10, true);
            ship.setStateEmotion(1, 2, true);
            ship.playSound(BasicEntityShip.getCustomSound(4, ship), ConfigHandler.volumeShip, 1.0F);
            player.inventory.setInventorySlotContents(player.inventory.currentItem, ItemStack.EMPTY);
            return true;
        }
        return false;
    }

    public static boolean interactFeed(final BasicEntityShip ship, final EntityPlayer player, final ItemStack itemstack) {
        if (ship.getFoodSaturation() >= ship.getFoodSaturationMax()) {
            if (ship.getEmotesTick() <= 0) {
                ship.setEmotesTick(40);
                ship.applyParticleEmotion(ship.getRNG().nextInt(4) == 0 ? 11 : ship.getRNG().nextInt(3) * 15 + 2);
            }
            return false;
        }
        final Item item = itemstack.getItem();
        final int meta = itemstack.getMetadata();
        boolean isFood = false;
        int moraleValue = 1;
        int grudgeValue = 0;
        int ammoLightValue = 0;
        int ammoHeavyValue = 0;
        int saturationValue = 0;
        if (item instanceof ItemFood) {
            isFood = true;
            final ItemFood food = (ItemFood) item;
            float fv = food.getHealAmount(itemstack);
            final float sv = food.getSaturationModifier(itemstack);
            if (fv < 1.0F) fv = 1.0F;
            grudgeValue = moraleValue = (int) ((fv + ship.getRNG().nextInt((int) fv + 5)) * sv * 20.0F);
        } else if (item instanceof IShipFoodItem) {
            isFood = true;
            final IShipFoodItem food = (IShipFoodItem) item;
            final int foodValue = (int)food.getFoodValue(meta);
            moraleValue = foodValue + ship.getRNG().nextInt(foodValue + 1);
            final int specialEffect = food.getSpecialEffect(meta);
            switch (specialEffect) {
                case 1: grudgeValue = 300 + ship.getRNG().nextInt(500); break;
                case 2: ship.heal(ship.getMaxHealth() * 0.05F + 1.0F); break;
                case 3:
                    switch (meta) {
                        case 0: ammoLightValue = 30 + ship.getRNG().nextInt(10); break;
                        case 1: ammoLightValue = 270 + ship.getRNG().nextInt(90); break;
                        case 2: ammoHeavyValue = 15 + ship.getRNG().nextInt(5); break;
                        case 3: ammoHeavyValue = 135 + ship.getRNG().nextInt(45); break;
                        default:
                    }
                    break;
                case 4:
                    if (ship instanceof BasicEntityShipCV && ship.getRNG().nextInt(10) > 4) {
                        final BasicEntityShipCV shipCV = (BasicEntityShipCV) ship;
                        shipCV.setNumAircraftLight(shipCV.getNumAircraftLight() + 1);
                        shipCV.setNumAircraftHeavy(shipCV.getNumAircraftHeavy() + 1);
                    }
                    break;
                case 5:
                    if (ship instanceof BasicEntityShipCV) {
                        final BasicEntityShipCV shipCV = (BasicEntityShipCV) ship;
                        shipCV.setNumAircraftLight(shipCV.getNumAircraftLight() + ship.getRNG().nextInt(3) + 1);
                        shipCV.setNumAircraftHeavy(shipCV.getNumAircraftHeavy() + ship.getRNG().nextInt(3) + 1);
                    }
                    break;
                case 6:
                    saturationValue = 3;
                    grudgeValue = moraleValue;
                    moraleValue = ((IShipCombatRation) item).getMoraleValue(meta);
                    if (meta == 4 || meta == 5) BuffHelper.removeDebuffs(ship);
                    break;
                default:
            }
        } else if (item instanceof ItemPotion) {
            isFood = true;
            moraleValue = -100;
            grudgeValue = 300;
        }
        if (!isFood) return false;
        ship.setStateEmotion(1, 8, true);
        if (ship.getStateTimer(6) <= 0) {
            ship.setStateTimer(6, 20 + ship.getRNG().nextInt(20));
            ship.playSound(BasicEntityShip.getCustomSound(7, ship), ConfigHandler.volumeShip, getSoundPitch(ship));
        }
        final List<PotionEffect> pbuffs = PotionUtils.getEffectsFromStack(itemstack);
        if (!pbuffs.isEmpty()) {
            for (final PotionEffect pe : pbuffs) {
                ship.addPotionEffect(new PotionEffect(pe));
            }
            final float hp1p = Math.max(1.0F, ship.getMaxHealth() * 0.01F);
            final int healLevel = BuffHelper.checkPotionHeal(pbuffs);
            if (healLevel > 0) ship.heal((hp1p * 2.0F + 2.0F) * healLevel);
            final int dmgLevel = BuffHelper.checkPotionDamage(pbuffs);
            if (dmgLevel > 0) ship.attackEntityFrom(DamageSource.MAGIC, (hp1p * 2.0F + 2.0F) * dmgLevel);
            ship.calcShipAttributes(8, true);
        }
        ship.addMorale(moraleValue);
        ship.setFoodSaturation(ship.getFoodSaturation() + 1 + saturationValue);
        ship.addGrudge((int) (grudgeValue * ship.getAttrs().getAttrsBuffed(17)));
        ship.addAmmoLight((int) (ammoLightValue * ship.getAttrs().getAttrsBuffed(18)));
        ship.addAmmoHeavy((int) (ammoHeavyValue * ship.getAttrs().getAttrsBuffed(18)));
        if (player != null && !player.capabilities.isCreativeMode) {
            itemstack.shrink(1);
            if (itemstack.isEmpty()) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, item.getContainerItem(itemstack));
            }
        }
        if (ship.getEmotesTick() <= 0) {
            ship.setEmotesTick(40);
            switch (ship.getRNG().nextInt(3)) {
                case 0: ship.applyParticleEmotion(1); break;
                case 1: ship.applyParticleEmotion(9); break;
                case 2: ship.applyParticleEmotion(30); break;
                default:
            }
        }
        return true;
    }
}