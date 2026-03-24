package com.lulan.shincolle.utility;

import com.lulan.shincolle.client.particle.*;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipEmotion;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.proxy.CommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

public class ParticleHelper {
    private ParticleHelper() {}

    private static final Random rand = new Random();

    public static void spawnAttackTextParticle(Entity host, int type) {
        if (host == null || host.world == null || host.world.isRemote) {
            return;
        }
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(host.dimension, host.posX, host.posY, host.posZ, 64.0);
        switch (type) {
            case 0: {
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(host, 10, false), point);
                break;
            }
            case 1: {
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(host, 11, false), point);
                break;
            }
            case 2: {
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(host, 12, false), point);
                break;
            }
            case 3: {
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(host, 13, false), point);
                break;
            }
            case 4: {
                CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(host, 34, false), point);
                break;
            }
            default:
        }
    }

    @SideOnly(value=Side.CLIENT)
    public static void spawnAttackParticleCustomVector(Entity target, double posX, double posY, double posZ, double lookX, double lookY, double lookZ, byte type, boolean isShip) {
        if (target != null) {
            if (isShip && target instanceof IShipEmotion) {
                ((IShipEmotion)target).setAttackTick(50);
            }
            ParticleHelper.spawnAttackParticleAt(posX, posY, posZ, lookX, lookY, lookZ, type);
        }
    }

    @SideOnly(value=Side.CLIENT)
    public static void spawnAttackParticle(Entity target, byte type, boolean setAtkTime) {
        if (target == null) {
            return;
        }
        if (setAtkTime && target instanceof IShipEmotion) {
            ((IShipEmotion)target).setAttackTick(50);
        }
        if (type == 0) {
            return;
        }
        double lookX = 0.0;
        double lookY = 0.0;
        double lookZ = 0.0;
        if (type > 9) {
            lookY = target.height * 1.3;
        } else if (target.getLookVec() != null) {
            lookX = target.getLookVec().x;
            lookY = target.getLookVec().y;
            lookZ = target.getLookVec().z;
        }
        ParticleHelper.spawnAttackParticleAt(target.posX, target.posY, target.posZ, lookX, lookY, lookZ, type);
    }

    @SideOnly(value=Side.CLIENT)
    public static void spawnAttackParticleAt(double posX, double posY, double posZ, double lookX, double lookY, double lookZ, byte type) {
        World world = ClientProxy.getClientWorld();
        double ran1;
        double ran2;
        double ran3;
        float degYaw;
        switch (type) {
            case 1: {
                world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, posX, posY + 2.0, posZ, 0.0, 0.0, 0.0);
                break;
            }
            case 2: {
                world.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE, posX, posY + 1.0, posZ, 0.0, 0.0, 0.0);
                for (int i = 0; i < 24; ++i) {
                    ran1 = rand.nextFloat() * 6.0f - 3.0f;
                    ran2 = rand.nextFloat() * 6.0f - 3.0f;
                    world.spawnParticle(EnumParticleTypes.LAVA, posX + ran1, posY + 1.0, posZ + ran2, 0.0, 0.0, 0.0);
                }
                break;
            }
            case 3: {
                for (int i = 0; i < 7; ++i) {
                    double d0 = rand.nextGaussian() * 0.02;
                    double d1 = rand.nextGaussian() * 0.02;
                    double d2 = rand.nextGaussian() * 0.02;
                    world.spawnParticle(EnumParticleTypes.HEART, posX + rand.nextFloat() * 2.0 - 1.0, posY + 0.5 + rand.nextFloat() * 2.0, posZ + (rand.nextFloat() * 2.0f) - 1.0, d0, d1, d2);
                }
                break;
            }
            case 4: {
                for (int i = 0; i < 3; ++i) {
                    ran1 = rand.nextFloat() * lookX - lookX / 2.0;
                    ran2 = rand.nextFloat() * lookX - lookX / 2.0;
                    ran3 = rand.nextFloat() * lookX - lookX / 2.0;
                    world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX + ran1, posY + ran2, posZ + ran3, 0.0, lookY, 0.0);
                }
                break;
            }
            case 5: {
                for (int i = 0; i < 3; ++i) {
                    ran1 = rand.nextFloat() * lookX - lookX / 2.0;
                    ran2 = rand.nextFloat() * lookX - lookX / 2.0;
                    ran3 = rand.nextFloat() * lookX - lookX / 2.0;
                    world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX + ran1, posY + ran2, posZ + ran3, 0.0, lookY, 0.0);
                    world.spawnParticle(EnumParticleTypes.FLAME, posX + ran3, posY + ran2, posZ + ran1, 0.0, lookY, 0.0);
                }
                break;
            }
            case 6: {
                for (int i = 0; i < 24; ++i) {
                    ran1 = rand.nextFloat() - 0.5f;
                    ran2 = rand.nextFloat();
                    ran3 = rand.nextFloat();
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.5 + 0.05 * i, posY + 0.6 + ran1, posZ + lookZ - 0.5 + 0.05 * i, lookX * 0.3 * ran2, 0.05 * ran2, lookZ * 0.3 * ran2);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.5 + 0.05 * i, posY + 1.0 + ran1, posZ + lookZ - 0.5 + 0.05 * i, lookX * 0.3 * ran3, 0.05 * ran3, lookZ * 0.3 * ran3);
                }
                break;
            }
            case 7: {
                for (int i = 0; i < 4; ++i) {
                    ran1 = rand.nextFloat() * lookX - lookX / 2.0;
                    ran2 = rand.nextFloat() * lookX - lookX / 2.0;
                    ran3 = rand.nextFloat() * lookX - lookX / 2.0;
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + ran1, posY + ran2, posZ + ran3, 0.0, 0.0, 0.0);
                    world.spawnParticle(EnumParticleTypes.FLAME, posX + ran3, posY + ran2, posZ + ran1, 0.0, 0.05, 0.0);
                }
                break;
            }
            case 8: {
                world.spawnParticle(EnumParticleTypes.FLAME, posX, posY - 0.1, posZ, 0.0, 0.0, 0.0);
                world.spawnParticle(EnumParticleTypes.FLAME, posX, posY, posZ, 0.0, 0.0, 0.0);
                world.spawnParticle(EnumParticleTypes.FLAME, posX, posY + 0.1, posZ, 0.0, 0.0, 0.0);
                break;
            }
            case 9: {
                world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, posX, posY + 1.5, posZ, 0.0, 0.0, 0.0);
                for (int i = 0; i < 15; ++i) {
                    ran1 = rand.nextFloat() * 3.0f - 1.5f;
                    ran2 = rand.nextFloat() * 3.0f - 1.5f;
                    world.spawnParticle(EnumParticleTypes.LAVA, posX + ran1, posY + 1.0, posZ + ran2, 0.0, 0.0, 0.0);
                }
                break;
            }
            case 10: {
                ParticleTexts particleMiss = new ParticleTexts(world, posX, posY + lookY, posZ, 1.0f, 0);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleMiss);
                break;
            }
            case 11: {
                ParticleTexts particleCri = new ParticleTexts(world, posX, posY + lookY, posZ, 1.0f, 1);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleCri);
                break;
            }
            case 12: {
                ParticleTexts particleDHit = new ParticleTexts(world, posX, posY + lookY, posZ, 1.0f, 2);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleDHit);
                break;
            }
            case 13: {
                ParticleTexts particleTHit = new ParticleTexts(world, posX, posY + lookY, posZ, 1.0f, 3);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleTHit);
                break;
            }
            case 14: {
                ParticleLaser particleLaser = new ParticleLaser(world, posX, posY, posZ, lookX, lookY, lookZ, 1.0f, 0);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleLaser);
                break;
            }
            case 15: {
                ParticleSpray particleSpray = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 1);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray);
                break;
            }
            case 16: {
                ParticleSpray particleSpray2 = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 2);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray2);
                break;
            }
            case 17: {
                ParticleSpray particleSpray3 = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 3);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray3);
                break;
            }
            case 18: {
                ParticleSpray particleSpray4 = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 4);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray4);
                break;
            }
            case 19: {
                degYaw = CalcHelper.getLookDegree(lookX, 0.0, lookZ, false)[0];
                float[] newPos1 = CalcHelper.rotateXZByAxis(0.0f, (float)lookY, degYaw, 1.0f);
                float[] newPos2 = CalcHelper.rotateXZByAxis(0.0f, (float)(-lookY), degYaw, 1.0f);
                for (int i = 0; i < 15; ++i) {
                    ran1 = rand.nextFloat() - 0.5f;
                    ran2 = rand.nextFloat();
                    ran3 = rand.nextFloat();
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.5 + 0.05 * i + newPos1[1], posY + 0.6 + ran1, posZ + lookZ - 0.5 + 0.05 * i + newPos1[0], lookX * 0.3 * ran2, 0.05 * ran2, lookZ * 0.3 * ran2);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.5 + 0.05 * i + newPos2[1], posY + 0.6 + ran1, posZ + lookZ - 0.5 + 0.05 * i + newPos2[0], lookX * 0.3 * ran3, 0.05 * ran3, lookZ * 0.3 * ran3);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.5 + 0.05 * i + newPos1[1], posY + 0.9 + ran1, posZ + lookZ - 0.5 + 0.05 * i + newPos1[0], lookX * 0.3 * ran3, 0.05 * ran3, lookZ * 0.3 * ran3);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.5 + 0.05 * i + newPos2[1], posY + 0.9 + ran1, posZ + lookZ - 0.5 + 0.05 * i + newPos2[0], lookX * 0.3 * ran2, 0.05 * ran2, lookZ * 0.3 * ran2);
                }
                break;
            }
            case 20: {
                for (int i = 0; i < 3; ++i) {
                    world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX, posY + i * 0.1, posZ, lookX, lookY, lookZ);
                }
                break;
            }
            case 21: {
                ParticleLaser particleLaser2 = new ParticleLaser(world, posX, posY, posZ, lookX, lookY, lookZ, 4.0f, 1);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleLaser2);
                ParticleLaser particleLaser3 = new ParticleLaser(world, posX, posY + 0.4, posZ, lookX, lookY + 0.4, lookZ, 4.0f, 1);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleLaser3);
                ParticleLaser particleLaser4 = new ParticleLaser(world, posX, posY + 0.8, posZ, lookX, lookY + 0.8, lookZ, 4.0f, 1);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleLaser4);
                for (int i = 0; i < 20; ++i) {
                    float[] newPos1 = CalcHelper.rotateXZByAxis(1.0f, 0.0f, 0.314f * i, 1.0f);
                    ParticleSpray particleSpray5 = new ParticleSpray(world, lookX, lookY + 0.3, lookZ, newPos1[0] * 0.35, 0.0, newPos1[1] * 0.35, 0);
                    Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray5);
                }
                Particle91Type particle91Type = new Particle91Type(world, lookX, lookY + 3.0, lookZ, 0.6f);
                Minecraft.getMinecraft().effectRenderer.addEffect(particle91Type);
                break;
            }
            case 22: {
                for (int i = 0; i < 20; ++i) {
                    float[] newPos1 = CalcHelper.rotateXZByAxis((float)lookX, 0.0f, 0.314f * i, 1.0f);
                    ParticleSpray particleSpray7 = new ParticleSpray(world, posX + newPos1[0], posY + lookY, posZ + newPos1[1], (-newPos1[0]) * 0.06, 0.0, (-newPos1[1]) * 0.06, 5);
                    Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray7);
                }
                break;
            }
            case 23: {
                for (int i = 0; i < 20; ++i) {
                    float[] newPos1 = CalcHelper.rotateXZByAxis((float)lookX, 0.0f, 0.314f * i, 1.0f);
                    ParticleSpray particleSpray8 = new ParticleSpray(world, posX, posY + lookY, posZ, newPos1[0], 0.0, newPos1[1], 6);
                    Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray8);
                }
                break;
            }
            case 24: {
                for (int i = 0; i < 3; ++i) {
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX, posY + i * 0.3, posZ, lookX, lookY, lookZ);
                }
                break;
            }
            case 25: {
                ParticleTeam particleTeam = new ParticleTeam(world, (float)lookX, (int)lookY, posX, posY, posZ);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleTeam);
                break;
            }
            case 26: {
                ParticleSpray particleSpray7 = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 7);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray7);
                break;
            }
            case 27: {
                ParticleSpray particleSpray8 = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 8);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray8);
                break;
            }
            case 28: {
                ran1 = rand.nextFloat() * 0.7 - 0.35;
                ran2 = rand.nextFloat() * 0.7 - 0.35;
                world.spawnParticle(EnumParticleTypes.DRIP_WATER, posX + ran1, posY, posZ + ran2, lookX, lookY, lookZ);
                break;
            }
            case 29: {
                ParticleSpray particleSpray9 = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 9);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray9);
                break;
            }
            case 30: {
                for (int i = 0; i < 15; ++i) {
                    ran1 = rand.nextFloat() * 2.0f - 1.0f;
                    ran2 = rand.nextFloat() * 2.0f - 1.0f;
                    ran3 = rand.nextFloat() * 2.0f - 1.0f;
                    world.spawnParticle(EnumParticleTypes.SNOWBALL, posX + ran1, posY + 0.8 + ran2, posZ + ran3, lookX * 0.2, 0.5, lookZ * 0.2);
                }
                break;
            }
            case 31: {
                for (int i = 0; i < 22; ++i) {
                    ran1 = rand.nextFloat() - 0.5f;
                    ran2 = rand.nextFloat();
                    ran3 = rand.nextFloat();
                    world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, posX + lookX - 0.5 + 0.05 * i, posY + 0.7 + ran1, posZ + lookZ - 0.5 + 0.05 * i, lookX * 0.3 * ran2, 0.05 * ran2, lookZ * 0.3 * ran2);
                    world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, posX + lookX - 0.5 + 0.05 * i, posY + 0.9 + ran1, posZ + lookZ - 0.5 + 0.05 * i, lookX * 0.3 * ran3, 0.05 * ran3, lookZ * 0.3 * ran3);
                }
                break;
            }
            case 32: {
                ParticleSpray particleSpray10 = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 10);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray10);
                break;
            }
            case 33: {
                ParticleSpray particleSpray11 = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 11);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray11);
                break;
            }
            case 34: {
                ParticleTexts particleTDodge = new ParticleTexts(world, posX, posY + lookY, posZ, 1.0f, 4);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleTDodge);
                break;
            }
            case 35: {
                degYaw = CalcHelper.getLookDegree(lookX, 0.0, lookZ, false)[0];
                float[] newPos1 = CalcHelper.rotateXZByAxis(0.0f, (float)lookY, degYaw, 1.0f);
                float[] newPos2 = CalcHelper.rotateXZByAxis(0.0f, (float)(-lookY), degYaw, 1.0f);
                for (int i = 0; i < 15; ++i) {
                    ran1 = rand.nextFloat() - 0.5f;
                    ran2 = rand.nextFloat();
                    ran3 = rand.nextFloat();
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.6 + 0.1 * i + newPos1[1] + ran2, posY + ran1, posZ + lookZ - 0.6 + 0.1 * i + newPos1[0] + ran2, lookX * 0.3 * ran2, 0.05 * ran2, lookZ * 0.3 * ran2);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.6 + 0.1 * i + newPos2[1] + ran3, posY + ran1, posZ + lookZ - 0.6 + 0.1 * i + newPos2[0] + ran3, lookX * 0.3 * ran3, 0.05 * ran3, lookZ * 0.3 * ran3);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.6 + 0.1 * i + newPos1[1] + ran3, posY + 0.3 + ran1, posZ + lookZ - 0.6 + 0.1 * i + newPos1[0] + ran3, lookX * 0.3 * ran3, 0.05 * ran3, lookZ * 0.3 * ran3);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.6 + 0.1 * i + newPos2[1] + ran2, posY + 0.3 + ran1, posZ + lookZ - 0.6 + 0.1 * i + newPos2[0] + ran2, lookX * 0.3 * ran2, 0.05 * ran2, lookZ * 0.3 * ran2);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.6 + 0.1 * i + newPos1[1] + ran2, posY + 0.6 + ran1, posZ + lookZ - 0.6 + 0.1 * i + newPos1[0] + ran2, lookX * 0.3 * ran3, 0.05 * ran3, lookZ * 0.3 * ran3);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.6 + 0.1 * i + newPos2[1] + ran3, posY + 0.6 + ran1, posZ + lookZ - 0.6 + 0.1 * i + newPos2[0] + ran3, lookX * 0.3 * ran2, 0.05 * ran2, lookZ * 0.3 * ran2);
                }
                break;
            }
            case 36: {
                ParticleEmotion partEmo = new ParticleEmotion(world, null, posX, posY, posZ, (float)lookX, (int)lookY, (int)lookZ);
                Minecraft.getMinecraft().effectRenderer.addEffect(partEmo);
                break;
            }
            case 37: {
                ParticleSpray particleSpray12 = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 12);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray12);
                break;
            }
            case 38: {
                ParticleSpray particleSpray13 = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 13);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray13);
                break;
            }
            case 39: {
                ParticleSpray particleSpray14 = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 14);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray14);
                break;
            }
            case 40: {
                ParticleCraning particleCrane = new ParticleCraning(world, posX, posY, posZ, lookX, lookZ);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleCrane);
                break;
            }
            case 41: {
                ParticleSpray particleSpray15 = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 15);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray15);
                break;
            }
            case 42: {
                float[] newPos1 = CalcHelper.rotateXZByAxis(0.0f, (float)lookY, (float)(lookX * 0.01745329238474369), 1.0f);
                float[] newPos2 = CalcHelper.rotateXZByAxis(0.0f, (float)(-lookY), (float)(lookX * 0.01745329238474369), 1.0f);
                for (int i = 0; i < 15; ++i) {
                    ran1 = rand.nextFloat() - 0.5f;
                    ran2 = rand.nextFloat();
                    ran3 = rand.nextFloat();
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.5 + 0.05 * i + newPos1[1], posY + 0.6 + ran1, posZ + lookZ - 0.5 + 0.05 * i + newPos1[0], lookX * 0.3 * ran2, 0.05 * ran2, lookZ * 0.3 * ran2);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.5 + 0.05 * i + newPos2[1], posY + 0.6 + ran1, posZ + lookZ - 0.5 + 0.05 * i + newPos2[0], lookX * 0.3 * ran3, 0.05 * ran3, lookZ * 0.3 * ran3);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.5 + 0.05 * i + newPos1[1], posY + 0.9 + ran1, posZ + lookZ - 0.5 + 0.05 * i + newPos1[0], lookX * 0.3 * ran3, 0.05 * ran3, lookZ * 0.3 * ran3);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + lookX - 0.5 + 0.05 * i + newPos2[1], posY + 0.9 + ran1, posZ + lookZ - 0.5 + 0.05 * i + newPos2[0], lookX * 0.3 * ran2, 0.05 * ran2, lookZ * 0.3 * ran2);
                }
                break;
            }
            case 43: {
                for (int i = 0; i < 3; ++i) {
                    ParticleSmoke smoke1 = new ParticleSmoke(world, posX, posY + i * 0.1, posZ, 0.0, lookY, 0.0, (float)lookX);
                    Minecraft.getMinecraft().effectRenderer.addEffect(smoke1);
                }
                break;
            }
            case 44: {
                ParticleLine line1 = new ParticleLine(world, 0, new float[]{2.5f, 8.0f, 22.0f, 1.0f, 1.0f, 0.4f, 0.8f, (float)posX, (float)posY, (float)posZ, (float)lookX, (float)lookY, (float)lookZ});
                ParticleLine line2 = new ParticleLine(world, 0, new float[]{1.0f, 8.0f, 20.0f, 1.0f, 1.0f, 0.7f, 0.9f, (float)posX, (float)posY, (float)posZ, (float)lookX, (float)lookY, (float)lookZ});
                ParticleLine line3 = new ParticleLine(world, 0, new float[]{0.8f, 7.0f, 18.0f, 1.0f, 1.0f, 1.0f, 1.0f, (float)posX, (float)posY, (float)posZ, (float)lookX, (float)lookY, (float)lookZ});
                Minecraft.getMinecraft().effectRenderer.addEffect(line1);
                Minecraft.getMinecraft().effectRenderer.addEffect(line2);
                Minecraft.getMinecraft().effectRenderer.addEffect(line3);
                break;
            }
            case 45: {
                ParticleLine line1 = new ParticleLine(world, 0, new float[]{2.4f, 8.0f, 22.0f, 1.0f, 0.0f, 0.0f, 0.4f, (float)posX, (float)posY, (float)posZ, (float)lookX, (float)lookY, (float)lookZ});
                ParticleLine line2 = new ParticleLine(world, 0, new float[]{0.24f, 8.0f, 20.0f, 1.0f, 0.0f, 1.0f, 0.85f, (float)posX, (float)posY, (float)posZ, (float)lookX, (float)lookY, (float)lookZ});
                ParticleLine line3 = new ParticleLine(world, 0, new float[]{0.2f, 7.0f, 18.0f, 1.0f, 1.0f, 1.0f, 1.0f, (float)posX, (float)posY, (float)posZ, (float)lookX, (float)lookY, (float)lookZ});
                Minecraft.getMinecraft().effectRenderer.addEffect(line1);
                Minecraft.getMinecraft().effectRenderer.addEffect(line2);
                Minecraft.getMinecraft().effectRenderer.addEffect(line3);
                break;
            }
            case 46: {
                ParticleLine line1 = new ParticleLine(world, 0, new float[]{0.6f, 7.0f, 7.0f, 1.0f, 0.6f, 1.0f, 0.3f, (float)posX, (float)posY, (float)posZ, (float)lookX, (float)lookY, (float)lookZ});
                ParticleLine line2 = new ParticleLine(world, 0, new float[]{0.3f, 4.0f, 4.0f, 1.0f, 0.8f, 1.0f, 0.8f, (float)posX, (float)posY, (float)posZ, (float)lookX, (float)lookY, (float)lookZ});
                ParticleLine line3 = new ParticleLine(world, 0, new float[]{0.2f, 3.0f, 3.0f, 1.0f, 1.0f, 1.0f, 1.0f, (float)posX, (float)posY, (float)posZ, (float)lookX, (float)lookY, (float)lookZ});
                Minecraft.getMinecraft().effectRenderer.addEffect(line1);
                Minecraft.getMinecraft().effectRenderer.addEffect(line2);
                Minecraft.getMinecraft().effectRenderer.addEffect(line3);
                break;
            }
            case 47: {
                int maxpar = (int)((3 - ClientProxy.getMineraft().gameSettings.particleSetting) * 1.8f);
                for (int i = 0; i < maxpar; ++i) {
                    ParticleSpray spray = new ParticleSpray(world, posX, posY, posZ, lookX, lookY, lookZ, 16);
                    Minecraft.getMinecraft().effectRenderer.addEffect(spray);
                }
                break;
            }
            case 48: {
                for (int i = 0; i < 14; ++i) {
                    ran1 = (rand.nextFloat() - 0.5f) * lookY;
                    ran2 = (rand.nextFloat() - 0.5f) * lookX;
                    ran3 = (rand.nextFloat() - 0.5f) * lookZ;
                    world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, posX + ran2, posY + ran1, posZ + ran3, 0.0, 0.0, 0.0);
                    world.spawnParticle(EnumParticleTypes.WATER_WAKE, posX + ran2, posY + ran1, posZ + ran3, 0.0, 0.0, 0.0);
                }
                break;
            }
            case 49: {
                ran1 = rand.nextFloat() * 0.7 - 0.35;
                ran2 = rand.nextFloat() * 0.7 - 0.35;
                world.spawnParticle(EnumParticleTypes.DRIP_LAVA, posX + ran1, posY, posZ + ran2, lookX, lookY, lookZ);
                break;
            }
            case 50: {
                ParticleShine particleShine = new ParticleShine(world, posX, posY, posZ, lookX, lookY, lookZ);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleShine);
                break;
            }
            case 51: {
                ParticleFog particleFog = new ParticleFog(world, posX, posY, posZ, lookX, lookY, lookZ);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleFog);
                break;
            }
            default:
        }
    }

    @SideOnly(value=Side.CLIENT)
    public static void spawnAttackParticleAtEntity(Entity ent, double par1, double par2, double par3, byte type) {
        if (ent == null) {
            return;
        }
        WorldClient world = Minecraft.getMinecraft().world;
        EntityLivingBase host;
        double ran1;
        double ran2;
        double ran3;
        double ran4;
        float degYaw;
        switch (type) {
            case 1: {
                ParticleChi fxChi1 = new ParticleChi(world, ent, (float)par1, (int)par2);
                Minecraft.getMinecraft().effectRenderer.addEffect(fxChi1);
                break;
            }
            case 2: {
                ParticleTeam fxTeam = new ParticleTeam(world, ent, (float)par1, (int)par2);
                Minecraft.getMinecraft().effectRenderer.addEffect(fxTeam);
                break;
            }
            case 3: {
                ParticleLightning fxLightning = new ParticleLightning(world, ent, (float)par1, (int)par2);
                Minecraft.getMinecraft().effectRenderer.addEffect(fxLightning);
                break;
            }
            case 4: {
                ParticleStickyLightning light1 = new ParticleStickyLightning(world, ent, (float)par1, (int)par2, (int)par3);
                Minecraft.getMinecraft().effectRenderer.addEffect(light1);
                ParticleStickyLightning light2 = new ParticleStickyLightning(world, ent, (float)par1, (int)par2, (int)par3);
                Minecraft.getMinecraft().effectRenderer.addEffect(light2);
                ParticleStickyLightning light3 = new ParticleStickyLightning(world, ent, (float)par1, (int)par2, (int)par3);
                Minecraft.getMinecraft().effectRenderer.addEffect(light3);
                ParticleStickyLightning light4 = new ParticleStickyLightning(world, ent, (float)par1, (int)par2, (int)par3);
                Minecraft.getMinecraft().effectRenderer.addEffect(light4);
                break;
            }
            case 5: {
                degYaw = ((EntityLivingBase)ent).renderYawOffset % 360.0f * ((float)Math.PI / 180);
                float[] newPos1 = CalcHelper.rotateXZByAxis((float)par2, (float)par1, degYaw, 1.0f);
                float[] newPos2 = CalcHelper.rotateXZByAxis((float)par2, (float)(-par1), degYaw, 1.0f);
                float[] newPos3 = CalcHelper.rotateXZByAxis(0.25f, 0.0f, degYaw, 1.0f);
                for (int i = 0; i < 24; ++i) {
                    ran1 = (rand.nextFloat() - 0.5f) * 2.0f;
                    ran2 = (rand.nextFloat() - 0.5f) * 2.0f;
                    ran3 = (rand.nextFloat() - 0.5f) * 2.0f;
                    ran4 = rand.nextFloat() * 2.0f;
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, ent.posX + newPos1[1] + ran1, ent.posY + par3 + ran2, ent.posZ + newPos1[0] + ran3, newPos3[1] * ran4, 0.05 * ran4, newPos3[0] * ran4);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, ent.posX + newPos2[1] + ran1, ent.posY + par3 + ran3, ent.posZ + newPos2[0] + ran2, newPos3[1] * ran4, 0.05 * ran4, newPos3[0] * ran4);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, ent.posX + newPos1[1] + ran2, ent.posY + par3 + ran1, ent.posZ + newPos1[0] + ran3, newPos3[1] * ran4, 0.05 * ran4, newPos3[0] * ran4);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, ent.posX + newPos2[1] + ran2, ent.posY + par3 + ran3, ent.posZ + newPos2[0] + ran1, newPos3[1] * ran4, 0.05 * ran4, newPos3[0] * ran4);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, ent.posX + newPos1[1] + ran3, ent.posY + par3 + ran1, ent.posZ + newPos1[0] + ran2, newPos3[1] * ran4, 0.05 * ran4, newPos3[0] * ran4);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, ent.posX + newPos2[1] + ran3, ent.posY + par3 + ran2, ent.posZ + newPos2[0] + ran1, newPos3[1] * ran4, 0.05 * ran4, newPos3[0] * ran4);
                }
                break;
            }
            case 6: {
                int i;
                for (i = 0; i < 4; ++i) {
                    ParticleStickyLightning light11 = new ParticleStickyLightning(world, ent, (float)par1, (int)par2, 2);
                    Minecraft.getMinecraft().effectRenderer.addEffect(light11);
                }
                for (i = 0; i < 4; ++i) {
                    ParticleStickyLightning light21 = new ParticleStickyLightning(world, ent, (float)par1, (int)par2, 3);
                    Minecraft.getMinecraft().effectRenderer.addEffect(light21);
                }
                break;
            }
            case 7: {
                if (!(ent instanceof EntityLivingBase)) {
                    return;
                }
                host = (EntityLivingBase)ent;
                ParticleCube cube1 = new ParticleCube(world, host, par1, par2, par3, 1.5f, 0);
                Minecraft.getMinecraft().effectRenderer.addEffect(cube1);
                for (int i = 0; i < 6; ++i) {
                    ParticleStickyLightning light21 = new ParticleStickyLightning(world, ent, (float)par1, 40, 3);
                    Minecraft.getMinecraft().effectRenderer.addEffect(light21);
                }
                break;
            }
            case 8: {
                if (!(ent instanceof EntityLivingBase)) {
                    return;
                }
                host = (EntityLivingBase)ent;
                ParticleLaserNoTexture laser1 = new ParticleLaserNoTexture(world, host, par1, par2, par3, 0.1f, 3);
                Minecraft.getMinecraft().effectRenderer.addEffect(laser1);
                break;
            }
            case 9: {
                ParticleStickyLightning light5 = new ParticleStickyLightning(world, ent, (float)par1, (int)par2, (int)par3);
                Minecraft.getMinecraft().effectRenderer.addEffect(light5);
                ParticleStickyLightning light6 = new ParticleStickyLightning(world, ent, (float)par1, (int)par2, (int)par3);
                Minecraft.getMinecraft().effectRenderer.addEffect(light6);
                break;
            }
            case 10: {
                float[] newPos1 = CalcHelper.rotateXZByAxis((float)par3, (float)par1, ((EntityLivingBase)ent).renderYawOffset * ((float)Math.PI / 180), 1.0f);
                float[] newPos2 = CalcHelper.rotateXZByAxis((float)par3, (float)(-par1), ((EntityLivingBase)ent).renderYawOffset * ((float)Math.PI / 180), 1.0f);
                float[] newPos3 = CalcHelper.rotateXZByAxis(1.5f, 0.0f, ((EntityLivingBase)ent).rotationYawHead * ((float)Math.PI / 180), 1.0f);
                for (int i = 0; i < 24; ++i) {
                    ran1 = rand.nextFloat() - 0.5f;
                    ran2 = rand.nextFloat();
                    ran3 = rand.nextFloat();
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, ent.posX - 0.5 + 0.05 * i + newPos1[1] + newPos3[1], ent.posY + par2 + 0.6 + ran1, ent.posZ - 0.5 + 0.05 * i + newPos1[0] + newPos3[0], newPos3[1] * 0.5 * ran2, 0.05 * ran2, newPos3[0] * 0.5 * ran2);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, ent.posX - 0.5 + 0.05 * i + newPos2[1] + newPos3[1], ent.posY + par2 + 0.6 + ran1, ent.posZ - 0.5 + 0.05 * i + newPos2[0] + newPos3[0], newPos3[1] * 0.5 * ran3, 0.05 * ran3, newPos3[0] * 0.5 * ran3);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, ent.posX - 0.5 + 0.05 * i + newPos1[1] + newPos3[1], ent.posY + par2 + 0.9 + ran1, ent.posZ - 0.5 + 0.05 * i + newPos1[0] + newPos3[0], newPos3[1] * 0.5 * ran3, 0.05 * ran3, newPos3[0] * 0.5 * ran3);
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, ent.posX - 0.5 + 0.05 * i + newPos2[1] + newPos3[1], ent.posY + par2 + 0.9 + ran1, ent.posZ - 0.5 + 0.05 * i + newPos2[0] + newPos3[0], newPos3[1] * 0.5 * ran2, 0.05 * ran2, newPos3[0] * 0.5 * ran2);
                }
                break;
            }
            case 11: {
                ParticleSphereLight light1 = new ParticleSphereLight(ent, 0, ent.height * 1.2f, ent.height * 0.75f, 0.8f, 0.03f, (float)par1, (float)par2, (float)par3, 1.0f, ent.height * 0.4f);
                Minecraft.getMinecraft().effectRenderer.addEffect(light1);
                break;
            }
            case 12: {
                if (ent instanceof IShipEmotion) {
                    ((IShipEmotion)ent).setAttackTick(100);
                }
                ParticleGradient grad1 = new ParticleGradient(ent, 1, ent.height * 1.2f, 0.85f, 0.08f, 8.0f, (float)par1, (float)par2, (float)par3, 0.7f);
                Minecraft.getMinecraft().effectRenderer.addEffect(grad1);
                break;
            }
            case 13: {
                ParticleSparkle spark1 = new ParticleSparkle(ent, 8, 0.1f, ent.width * 2.0f, 0.0f, 0.0f, (float)par1, (float)par2, (float)par3, 1.0f, ent.height * 0.4f);
                Minecraft.getMinecraft().effectRenderer.addEffect(spark1);
                break;
            }
            case 14: {
                ParticleGradient grad1 = new ParticleGradient(ent, 2, ent.height * 1.5f, 0.7f, 0.4f, 0.0f, (float)par1, (float)par2, (float)par3, 0.8f, 40.0f, 1.6f);
                ParticleGradient grad2 = new ParticleGradient(ent, 2, ent.height * 1.3f, 0.7f, 0.3f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 40.0f, 1.5f);
                Minecraft.getMinecraft().effectRenderer.addEffect(grad1);
                Minecraft.getMinecraft().effectRenderer.addEffect(grad2);
                break;
            }
            case 15: {
                if (ent instanceof IShipEmotion) {
                    ((IShipEmotion)ent).setAttackTick(50);
                }
                ParticleSweep swp1 = new ParticleSweep(ent, 0, ent.height, ent.height * 5.6f, ent.height * 2.0f, 0.95f, 4.0f, (float)par1, (float)par2, (float)par3, 1.0f);
                Minecraft.getMinecraft().effectRenderer.addEffect(swp1);
                break;
            }
            case 16: {
                if (ent instanceof IShipEmotion) {
                    ((IShipEmotion)ent).setAttackTick(50);
                }
                ParticleSweep swp1 = new ParticleSweep(ent, 0, ent.height * 0.1f, ent.height * 5.6f, ent.height * 6.0f, 0.95f, 4.0f, (float)par1, (float)par2, (float)par3, 1.0f);
                Minecraft.getMinecraft().effectRenderer.addEffect(swp1);
                break;
            }
            case 17: {
                ParticleSparkle spark1 = new ParticleSparkle(ent, 1, (float)par1, (float)par2, (float)par3, 0.0f, 1.0f, 1.0f, 1.0f);
                Minecraft.getMinecraft().effectRenderer.addEffect(spark1);
                break;
            }
            case 18: {
                ParticleDebugPlane plane = new ParticleDebugPlane(ent, 0, (float)par1, (float)par2, (float)par3);
                Minecraft.getMinecraft().effectRenderer.addEffect(plane);
                break;
            }
            case 19: {
                if (!(ent instanceof BasicEntityShip)) break;
                ParticleDebugPlane plane = new ParticleDebugPlane(ent, 1, (float)par1, (float)par2, (float)par3);
                Minecraft.getMinecraft().effectRenderer.addEffect(plane);
                break;
            }
            case 20: {
                if (!(ent instanceof BasicEntityShip)) break;
                ParticleDebugPlane plane = new ParticleDebugPlane(ent, 2, (float)par1, (float)par2, (float)par3);
                Minecraft.getMinecraft().effectRenderer.addEffect(plane);
                break;
            }
            case 21: {
                ParticleSparkle spark1 = new ParticleSparkle(ent, 0, 0.025f, ent.width * 1.5f, 0.0f, 0.0f, (float)par1, (float)par2, (float)par3, 1.0f, ent.height * 0.4f);
                Minecraft.getMinecraft().effectRenderer.addEffect(spark1);
                break;
            }
            case 22: {
                ParticleSparkle spark1 = new ParticleSparkle(ent, 3, 0.05f, ent.width, 0.0f, 0.0f, (float)par1, (float)par2, (float)par3, 1.0f, ent.height * 0.4f);
                Minecraft.getMinecraft().effectRenderer.addEffect(spark1);
                break;
            }
            case 23: {
                ParticleSparkle spark1 = new ParticleSparkle(ent, 2, 0.075f, ent.width * 1.5f, 0.0f, 0.0f, (float)par1, (float)par2, (float)par3, 1.0f, ent.height * 0.4f);
                Minecraft.getMinecraft().effectRenderer.addEffect(spark1);
                break;
            }
            case 24: {
                ParticleSphereLight light1 = new ParticleSphereLight(ent, (int)par1, (float)par2, (float)par3);
                Minecraft.getMinecraft().effectRenderer.addEffect(light1);
                break;
            }
            case 36: {
                ParticleEmotion partEmo = new ParticleEmotion(world, ent, ent.posX, ent.posY, ent.posZ, (float)par1, (int)par2, (int)par3);
                Minecraft.getMinecraft().effectRenderer.addEffect(partEmo);
                break;
            }
            default:
        }
    }

    @SideOnly(value=Side.CLIENT)
    public static void spawnAttackParticleAtEntity(Entity ent, byte type, double[] parms) {
        if (ent == null) {
            return;
        }
        switch (type) {
            case 1: 
            case 2: {
                ParticleSpray particleSpray1 = new ParticleSpray(ent, type, parms);
                Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray1);
                break;
            }
            default:
        }
    }

    @SideOnly(value=Side.CLIENT)
    public static void spawnAttackParticleAtEntity(Entity host, Entity target, double par1, double par2, double par3, byte type, boolean setAtkTime) {
        if (host == null || target == null) {
            return;
        }
        WorldClient world = Minecraft.getMinecraft().world;
        EntityLivingBase host2 = null;
        if (setAtkTime && host instanceof IShipEmotion) {
            ((IShipEmotion)host).setAttackTick(50);
        }
        switch (type) {
            case 0: {
                if (!(host instanceof EntityLivingBase)) {
                    return;
                }
                host2 = (EntityLivingBase)host;
                ParticleLaserNoTexture laser1 = new ParticleLaserNoTexture(world, host2, target, 0.9f, par1, 0.0, 0.05f, 0);
                Minecraft.getMinecraft().effectRenderer.addEffect(laser1);
                ParticleLaserNoTexture laser2 = new ParticleLaserNoTexture(world, host2, target, -0.9f, par1, 0.0, 0.05f, 0);
                Minecraft.getMinecraft().effectRenderer.addEffect(laser2);
                break;
            }
            case 1: {
                if (!(host instanceof EntityLivingBase)) {
                    return;
                }
                host2 = (EntityLivingBase)host;
                ParticleCube cube1 = new ParticleCube(world, host2, par1, par2, par3, 2.5f, 1);
                Minecraft.getMinecraft().effectRenderer.addEffect(cube1);
                ParticleLaserNoTexture laser3 = new ParticleLaserNoTexture(world, host2, target, par1, par2, par3, 2.0f, 1);
                Minecraft.getMinecraft().effectRenderer.addEffect(laser3);
                break;
            }
            case 2: {
                if (!(host instanceof EntityLivingBase)) {
                    return;
                }
                host2 = (EntityLivingBase)host;
                ParticleCube cube2 = new ParticleCube(world, host2, par1, par2, par3, 5.0f, 1);
                Minecraft.getMinecraft().effectRenderer.addEffect(cube2);
                ParticleLaserNoTexture laser4 = new ParticleLaserNoTexture(world, host2, target, par1, par2, par3, 4.0f, 1);
                Minecraft.getMinecraft().effectRenderer.addEffect(laser4);
                break;
            }
            case 3: {
                if (!(host instanceof EntityLivingBase)) {
                    return;
                }
                host2 = (EntityLivingBase)host;
                ParticleLaserNoTexture laser5 = new ParticleLaserNoTexture(world, host2, target, 0.0, 0.0, 0.0, 0.1f, 2);
                Minecraft.getMinecraft().effectRenderer.addEffect(laser5);
                break;
            }
            case 4: {
                if (!(host instanceof EntityLivingBase)) {
                    return;
                }
                host2 = (EntityLivingBase)host;
                ParticleLaserNoTexture laser6 = new ParticleLaserNoTexture(world, host2, target, 0.0, 0.0, 0.0, 0.1f, 4);
                Minecraft.getMinecraft().effectRenderer.addEffect(laser6);
                break;
            }
            case 5: {
                if (!(host instanceof EntityLivingBase)) {
                    return;
                }
                host2 = (EntityLivingBase)host;
                ParticleLaserNoTexture laser = new ParticleLaserNoTexture(world, host2, target, 0.0, 0.0, 0.0, 0.1f, 5);
                Minecraft.getMinecraft().effectRenderer.addEffect(laser);
                break;
            }
            case 6: {
                if (!(host instanceof EntityLivingBase)) {
                    return;
                }
                host2 = (EntityLivingBase)host;
                ParticleLaserNoTexture laser = new ParticleLaserNoTexture(world, host2, target, par1, par2, par3, 0.0f, 6);
                Minecraft.getMinecraft().effectRenderer.addEffect(laser);
                break;
            }
            default:
        }
    }

    @SideOnly(value=Side.CLIENT)
    public static void spawnAttackParticleAt(String text, double posX, double posY, double posZ, byte type, int ... parms) {
        if (text == null || text.isEmpty()) {
            return;
        }
        World w = ClientProxy.getClientWorld();
        switch (type) {
            case 0: {
                ParticleTextsCustom ptx = new ParticleTextsCustom(null, w, posX, posY, posZ, 1.0f, 0, text, parms);
                Minecraft.getMinecraft().effectRenderer.addEffect(ptx);
                break;
            }
            case 1: {
                Entity host = EntityHelper.getEntityByID(parms[2], 0, true);
                if (host == null) {
                    return;
                }
                ParticleTextsCustom ptx = new ParticleTextsCustom(host, w, posX, posY, posZ, 1.0f, 1, text, parms);
                Minecraft.getMinecraft().effectRenderer.addEffect(ptx);
                break;
            }
            default:
        }
    }
}
