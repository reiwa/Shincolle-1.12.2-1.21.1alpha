package com.lulan.shincolle.utility;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.entity.*;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.reference.unitclass.AttrsAdv;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAreaEffectCloud;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.*;

public class BuffHelper {

    private static final float LV_SCALE_DEF = 0.00133F;
    private static final float LV_SCALE_SPD = 0.004F;
    private static final float LV_SCALE_MOV = 0.002F;
    private static final float LV_SCALE_HIT = 0.02F;
    private static final float LV_SCALE_ATK = 0.133F;
    private static final Set<Integer> DEBUFF_POTION_IDS = new HashSet<>(Arrays.asList(2, 4, 7, 9, 15, 17, 18, 19, 20, 27));

    private BuffHelper() {}

    public static void updateAttrsRaw(final Attrs attrs, int shipClass, final float shipLevel) {
        if (attrs == null) return;
        if (!Values.ShipAttrMap.containsKey(shipClass)) shipClass = 0;
        final float[] attrBase = Values.ShipAttrMap.get(shipClass);
        final float[] attrType = attrs.getAttrsType();
        final byte[] attrBonus = attrs.getAttrsBonus();
        attrs.resetAttrsRaw();
        final float[] attrRaw = attrs.getAttrsRaw();
        attrRaw[0] = (attrBase[0] + (attrBonus[0] + 1.0F) * shipLevel * attrType[0]) * (float) ConfigHandler.scaleShip[0];
        attrRaw[5] = (attrBase[2] + (attrBonus[2] + 1.0F) * shipLevel * LV_SCALE_DEF * attrType[2]) * (float) ConfigHandler.scaleShip[2];
        attrRaw[6] = (attrBase[3] + (attrBonus[3] + 1.0F) * shipLevel * LV_SCALE_SPD * attrType[3]) * (float) ConfigHandler.scaleShip[3];
        attrRaw[7] = (attrBase[4] + (attrBonus[4] + 1.0F) * shipLevel * LV_SCALE_MOV * attrType[4]) * (float) ConfigHandler.scaleShip[4];
        attrRaw[8] = (attrBase[5] + (attrBonus[5] + 1.0F) * shipLevel * LV_SCALE_HIT * attrType[5]) * (float) ConfigHandler.scaleShip[5];
        final float baseATK = attrBase[1] + (attrBonus[1] + 1.0F) * shipLevel * LV_SCALE_ATK * attrType[1];
        attrRaw[1] = baseATK * (float) ConfigHandler.scaleShip[1];
        attrRaw[2] = baseATK * 3.0F * (float) ConfigHandler.scaleShip[1];
        attrRaw[3] = baseATK * (float) ConfigHandler.scaleShip[1];
        attrRaw[4] = baseATK * 3.0F * (float) ConfigHandler.scaleShip[1];
        attrRaw[16] = 1.0F;
        attrRaw[17] = 1.0F;
        attrRaw[18] = 1.0F;
        attrRaw[19] = 1.0F;
        attrRaw[20] = shipLevel * 0.005F;
    }

    public static void updateAttrsRawHostile(final Attrs attrs, final int shipScale, final int shipClass) {
        final double[] attrBase;
        final float[] attrMod = Values.HostileShipAttrMap.get(shipClass);
        float kb;
        attrs.resetAttrsRaw();
        final float[] attrsRaw = attrs.getAttrsRaw();
        switch (shipScale) {
            case 1:
                attrBase = ConfigHandler.scaleMobLarge;
                kb = 0.4F;
                break;
            case 2:
                attrBase = ConfigHandler.scaleBossSmall;
                kb = 0.85F;
                break;
            case 3:
                attrBase = ConfigHandler.scaleBossLarge;
                kb = 1.0F;
                break;
            default:
                attrBase = ConfigHandler.scaleMobSmall;
                kb = 0.2F;
        }
        final float baseAtk = (float) attrBase[1] * attrMod[1];
        attrsRaw[0] = (float) attrBase[0] * attrMod[0];
        attrsRaw[1] = baseAtk;
        attrsRaw[2] = baseAtk * 3.0F;
        attrsRaw[3] = baseAtk;
        attrsRaw[4] = baseAtk * 3.0F;
        attrsRaw[5] = (float) attrBase[2] * attrMod[2];
        attrsRaw[6] = (float) attrBase[3] * attrMod[3];
        attrsRaw[7] = (float) attrBase[4] * attrMod[4];
        attrsRaw[8] = (float) attrBase[5] * attrMod[5];
        attrsRaw[9] = 0.15F;
        attrsRaw[10] = 0.1F;
        attrsRaw[11] = 0.1F;
        attrsRaw[12] = 0.0F;
        attrsRaw[13] = 0.0F;
        attrsRaw[14] = 0.0F;
        attrsRaw[15] = 0.15F;
        attrsRaw[16] = 1.0F;
        attrsRaw[17] = 1.0F;
        attrsRaw[18] = 1.0F;
        attrsRaw[19] = 1.0F;
        attrsRaw[20] = kb;
    }

    public static <T extends EntityLivingBase> void updateBuffPotion(final T host) {
        BuffHelper.convertPotionToBuffMap(host);
        BuffHelper.convertBuffMapToAttrs(((IShipAttackBase) host).getBuffMap(), ((IShipAttrs) host).getAttrs());
    }

    public static <T extends EntityLivingBase> void convertPotionToBuffMap(final T host) {
        if (host == null) return;
        EntityLivingBase effectiveHost = host;
        if (host instanceof IShipMorph && ((IShipMorph) host).getMorphHost() != null) {
            effectiveHost = ((IShipMorph) host).getMorphHost();
        }
        final HashMap<Integer, Integer> buffMap = new HashMap<>();
        for (final PotionEffect potioneffect : effectiveHost.getActivePotionEffects()) {
            buffMap.put(Potion.getIdFromPotion(potioneffect.getPotion()), potioneffect.getAmplifier());
        }
        ((IShipAttackBase) host).setBuffMap(buffMap);
    }

    public static PotionEffect convertIdToPotion(final int id, final int ampLevel, final int ticks) {
        final Potion p = Potion.getPotionById(id);
        if (p != null) {
            return new PotionEffect(p, ticks, ampLevel, false, true);
        }
        return null;
    }

    public static void convertBuffMapToAttrs(final Map<Integer, Integer> buffMap, final Attrs attrs) {
        if (buffMap == null || attrs == null) return;
        attrs.resetAttrsPotion();
        final float[] potion = attrs.getAttrsPotion();
        buffMap.forEach((id, amp) -> {
            final int lv = MathHelper.clamp(amp, 0, 4) + 1;
            switch (id) {
                case 1: potion[7] += 0.08F * lv; break;
                case 2: potion[7] -= 0.15F * lv; potion[20] += 0.15F * lv; break;
                case 3: potion[6] += 0.6F * lv; break;
                case 4: potion[6] -= 0.6F * lv; break;
                case 5: potion[1] += 15.0F * lv; potion[2] += 15.0F * lv; potion[3] += 15.0F * lv; potion[4] += 15.0F * lv; potion[20] += 0.15F * lv; break;
                case 8: potion[8] += 2.0F * lv; break;
                case 13: potion[15] += 0.15F * lv; potion[14] += 20.0F * lv; break;
                case 15: potion[8] -= 24.0F; break;
                case 18: potion[1] -= 15.0F * lv; potion[2] -= 15.0F * lv; potion[3] -= 15.0F * lv; potion[4] -= 15.0F * lv; potion[20] -= 0.15F * lv; break;
                case 19: potion[5] -= 0.25F * lv; potion[20] -= 0.1F * lv; break;
                case 21: potion[0] += 150.0F * lv; potion[19] += 0.5F * lv; break;
                case 22: potion[0] += 100.0F * lv; potion[5] += 0.2F * lv; break;
                case 23: potion[17] += 0.5F * lv; potion[18] += 0.5F * lv; break;
                case 25: potion[15] += 0.1F * lv; potion[13] += 20.0F * lv; potion[20] -= 0.2F * lv; break;
                case 26: potion[9] += 0.2F * lv; potion[10] += 0.2F * lv; potion[11] += 0.2F * lv; break;
                case 27: potion[9] -= 0.3F * lv; potion[10] -= 0.3F * lv; potion[11] -= 0.3F * lv; break;
                default:
            }
        });
    }

    public static <T extends EntityLivingBase> void applyBuffOnTicks(final T host) {
        final Map<Integer, Integer> buffMap = ((IShipAttackBase) host).getBuffMap();
        float hp1p = Math.max(1.0F, host.getMaxHealth() * 0.01F);
        for (final Map.Entry<Integer, Integer> entry : buffMap.entrySet()) {
            final int id = entry.getKey();
            final int amp = entry.getValue();
            final int lv = MathHelper.clamp(amp, 0, 4) + 1;
            switch (id) {
                case 10:
                    if (host.getHealth() < host.getMaxHealth()) {
                        host.heal((hp1p + 4.0F) * (1.0F + lv * 0.5F));
                    }
                    break;
                case 20:
                    host.attackEntityFrom(DamageSource.MAGIC, (hp1p + 4.0F) * (1.0F + lv * 0.5F));
                    break;
                case 23:
                    if (host.getHealth() < host.getMaxHealth()) {
                        host.heal((hp1p + 2.0F) * (0.8F + lv * 0.2F));
                    }
                    if (host instanceof BasicEntityShip) {
                        ((BasicEntityShip) host).addMorale(ConfigHandler.buffSaturation * lv);
                    }
                    break;
                default:
            }
        }
    }

    public static void applyBuffOnTarget(final Entity target, final Map<Integer, int[]> effects) {
        if (target instanceof EntityLivingBase && effects != null && !effects.isEmpty()) {
            final EntityLivingBase ent = (EntityLivingBase) target;
            effects.forEach((id, content) -> {
                if (ent.getRNG().nextInt(100) < content[2]) {
                    final int duration = (id == 6 || id == 7) ? 5 : content[1];
                    final PotionEffect pe = BuffHelper.convertIdToPotion(id, content[0], duration);
                    if (pe != null) {
                        ent.addPotionEffect(pe);
                    }
                }
            });
        }
    }

    public static int getPotionLevel(final Entity host, final int pid) {
        if (host instanceof IShipAttackBase) {
            return BuffHelper.getPotionLevel(((IShipAttackBase) host).getBuffMap(), pid);
        }
        return 0;
    }

    public static int getPotionLevel(final Map<Integer, Integer> buffMap, final int pid) {
        if (buffMap != null) {
            return buffMap.getOrDefault(pid, -1) + 1;
        }
        return 0;
    }

    public static int getPotionLevel(final ItemStack stack, final int pid) {
        if (!stack.isEmpty()) {
            return BuffHelper.getPotionLevel(PotionUtils.getEffectsFromStack(stack), pid);
        }
        return 0;
    }

    public static int getPotionLevel(final List<PotionEffect> list, final int pid) {
        if (list != null && !list.isEmpty()) {
            for (final PotionEffect effect : list) {
                if (Potion.getIdFromPotion(effect.getPotion()) == pid) {
                    return effect.getAmplifier() + 1;
                }
            }
        }
        return 0;
    }

    public static <T extends EntityLivingBase> float getPotionDamage(final T host, final DamageSource source, final float atk) {
        if (host == null || source == null) return 0.0F;
        int level = 0;
        final Entity trueSource = source.getTrueSource();
        if (trueSource instanceof EntityPotion) {
            level = BuffHelper.getPotionLevel(((EntityPotion) trueSource).getPotion(), 7);
        } else if (trueSource instanceof EntityAreaEffectCloud) {
            final EntityAreaEffectCloud cloud = (EntityAreaEffectCloud) trueSource;
            final NBTTagCompound nbt = new NBTTagCompound();
            cloud.writeToNBT(nbt);
            level = BuffHelper.getPotionLevel(PotionUtils.getEffectsFromTag(nbt), 7);
        } else {
            return 0.0F;
        }
        if (level > 0) {
            final float hp1p = Math.max(1.0F, host.getMaxHealth() * 0.01F);
            return (hp1p * 2.0F + 2.0F) * level + atk;
        }
        return 0.0F;
    }

    public static <T extends EntityLivingBase> float applyBuffOnDamageByResist(final T host, final DamageSource source, float atk) {
        if (host == null || source == null) return atk;
        final Map<Integer, Integer> buffMap = ((IShipAttackBase) host).getBuffMap();
        int level;
        if (source.getTrueSource() instanceof EntityAbyssMissile) {
            level = BuffHelper.getPotionLevel(buffMap, 11);
            if (level > 0) atk *= 1.0F - Math.min(level, 4) * 0.2F;
        } else if (source.getTrueSource() instanceof IShipAttackBase) {
            level = BuffHelper.getPotionLevel(buffMap, 12);
            if (level > 0) atk *= 1.0F - Math.min(level, 4) * 0.2F;
        }
        return atk;
    }

    public static <T extends EntityLivingBase> float applyBuffOnDamageByLight(final T host, final DamageSource source, float atk) {
        if (host == null || source == null || !(source.getTrueSource() instanceof IShipAttackBase)) return atk;
        final IShipAttackBase attacker = (IShipAttackBase) source.getTrueSource();
        final BlockPos pos = new BlockPos(host);
        float lightCoeff = MathHelper.clamp((host.world.getLight(pos, true) - 2.0F) / 6.0F, 0.0F, 1.0F);
        final float level = BuffHelper.getPotionLevel(attacker.getBuffMap(), 16);
        if (level > 0.0F) {
            lightCoeff += 0.8F;
        }
        return CombatHelper.modDamageByLight(atk, attacker.getDamageType(), ((IShipAttackBase) host).getDamageType(), lightCoeff);
    }

    public static float applyBuffOnHeal(final EntityLivingBase host, final float heal) {
        if (host == null) return heal;
        final float hp1p = Math.max(1.0F, host.getMaxHealth() * 0.01F);
        final List<EntityAreaEffectCloud> clouds = host.world.getEntitiesWithinAABB(EntityAreaEffectCloud.class, host.getEntityBoundingBox().grow(3.0D));
        for (final EntityAreaEffectCloud cloud : clouds) {
            final NBTTagCompound nbt = new NBTTagCompound();
            cloud.writeToNBT(nbt);
            final int level = BuffHelper.checkPotionHeal(PotionUtils.getEffectsFromTag(nbt));
            if (level > 0) return heal + (hp1p + 3.0F) * (1.0F + level * 0.5F);
        }
        final List<EntityPotion> potions = host.world.getEntitiesWithinAABB(EntityPotion.class, host.getEntityBoundingBox().grow(3.0D));
        for (final EntityPotion potion : potions) {
            final ItemStack pstack = potion.getPotion();
            final int level = BuffHelper.checkPotionHeal(PotionUtils.getEffectsFromStack(pstack));
            if (level > 0) return heal + (hp1p + 6.0F) * (1.0F + level * 0.5F);
        }
        return heal;
    }

    public static int checkPotionHeal(final List<PotionEffect> list) {
        if (list != null) {
            for (final PotionEffect pe : list) {
                final int id = Potion.getIdFromPotion(pe.getPotion());
                if (id == 6 || id == 10) return pe.getAmplifier() + 1;
            }
        }
        return 0;
    }

    public static int checkPotionDamage(final List<PotionEffect> list) {
        if (list != null) {
            for (final PotionEffect pe : list) {
                final int id = Potion.getIdFromPotion(pe.getPotion());
                if (id == 7 || id == 19 || id == 20) return pe.getAmplifier() + 1;
            }
        }
        return 0;
    }

    public static void updateBuffMorale(final AttrsAdv attrs, final int moraleValue) {
        if (attrs == null) return;
        attrs.resetAttrsMorale();
        int id = -1;
        if (moraleValue > 5100) id = 0;
        else if (moraleValue > 3900) id = 1;
        else if (moraleValue <= 900) id = 3;
        else if (moraleValue <= 2100) id = 2;
        final float[] attrsMorale = Values.MoraleAttrs.get(id);
        if (attrsMorale != null) {
            attrs.setAttrsMorale(attrsMorale);
        }
    }

    public static void updateBuffFormation(final BasicEntityShip host) {
        if (host == null || host.world.isRemote) return;
        final CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(host.getPlayerUID());
        if (capa == null) return;
        final int[] teamSlot = capa.checkIsInFormation(host.getShipUID());
        final AttrsAdv attrs = (AttrsAdv) host.getAttrs();
        if (teamSlot[0] >= 0 && teamSlot[1] >= 0 && capa.getFormatID(teamSlot[0]) > 0) {
            host.setStateMinor(26, capa.getFormatID(teamSlot[0]));
            if (host.getStateMinor(26) == 3 && capa.getNumberOfShip(teamSlot[0]) == 5) {
                final int slotID = capa.getFormationPos(teamSlot[0], host.getShipUID());
                if (slotID >= 0) host.setStateMinor(27, slotID);
            } else {
                host.setStateMinor(27, teamSlot[1]);
            }
            attrs.setAttrsFormation(host.getStateMinor(26), host.getStateMinor(27));
            attrs.setMinMOV(capa.getMinMOVInTeam(teamSlot[0]));
        } else {
            host.setStateMinor(26, 0);
            host.setStateMinor(27, -1);
            attrs.resetAttrsFormation();
        }
    }

    public static float[] calcAttrsBuffed(final float[] raw, final float[] equip, final float[] morale, final float[] potion, final float[] formation) {
        final float[] buffed = new float[raw.length];
        final double[] scale = ConfigHandler.scaleShip;
        buffed[0] = raw[0] + equip[0] + (morale[0] + potion[0] + formation[0]) * (float) scale[0];
        buffed[8] = raw[8] + equip[8] + (morale[8] + potion[8] + formation[8]) * (float) scale[5];
        buffed[7] = raw[7] + equip[7] + (morale[7] + potion[7]) * (float) scale[4];
        buffed[5] = (raw[5] + equip[5] + (morale[5] + potion[5]) * (float) scale[2]) * formation[5];
        final int[] idsAdd = {15, 16, 17, 18, 19, 20};
        for (final int id : idsAdd) {
            buffed[id] = raw[id] + equip[id] + morale[id] + potion[id] + formation[id];
        }
        final int[] idsMulMoraleFormation = {9, 10, 11, 12, 13, 14};
        for (final int id : idsMulMoraleFormation) {
            buffed[id] = (raw[id] + equip[id] + potion[id]) * morale[id] * formation[id];
        }
        buffed[1] = (raw[1] + equip[1] + potion[1] * (float) scale[1]) * morale[1] * formation[1];
        buffed[2] = (raw[2] + equip[2] + potion[2] * 3.0F * (float) scale[1]) * morale[2] * formation[2];
        buffed[3] = (raw[3] + equip[3] + potion[3] * (float) scale[1]) * morale[3] * formation[3];
        buffed[4] = (raw[4] + equip[4] + potion[4] * 3.0F * (float) scale[1]) * morale[4] * formation[4];
        buffed[6] = (raw[6] + equip[6] + potion[6] * (float) scale[3]) * morale[6] * formation[6];
        return buffed;
    }

    public static float[] calcAttrsWithoutBuff(final AttrsAdv attrs, final int type) {
        float[] data = Attrs.getResetZeroValue();
        if (attrs != null) {
            switch (type) {
                case 0: data = BuffHelper.calcAttrsBuffed(attrs.getAttrsRaw(), attrs.getAttrsEquip(), attrs.getAttrsMorale(), attrs.getAttrsPotion(), attrs.getAttrsFormation()); break;
                case 1: data = BuffHelper.calcAttrsBuffed(attrs.getAttrsRaw(), Attrs.getResetZeroValue(), attrs.getAttrsMorale(), attrs.getAttrsPotion(), attrs.getAttrsFormation()); break;
                case 2: data = BuffHelper.calcAttrsBuffed(attrs.getAttrsRaw(), attrs.getAttrsEquip(), AttrsAdv.getResetMoraleValue(), attrs.getAttrsPotion(), attrs.getAttrsFormation()); break;
                case 3: data = BuffHelper.calcAttrsBuffed(attrs.getAttrsRaw(), attrs.getAttrsEquip(), attrs.getAttrsMorale(), Attrs.getResetZeroValue(), attrs.getAttrsFormation()); break;
                case 4: data = BuffHelper.calcAttrsBuffed(attrs.getAttrsRaw(), attrs.getAttrsEquip(), attrs.getAttrsMorale(), attrs.getAttrsPotion(), AttrsAdv.getResetFormationValue()); break;
                default:
            }
        }
        Attrs.checkAttrsLimit(data);
        return data;
    }

    public static void applyBuffOnAttrs(final BasicEntityShip host) {
        if (host == null || host.getAttrs() == null) return;
        final AttrsAdv attrs = (AttrsAdv) host.getAttrs();
        attrs.setAttrsBuffed(BuffHelper.calcAttrsBuffed(attrs.getAttrsRaw(), attrs.getAttrsEquip(), attrs.getAttrsMorale(), attrs.getAttrsPotion(), attrs.getAttrsFormation()));
        attrs.checkAttrsLimit();
    }

    public static void applyBuffOnAttrsHostile(final BasicEntityShipHostile host) {
        if (host == null || host.getAttrs() == null) return;
        final Attrs attrs = host.getAttrs();
        attrs.resetAttrsBuffed();
        final float[] buffed = attrs.getAttrsBuffed();
        final float[] raw = attrs.getAttrsRaw();
        final float[] potion = attrs.getAttrsPotion();
        for (int i = 0; i < 21; ++i) {
            buffed[i] = raw[i] + potion[i];
            if (buffed[i] < 0.0F) buffed[i] = 0.0F;
        }
        if (buffed[0] < 1.0F) buffed[0] = 1.0F;
        if (buffed[1] < 1.0F) buffed[1] = 1.0F;
        if (buffed[2] < 1.0F) buffed[2] = 1.0F;
        if (buffed[3] < 1.0F) buffed[3] = 1.0F;
        if (buffed[4] < 1.0F) buffed[4] = 1.0F;
        if (buffed[8] < 1.0F) buffed[8] = 1.0F;
        if (buffed[6] < 0.2F) buffed[6] = 0.2F;
    }

    public static void removeDebuffs(final EntityLivingBase host) {
        if (host == null) return;
        EntityLivingBase effectiveHost = host;
        if (host instanceof IShipMorph) {
            final EntityLivingBase morphHost = ((IShipMorph) host).getMorphHost();
            if (morphHost != null) effectiveHost = morphHost;
        }
        final List<Potion> toRemove = new ArrayList<>();
        for (final PotionEffect pe : effectiveHost.getActivePotionEffects()) {
            if (DEBUFF_POTION_IDS.contains(Potion.getIdFromPotion(pe.getPotion()))) {
                toRemove.add(pe.getPotion());
            }
        }
        if (toRemove.isEmpty()) return;
        final Map<Integer, Integer> buffs = (host instanceof IShipAttackBase) ? ((IShipAttackBase) host).getBuffMap() : null;
        for (final Potion potion : toRemove) {
            host.removePotionEffect(potion);
            if (effectiveHost != host) {
                effectiveHost.removePotionEffect(potion);
            }
            if (buffs != null) {
                buffs.remove(Potion.getIdFromPotion(potion));
            }
        }
    }

    public static void addEffectToAttackEffectMap(final BasicEntityShip ship, final Map<Integer, int[]> emap) {
        if (ship == null || emap == null || emap.isEmpty()) return;
        ship.getAttackEffectMap().putAll(emap);
    }

    public static void addEffectToAttackEffectMapFromStack(final BasicEntityShip ship, final ItemStack stack) {
        if (ship == null || stack.isEmpty() || !stack.hasTagCompound()) return;
        final Map<Integer, int[]> smap = ship.getAttackEffectMap();
        final NBTTagCompound nbt = stack.getTagCompound();
        final NBTTagList nbtlist = nbt.getTagList("PList", 10);
        for (int i = 0; i < nbtlist.tagCount(); ++i) {
            final NBTTagCompound nbtX = nbtlist.getCompoundTagAt(i);
            final int pid = nbtX.getInteger("PID");
            final int plv = nbtX.getInteger("PLV");
            final int ptime = nbtX.getInteger("PTick");
            final int pchance = nbtX.getInteger("PChance");
            smap.put(pid, new int[]{plv, ptime, pchance});
        }
    }
}