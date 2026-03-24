package com.lulan.shincolle.utility;

import com.google.common.base.Predicate;
import com.lulan.shincolle.entity.*;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.network.C2SGUIPackets;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.proxy.ServerProxy;
import net.minecraft.entity.*;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import org.lwjgl.input.Keyboard;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class TargetHelper {
    private TargetHelper() {}

    public static boolean checkUnattackTargetList(Entity target) {
        if (target == null) return false;
        HashMap<Integer, String> unatklist = ServerProxy.getUnattackableTargetClass();
        if (unatklist != null) {
            String tarClass = target.getClass().getSimpleName();
            return unatklist.containsKey(tarClass.hashCode());
        }
        return false;
    }

    public static boolean checkAttackTargetList(Entity host, Entity target) {
        if (target != null && host instanceof IShipAttackBase) {
            HashMap<Integer, String> tarList = ServerProxy.getPlayerTargetClass(((IShipAttackBase) host).getPlayerUID());
            if (tarList != null && tarList.containsKey((target.getClass().getSimpleName()).hashCode())) {
                if (target instanceof IEntityOwnable) {
                    return !TeamHelper.checkSameOwner(host, target);
                }
                return true;
            }
        }
        return false;
    }

    public static void updateTarget(IShipAttackBase host) {
        if (host.getEntityTarget() != null && (!host.getEntityTarget().isEntityAlive() || TeamHelper.checkSameOwner((Entity) host, host.getEntityTarget()))) {
            host.setEntityTarget(null);
        }
        if (host.getEntityRevengeTarget() != null && (!host.getEntityRevengeTarget().isEntityAlive() || host.getTickExisted() - host.getEntityRevengeTime() > 200)) {
            host.setEntityRevengeTarget(null);
        }
        if (host instanceof BasicEntityShipHostile) {
            BasicEntityShipHostile hostileShip = (BasicEntityShipHostile) host;
            EntityLivingBase attackTarget = hostileShip.getAttackTarget();
            if (attackTarget != null && (!attackTarget.isEntityAlive() || TeamHelper.checkSameOwner((Entity) host, attackTarget))) {
                hostileShip.setAttackTarget(null);
            }
        }
        if ((host.getTickExisted() & 63) == 0 && host.getEntityTarget() != null && host.getEntityTarget().isInvisible()) {
            if (host.getStateMinor(38) < 1 && host.getStateMinor(39) < 1) {
                host.setEntityTarget(null);
            }
        }
    }

    public static void setRevengeTargetAroundPlayer(EntityPlayer player, double dist, Entity target) {
        if (player != null && target != null) {
            List<BasicEntityShip> list = player.world.getEntitiesWithinAABB(BasicEntityShip.class, player.getEntityBoundingBox().grow(dist));
            for (BasicEntityShip ship : list) {
                if (ship.equals(target) || !TeamHelper.checkSameOwner(player, ship)) continue;
                if (!ship.getStateFlag(18) && (target instanceof EntityPlayer || target instanceof BasicEntityShip)) {
                    continue;
                }
                ship.setEntityRevengeTarget(target);
                ship.setEntityRevengeTime();
            }
        }
    }

    public static void setRevengeTargetAroundHostileShip(BasicEntityShipHostile host, double dist, Entity target) {
        if (host != null && target != null) {
            List<BasicEntityShipHostile> list = host.world.getEntitiesWithinAABB(BasicEntityShipHostile.class, host.getEntityBoundingBox().grow(dist));
            for (BasicEntityShipHostile ship : list) {
                ship.setEntityRevengeTarget(target);
                ship.setEntityRevengeTime();
            }
        }
    }

    public static boolean isEntityInvulnerable(Entity target) {
        if (target instanceof IProjectile || target instanceof EntityFireball || target instanceof EntityFireworkRocket ||
                target instanceof EntityFishHook || target instanceof EntityHanging || target instanceof EntityAreaEffectCloud) {
            return true;
        }
        return target.world != null && !target.world.isRemote && TargetHelper.checkUnattackTargetList(target);
    }

    public static void handleOPToolKeyInput() {
        if (ClientProxy.debugCooldown > 0) return;
        EntityPlayer player = ClientProxy.getClientPlayer();
        ItemStack optool = player.inventory.getCurrentItem();
        if (optool.isEmpty() || optool.getItem() != ModItems.OPTool) return;
        if (Keyboard.isKeyDown(Keyboard.KEY_NUMPAD7)) {
            ClientProxy.debugCooldown = 5;
            RayTraceResult hitObj = EntityHelper.getMouseOverEntity(ClientProxy.getMineraft().getRenderViewEntity(), 32.0, 1.0f, null, false, false);
            if (hitObj != null && hitObj.entityHit != null && !(hitObj.entityHit instanceof BasicEntityShip)) {
                String tarName = hitObj.entityHit.getClass().getSimpleName();
                LogHelper.debug("DEBUG: target wrench get class: " + tarName);
                CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, (byte) 50, tarName));
            }
        } else if (Keyboard.isKeyDown(Keyboard.KEY_NUMPAD8)) {
            ClientProxy.debugCooldown = 20;
            CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, (byte) 51));
        }
    }

    public static final class RevengeSelectorForHostile implements Predicate<Entity> {
        private final Entity host;

        public RevengeSelectorForHostile(Entity host) {
            this.host = host;
        }

        public boolean apply(Entity target) {
            if (target == null || this.host == null || this.host.equals(target) || !target.isEntityAlive() || target.isInvisible()) {
                return false;
            }
            if (target instanceof EntityPlayer && ((EntityPlayer) target).capabilities.disableDamage) {
                return false;
            }
            if (TargetHelper.isEntityInvulnerable(target) || target instanceof BasicEntityShipHostile) {
                return false;
            }
            if (target instanceof BasicEntityShip) {
                return true;
            }
            return !TeamHelper.checkSameOwner(this.host, target);
        }
    }

    public static final class SelectorForHostile implements Predicate<Entity> {
        private final Entity host;

        public SelectorForHostile(Entity host) {
            this.host = host;
        }

        public boolean apply(Entity target) {
            if (target == null || this.host == null || this.host.equals(target) || !target.isEntityAlive() || target.isInvisible()) {
                return false;
            }
            if (target instanceof EntityPlayer) {
                return !((EntityPlayer) target).capabilities.disableDamage && ConfigHandler.mobAttackPlayer;
            }
            if (TargetHelper.isEntityInvulnerable(target) || target instanceof BasicEntityShipHostile) {
                return false;
            }
            return target instanceof BasicEntityShip || target instanceof BasicEntityMount || !TeamHelper.checkSameOwner(this.host, target);
        }
    }

    public static class RevengeSelector implements Predicate<Entity> {
        protected final Entity host;

        public RevengeSelector(Entity host) {
            this.host = host;
        }

        public boolean apply(Entity target) {
            if (target == null || this.host == null || this.host.equals(target) || !target.isEntityAlive()) {
                return false;
            }
            if (target instanceof EntityPlayer && ((EntityPlayer) target).capabilities.disableDamage) {
                return false;
            }
            if (TargetHelper.isEntityInvulnerable(target)) {
                return false;
            }
            boolean isPVP = this.host instanceof BasicEntityShip && ((BasicEntityShip) this.host).getStateFlag(18);
            if (!isPVP && (target instanceof EntityPlayer || target instanceof BasicEntityShip)) {
                return false;
            }
            if (target.isInvisible()) {
                boolean canSeeInvisible = false;
                BasicEntityShip ship = null;
                if (this.host instanceof BasicEntityShip) ship = (BasicEntityShip) this.host;
                else if (this.host instanceof IShipOwner && ((IShipOwner) this.host).getHostEntity() instanceof BasicEntityShip) {
                    ship = (BasicEntityShip) ((IShipOwner) this.host).getHostEntity();
                }
                if (ship != null) canSeeInvisible = ship.getStateMinor(38) >= 1 || ship.getStateMinor(39) >= 1;
                if (!canSeeInvisible) return false;
            }
            return !TeamHelper.checkIsAlly(this.host, target);
        }
    }

    public static class Selector implements Predicate<Entity> {
        protected final Entity host;

        public Selector(Entity host) {
            this.host = host;
        }

        public boolean apply(Entity target) {
            if (target == null || this.host == null || this.host.equals(target) || !target.isEntityAlive()) {
                return false;
            }
            boolean isPVP = false, isAA = false, isASM = false;
            if (this.host instanceof BasicEntityShip) {
                isPVP = ((BasicEntityShip) this.host).getStateFlag(18);
                isAA = ((BasicEntityShip) this.host).getStateFlag(19);
                isASM = ((BasicEntityShip) this.host).getStateFlag(20);
            }
            if (target instanceof EntityPlayer) {
                if (((EntityPlayer) target).capabilities.disableDamage) return false;
                if (isPVP) {
                    switch (ConfigHandler.shipAttackPlayer) {
                        case 1: if (TeamHelper.checkIsBanned(this.host, target)) return true; break;
                        case 2: if (!TeamHelper.checkIsAlly(this.host, target)) return true; break;
                        case 3: if (!TeamHelper.checkSameOwner(this.host, target)) return true; break;
                        default:
                    }
                }
            }
            if (TargetHelper.isEntityInvulnerable(target)) return false;
            if (target.isInvisible()) {
                boolean canSeeInvisible = false;
                BasicEntityShip ship = null;
                if (this.host instanceof BasicEntityShip) ship = (BasicEntityShip) this.host;
                else if (this.host instanceof IShipOwner && ((IShipOwner) this.host).getHostEntity() instanceof BasicEntityShip) {
                    ship = (BasicEntityShip) ((IShipOwner) this.host).getHostEntity();
                }
                if (ship != null) canSeeInvisible = ship.getStateMinor(38) >= 1 || ship.getStateMinor(39) >= 1;
                if (!canSeeInvisible) return false;
            }
            if (this.host instanceof BasicEntityShip && ((BasicEntityShip) this.host).getStateFlag(12) && !((EntityLiving) this.host).canEntityBeSeen(target)) {
                return false;
            }
            if (target instanceof BasicEntityAirplane || target instanceof EntityAbyssMissile) {
                return isAA && TeamHelper.checkIsBanned(this.host, target);
            }
            if (target instanceof IShipInvisible) {
                return isASM && TeamHelper.checkIsBanned(this.host, target);
            }
            if (isPVP && (target instanceof BasicEntityShip || target instanceof BasicEntityMount)) {
                return TeamHelper.checkIsBanned(this.host, target);
            }
            if (target instanceof EntityMob || target instanceof EntitySlime) {
                return true;
            }
            return TargetHelper.checkAttackTargetList(this.host, target);
        }
    }

    public static final class Sorter implements Comparator<Entity> {
        private final Entity host;

        public Sorter(Entity entity) {
            this.host = entity;
        }

        @Override
        public int compare(Entity target1, Entity target2) {
            double dist1 = this.host.getDistanceSq(target1);
            double dist2 = this.host.getDistanceSq(target2);
            return Double.compare(dist1, dist2);
        }
    }
}