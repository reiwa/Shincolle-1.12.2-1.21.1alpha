package com.lulan.shincolle.utility;

import com.lulan.shincolle.entity.*;
import com.lulan.shincolle.entity.other.EntityProjectileStatic;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.unitclass.Attrs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

public class CombatHelper {
    private CombatHelper() {}

    public static final Random rand = new Random();

    public static float getFinalDamage(IShipAttrs host, DamageSource source, float atk) {
        Entity attacker = source.getTrueSource();
        float finalDmg = atk;
        boolean checkDEF = true;
        float potionDmg = BuffHelper.getPotionDamage((BasicEntityShip)host, source, atk);
        if (potionDmg > 0.0f) {
            finalDmg = potionDmg;
            checkDEF = false;
        }
        if (checkDEF) {
            finalDmg = applyDamageReduceByDEF(host.getAttrs(), finalDmg);
            if (attacker instanceof IShipOwner && ((IShipOwner) attacker).getPlayerUID() > 0 &&
                    (attacker instanceof BasicEntityShip || attacker instanceof BasicEntitySummon || attacker instanceof BasicEntityMount)) {
                finalDmg *= ConfigHandler.dmgSvS * 0.01f;
            }
        }
        finalDmg = BuffHelper.applyBuffOnDamageByResist((BasicEntityShip)host, source, finalDmg);
        finalDmg = BuffHelper.applyBuffOnDamageByLight((BasicEntityShip)host, source, finalDmg);
        if (finalDmg > 0.0f && finalDmg < 1.0f) {
            finalDmg = 1.0f;
        } else if (finalDmg < 0.0f) {
            finalDmg = 0.0f;
        }
        return finalDmg;
    }

    public static float applyDamageReduceByDEF(Attrs attrs, float rawAtk) {
        return rawAtk * (1.0f - attrs.getDefense() + (rand.nextFloat() * 0.5f - 0.25f));
    }

    public static float applyDamageReduceOnPlayer(Entity target, float rawAtk) {
        if (target instanceof EntityPlayer) {
            float newAtk = rawAtk * 0.25f;
            return Math.min(newAtk, 59.0f);
        }
        return rawAtk;
    }

    public static float applyCombatRateToDamage(IShipAttackBase host, boolean canMultiHit, float distance, float rawAtk) {
        if (host == null) return rawAtk;
        if ((host instanceof BasicEntitySummon || host instanceof BasicEntityMount) && host.getHostEntity() instanceof IShipAttackBase) {
            host = (IShipAttackBase) host.getHostEntity();
        }
        float miss = calcMissRate(host, distance);
        float cri = miss + host.getAttrs().getAttrsBuffed(9);
        float dhit = cri + host.getAttrs().getAttrsBuffed(10);
        float thit = dhit + host.getAttrs().getAttrsBuffed(11);
        float roll = host.getRand().nextFloat();
        if (roll <= miss) {
            if (host instanceof Entity) ParticleHelper.spawnAttackTextParticle((Entity) host, 0);
            return 0.0f;
        }
        if (roll <= cri) {
            if (host instanceof Entity) ParticleHelper.spawnAttackTextParticle((Entity) host, 1);
            return rawAtk * 1.5f;
        }
        if (canMultiHit && roll <= dhit) {
            if (host instanceof Entity) ParticleHelper.spawnAttackTextParticle((Entity) host, 2);
            return rawAtk * 2.0f;
        }
        if (canMultiHit && roll <= thit) {
            if (host instanceof Entity) ParticleHelper.spawnAttackTextParticle((Entity) host, 3);
            return rawAtk * 3.0f;
        }
        return rawAtk;
    }

    public static float calcMissRate(IShipAttackBase host, float distance) {
        float miss;
        float range = host.getAttrs().getAttackRange();
        float levelMod = 0.001f * host.getLevel();
        if (range <= 3.0f) {
            miss = 0.25f - levelMod;
        } else if (range <= 6.0f) {
            miss = 0.25f + 0.15f * (distance / range) - levelMod;
        } else {
            miss = 0.25f + 0.25f * (distance / range) - levelMod;
        }
        miss -= host.getAttrs().getAttrsBuffed(12);
        miss = Math.max(0.0f, Math.min(miss, 0.5f));
        if (BuffHelper.getPotionLevel(host.getBuffMap(), 9) > 0) {
            miss += 0.4f;
        }
        return miss;
    }

    public static boolean canDodge(IShipAttrs host, float distSq) {
        if (host == null || ((Entity) host).world.isRemote) {
            return false;
        }
        float dodge = host.getAttrs().getAttrsBuffed(15);
        if (host instanceof IShipInvisible) {
            if (distSq > 36.0f) {
                dodge += ((IShipInvisible) host).getInvisibleLevel();
                dodge = Math.min(dodge, (float) ConfigHandler.limitShipAttrs[15]);
            }
            if (distSq > 256.0f) {
                dodge += 0.5f;
            }
        }
        if (rand.nextFloat() <= dodge) {
            Entity ent = (Entity) host;
            if (host instanceof IShipMorph && ((IShipMorph) host).getMorphHost() != null) {
                ent = ((IShipMorph) host).getMorphHost();
            }
            ParticleHelper.spawnAttackTextParticle(ent, 4);
            return true;
        }
        return false;
    }

    public static float modDamageByLight(float dmg, int typeAtk, int typeDef, float lightCoef) {
        if (typeAtk <= 0 || typeDef <= 0) {
            return dmg;
        }
        lightCoef = Math.max(0.0f, Math.min(lightCoef, 1.0f));
        float modDay = Values.ModDmgNight[typeAtk - 1][typeDef - 1];
        float modNight = Values.ModDmgDay[typeAtk - 1][typeDef - 1];
        float mod = modNight + (modDay - modNight) * lightCoef;

        return dmg * mod;
    }

    public static float modDamageByAdditionAttrs(IShipAttrs host, Entity target, float dmg, int type) {
        float modEffect;
        switch (type) {
            case 2: modEffect = 4.0f; break;
            case 3: modEffect = 1.5f; break;
            default: modEffect = 1.0f;
        }
        int targetType = EntityHelper.checkEntityMovingType(target);
        if (targetType == 1) {
            return (dmg + host.getAttrs().getAttrsBuffed(13)) * modEffect;
        }
        if (targetType == 2) {
            return (dmg + host.getAttrs().getAttrsBuffed(14)) * modEffect;
        }
        return dmg * modEffect;
    }

    public static int getAttackDelay(float aspd, int type) {
        if (aspd < 0.01f) {
            aspd = 0.01f;
        }
        if (type >= 0 && type < ConfigHandler.baseAttackSpeed.length) {
            return (int) (ConfigHandler.baseAttackSpeed[type] / aspd) + ConfigHandler.fixedAttackDelay[type];
        }
        return 40;
    }

    public static int calcMissileMoveType(IShipAttackBase host, double tarY, int type) {
        int moveType = host.getMissileData(type).movetype;
        if (moveType < 0) {
            double depth = host.getShipDepth(0);
            if (depth > 2.0) {
                moveType = 0;
            } else if (depth > 0.0) {
                moveType = (tarY <= ((Entity) host).posY || tarY - ((Entity) host).posY < depth) ? 2 : 1;
            } else {
                moveType = 1;
            }
        }
        return moveType;
    }

    public static int calcMissileMoveTypeForAirplane(IShipAttackBase host, Entity target, int type) {
        int moveType = host.getMissileData(type).movetype;
        if (moveType < 0) {
            boolean targetInLiquid = EntityHelper.checkEntityIsInLiquid(target);
            boolean hostInLiquid = EntityHelper.checkEntityIsInLiquid((Entity) host);
            if (hostInLiquid) {
                moveType = 0;
            } else if (targetInLiquid) {
                BlockPos posUnderHost = new BlockPos(((Entity) host).posX, target.posY, ((Entity) host).posZ);
                boolean hostOverLiquid = BlockHelper.checkBlockIsLiquid(target.world.getBlockState(posUnderHost));
                moveType = hostOverLiquid && target.posY <= ((Entity) host).posY ? 2 : 0;
            } else {
                moveType = 0;
            }
        }
        return moveType;
    }

    public static void specialAttackEffect(IShipAttackBase host, int type, float[] data) {
        if (!(host instanceof Entity)) return;
        if(type == 5){
            Entity hostEntity = (Entity) host;
            EntityProjectileStatic beam = new EntityProjectileStatic(hostEntity.world);
            double[] beamData = new double[]{
                    data[0], data[1], data[2],
                    20.0 + host.getLevel() * 0.125,
                    0.12 + host.getLevel() * 7.5E-4,
                    4.0 + host.getLevel() * 0.035
            };
            beam.initAttrs(host, 0, beamData);
            hostEntity.world.spawnEntity(beam);
        }
    }
}
