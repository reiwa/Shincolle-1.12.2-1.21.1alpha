package com.lulan.shincolle.utility;

import com.google.common.base.Predicate;
import com.lulan.shincolle.ai.path.ShipPath;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.ai.path.ShipPathPoint;
import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.crafting.ShipCalc;
import com.lulan.shincolle.entity.*;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.network.C2SGUIPackets;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.reference.Enums;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.server.CacheDataPlayer;
import com.lulan.shincolle.server.CacheDataShip;
import com.lulan.shincolle.tileentity.ITileWaypoint;
import com.lulan.shincolle.tileentity.TileEntityCrane;
import com.lulan.shincolle.tileentity.TileEntityWaypoint;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class EntityHelper {

    private EntityHelper() {}

    public static boolean checkEntityIsInLiquid(final Entity entity) {
        final BlockPos pos = new BlockPos(entity.posX, entity.getEntityBoundingBox().minY, entity.posZ);
        final IBlockState block = entity.world.getBlockState(pos);
        return BlockHelper.checkBlockIsLiquid(block);
    }

    public static boolean checkEntityIsFree(final Entity entity) {
        return BlockHelper.checkBlockSafe(entity.world, MathHelper.floor(entity.posX), (int)(entity.getEntityBoundingBox().minY + 0.5), MathHelper.floor(entity.posZ));
    }

    public static int checkEntityMovingType(final Entity entity) {
        if (entity instanceof IShipAttackBase) {
            switch (((IShipAttackBase) entity).getDamageType()) {
                case 7: return 1;
                case 6: return 2;
                default: return 0;
            }
        }
        if (entity instanceof EntityWaterMob || entity instanceof EntityGuardian) return 2;
        if (entity instanceof EntityBlaze || entity instanceof EntityWither || entity instanceof EntityDragon || entity instanceof EntityBat || entity instanceof EntityFlying) return 1;
        return 0;
    }

    public static void checkDepth(final IShipFloating host) {
        final Entity hostEntity = (Entity) host;
        final World world = hostEntity.world;
        final int px = MathHelper.floor(hostEntity.posX);
        int py = MathHelper.floor(hostEntity.getEntityBoundingBox().minY);
        final int pz = MathHelper.floor(hostEntity.posZ);
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(px, py, pz);
        final IBlockState state = world.getBlockState(pos);
        double depth;
        if (BlockHelper.checkBlockIsLiquid(state)) {
            depth = 1.0;
            for (int i = 1; py + i < 255D; i++) {
                pos.setY(py + i);
                final IBlockState nextState = world.getBlockState(pos);
                if (BlockHelper.checkBlockIsLiquid(nextState)) {
                    depth += 1.0;
                } else {
                    host.setStateFlag(0, nextState.getMaterial() == Material.AIR);
                    break;
                }
            }
            depth -= (hostEntity.posY - Math.floor(hostEntity.posY));
        } else {
            depth = 0.0;
            host.setStateFlag(0, false);
        }
        host.setShipDepth(depth);
    }

    public static boolean checkShipOutOfCombat(final BasicEntityShip ship) {
        return ship != null && ship.ticksExisted - ship.getCombatTick() > 128;
    }

    public static boolean checkShipColled(final int classID, final CapaTeitoku capa) {
        return capa != null && capa.getColleShipList() != null && capa.getColleShipList().contains(classID);
    }

    public static boolean checkOP(final EntityPlayer player) {
        if (player == null) return false;
        if (player.capabilities.isCreativeMode) return true;
        if (!player.world.isRemote) {
            return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().canSendCommands(player.getGameProfile());
        } else {
            return !CommonProxy.isMultiplayer || player.canUseCommandBlock();
        }
    }

    public static String getOwnerName(final BasicEntityShip ship) {
        if (ship == null) return "";
        if (ship.ownerName != null && !ship.ownerName.isEmpty()) return ship.ownerName;
        final EntityPlayer player = EntityHelper.getEntityPlayerByUID(ship.getPlayerUID());
        if (player != null) return player.getName();
        final EntityLivingBase owner = ship.getOwner();
        if (owner instanceof EntityPlayer) return owner.getName();
        return "";
    }

    public static Entity getEntityByID(final int entityID, final int worldID, final boolean isClient) {
        final World world = isClient ? ClientProxy.getClientWorld() : ServerProxy.getServerWorld(worldID);
        if (world != null && entityID > 0) return world.getEntityByID(entityID);
        return null;
    }

    public static BasicEntityShip getShipByUID(final int sid) {
        if (sid <= 0) return null;
        final CacheDataShip data = ServerProxy.getShipWorldData(sid);
        if (data != null) {
            final Entity getEnt = EntityHelper.getEntityByID(data.entityID, data.worldID, false);
            if (getEnt instanceof BasicEntityShip) return (BasicEntityShip) getEnt;
        }
        return null;
    }

    public static BasicEntityShip getShipFromHost(final Entity entity) {
        if (entity instanceof BasicEntityShip) return (BasicEntityShip) entity;
        if (entity instanceof BasicEntityMount) {
            final Entity host = ((BasicEntityMount) entity).getHostEntity();
            if (host instanceof BasicEntityShip) return (BasicEntityShip) host;
        }
        return null;
    }

    public static EntityPlayer getEntityPlayerByID(final int entityID, final int worldID, final boolean isClient) {
        final World world = isClient ? ClientProxy.getClientWorld() : ServerProxy.getServerWorld(worldID);
        if (world != null && entityID > 0) {
            for (final EntityPlayer p : world.playerEntities) {
                if (p != null && p.getEntityId() == entityID) return p;
            }
        }
        return null;
    }

    public static EntityPlayer getEntityPlayerByName(final String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            for (final WorldServer w : ServerProxy.getServerWorld()) {
                if (w != null) {
                    for (final EntityPlayer p : w.playerEntities) {
                        if (p != null && p.getName().equals(name)) return p;
                    }
                }
            }
        } catch (final Exception e) {
            LogHelper.info("EXCEPTION: get EntityPlayer by name fail: " + e);
            e.printStackTrace();
        }
        return null;
    }

    public static EntityPlayer getEntityPlayerByUIDAtClient(final int uid) {
        if (uid <= 0) return null;
        try {
            final World w = ClientProxy.getClientWorld();
            if (w == null) return null;
            for (final EntityPlayer p : w.playerEntities) {
                if (p != null) {
                    final CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(p);
                    if (capa != null && capa.getPlayerUID() == uid) return p;
                }
            }
        } catch (final Exception e) {
            LogHelper.info("EXCEPTION: get EntityPlayer by UID at client fail: " + e);
            e.printStackTrace();
        }
        return null;
    }

    public static EntityPlayer getEntityPlayerByUID(int uid) {
        if (uid <= 0) return null;
        final int peid = EntityHelper.getPlayerEID(uid);
        if (peid < 0) return null;
        try {
            for (WorldServer w : ServerProxy.getServerWorld()) {
                if (w != null) {
                    final Entity entity = w.getEntityByID(peid);
                    if (entity instanceof EntityPlayer) return (EntityPlayer) entity;
                }
            }
        } catch (final Exception e) {
            LogHelper.info("EXCEPTION: get EntityPlayer by UID fail: " + e);
            e.printStackTrace();
        }
        return null;
    }

    public static int getPlayerEID(final int uid) {
        if (uid > 0) {
            final CacheDataPlayer pdata = ServerProxy.getPlayerWorldData(uid);
            if (pdata != null) return pdata.entityID;
        }
        return -1;
    }

    public static int getPlayerUID(final Entity ent) {
        if (ent == null) return -1;
        if (ent instanceof EntityPlayer) {
            final CapaTeitoku capa = CapaTeitoku.getTeitokuCapability((EntityPlayer) ent);
            return (capa != null) ? capa.getPlayerUID() : -1;
        }
        if (ent instanceof IShipOwner) return ((IShipOwner) ent).getPlayerUID();
        if (ent instanceof IEntityOwnable) {
            final Entity owner = ((IEntityOwnable) ent).getOwner();
            if (owner instanceof EntityPlayer) {
                final CapaTeitoku capa = CapaTeitoku.getTeitokuCapability((EntityPlayer) owner);
                return (capa != null) ? capa.getPlayerUID() : -1;
            }
        }
        return -1;
    }

    public static List<EntityPlayer> getEntityPlayerUsingGUI() {
        final List<EntityPlayer> plist = new ArrayList<>();
        for (final WorldServer w : ServerProxy.getServerWorld()) {
            if (w == null) continue;
            for (final EntityPlayer p : w.playerEntities) {
                final CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(p);
                if (capa != null && capa.isGuiOpening()) {
                    plist.add(p);
                }
            }
        }
        return plist;
    }

    public static String getPetPlayerUUID(final EntityTameable pet) {
        if (pet == null) return null;
        final UUID ownerId = pet.getOwnerId();
        return (ownerId != null) ? ownerId.toString() : "00000000-0000-0000-0000-000000000000";
    }

    public static void setPetPlayerUID(final EntityPlayer player, final IShipOwner pet) {
        EntityHelper.setPetPlayerUID(EntityHelper.getPlayerUID(player), pet);
    }

    public static void setPetPlayerUID(final int pid, final IShipOwner pet) {
        if (pet != null && pid > 0) pet.setPlayerUID(pid);
    }

    public static void setPetPlayerUUID(final int pid, final EntityTameable pet) {
        if (pet != null) {
            final EntityPlayer owner = EntityHelper.getEntityPlayerByUID(pid);
            EntityHelper.setPetPlayerUUID(owner, pet);
        }
    }

    public static void setPetPlayerUUID(final EntityPlayer player, final EntityTameable pet) {
        if (player != null) EntityHelper.setPetPlayerUUID(player.getUniqueID(), pet);
    }

    public static void setPetPlayerUUID(final UUID uuid, final EntityTameable pet) {
        if (pet != null) pet.setOwnerId(uuid);
    }

    public static void addPlayerColledShip(final int classID, final EntityPlayer player) {
        final CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
        if (capa != null) capa.setColleShip(classID);
    }

    public static void updateShipNavigator(final IShipAttackBase entity) {
        if (entity == null) return;
        final EntityLiving entity2 = (EntityLiving) entity;
        final ShipPathNavigate pathNavi = entity.getShipNavigate();
        if (pathNavi != null && !pathNavi.noPath()) {
            if (!entity2.getNavigator().noPath()) entity2.getNavigator().clearPath();
            if (entity.getIsSitting() || entity.getIsLeashed()) {
                entity.getShipNavigate().clearPathEntity();
                return;
            }
            if (ConfigHandler.debugMode && entity2.ticksExisted % 16 == 0) {
                EntityHelper.sendPathParticlePacket(pathNavi.getPath(), new NetworkRegistry.TargetPoint(entity2.dimension, entity2.posX, entity2.posY, entity2.posZ, 48.0));
            }
            pathNavi.onUpdateNavigation();
            entity.getShipMoveHelper().onUpdateMoveHelper();
            EntityHelper.travel(entity2, entity2.moveStrafing, entity2.moveForward);
        }
        if (!entity2.getNavigator().noPath()) {
            if (EntityHelper.checkEntityIsInLiquid(entity2)) entity2.getNavigator().clearPath();
            if (ConfigHandler.debugMode && entity2.ticksExisted % 16 == 0) {
                EntityHelper.sendPathParticlePacket(entity2.getNavigator().getPath(), new NetworkRegistry.TargetPoint(entity2.dimension, entity2.posX, entity2.posY, entity2.posZ, 64.0));
            }
        }
    }

    private static <T> void sendPathParticlePacket(final T path, final NetworkRegistry.TargetPoint target) {
        final int parType;
        final int len;
        final int[] points;
        if (path instanceof ShipPath) {
            final ShipPath p = (ShipPath) path;
            parType = 0;
            len = p.getCurrentPathLength();
            points = new int[len * 3 + 1];
            points[0] = p.getCurrentPathIndex();
            for (int i = 0; i < len; ++i) {
                final ShipPathPoint temp = p.getPathPointFromIndex(i);
                points[i * 3 + 1] = temp.xCoord;
                points[i * 3 + 2] = temp.yCoord;
                points[i * 3 + 3] = temp.zCoord;
            }
        } else if (path instanceof Path) {
            final Path p = (Path) path;
            parType = 1;
            len = p.getCurrentPathLength();
            points = new int[len * 3 + 1];
            points[0] = p.getCurrentPathIndex();
            for (int i = 0; i < len; ++i) {
                final PathPoint temp = p.getPathPointFromIndex(i);
                points[i * 3 + 1] = temp.x;
                points[i * 3 + 2] = temp.y;
                points[i * 3 + 3] = temp.z;
            }
        } else {
            return;
        }
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(parType, points), target);
    }

    @SideOnly(value = Side.CLIENT)
    public static RayTraceResult getPlayerMouseOverEntity(final double dist, final float partialTicks, final List<Entity> exlist, final boolean ignoreInvulnerable, final boolean ignoreInvisible) {
        return EntityHelper.getMouseOverEntity(ClientProxy.getMineraft().getRenderViewEntity(), dist, partialTicks, exlist, ignoreInvulnerable, ignoreInvisible);
    }

    @SideOnly(value = Side.CLIENT)
    public static RayTraceResult getPlayerMouseOverEntity(final double dist, final float partialTicks, final List<Entity> exlist) {
        return EntityHelper.getMouseOverEntity(ClientProxy.getMineraft().getRenderViewEntity(), dist, partialTicks, exlist, true, true);
    }

    @SideOnly(value = Side.CLIENT)
    public static RayTraceResult getPlayerMouseOverEntity(final double dist, final float partialTicks) {
        return EntityHelper.getMouseOverEntity(ClientProxy.getMineraft().getRenderViewEntity(), dist, partialTicks, null, true, true);
    }

    @SideOnly(value = Side.CLIENT)
    public static RayTraceResult getMouseOverEntity(Entity viewer, final double dist, final float partialTicks, @Nullable final List<Entity> exlist, final boolean ignoreInvulnerable, final boolean ignoreInvisible) {
        if (viewer == null || viewer.world == null) return null;
        if (viewer.getRidingEntity() instanceof BasicEntityMount && !ClientProxy.isViewPlayer) {
            final Entity ship = ((BasicEntityMount) viewer.getRidingEntity()).getHostEntity();
            if (ship != null) viewer = ship;
        }
        RayTraceResult lookBlock = BlockHelper.getMouseOverBlock(viewer, dist, partialTicks, false, true, true);
        final Vec3d eyePos = viewer.getPositionEyes(partialTicks);
        if (eyePos == null) return null;
        double maxDist = dist;
        if (lookBlock != null) maxDist = lookBlock.hitVec.distanceTo(eyePos);
        final Vec3d lookVec = viewer.getLook(partialTicks);
        final Vec3d endPos = eyePos.addVector(lookVec.x * dist, lookVec.y * dist, lookVec.z * dist);
        Entity pointedEntity = null;
        final AxisAlignedBB rayBox = viewer.getEntityBoundingBox().expand(lookVec.x * dist, lookVec.y * dist, lookVec.z * dist).grow(1.0D);
        final List<Entity> list = viewer.world.getEntitiesWithinAABBExcludingEntity(viewer, rayBox);
        double closestDistSq = maxDist * maxDist;
        for (final Entity entity : list) {
            if (exlist != null && exlist.contains(entity)) continue;
            if ((ignoreInvulnerable && TargetHelper.isEntityInvulnerable(entity)) || (ignoreInvisible && entity.isInvisible()) || !entity.canBeCollidedWith()) continue;
            final double borderSize = entity.getCollisionBorderSize();
            final AxisAlignedBB targetBox = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
            final RayTraceResult intercept = targetBox.calculateIntercept(eyePos, endPos);
            if (targetBox.contains(eyePos)) {
                if (closestDistSq >= 0.0D) {
                    pointedEntity = entity;
                    closestDistSq = 0.0D;
                }
                continue;
            }
            if (intercept == null) continue;
            final double hitDistSq = eyePos.squareDistanceTo(intercept.hitVec);
            if (hitDistSq < closestDistSq) {
                if (entity.getLowestRidingEntity() == viewer.getLowestRidingEntity() && !entity.canRiderInteract()) {
                    if (closestDistSq == 0.0D) pointedEntity = entity;
                } else {
                    pointedEntity = entity;
                    closestDistSq = hitDistSq;
                }
            }
        }
        if (pointedEntity != null && (closestDistSq < maxDist * maxDist || lookBlock == null)) {
            return new RayTraceResult(pointedEntity);
        }
        return lookBlock;
    }

    public static RayTraceResult getMouseoverTarget(final World world, final EntityPlayer player, final double dist, final boolean onLiquid, final boolean ignoreNoAABB, final boolean alwaysLastHit) {
        final float partialTicks = 1.0F;
        final float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        final float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
        final double playerX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        final double playerY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks + (world.isRemote ? player.getEyeHeight() - player.getDefaultEyeHeight() : player.getEyeHeight());
        final double playerZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
        final float f3 = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        final float f4 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        final float f5 = -MathHelper.cos(-pitch * 0.017453292F);
        final float f6 = MathHelper.sin(-pitch * 0.017453292F);
        final float f7 = f4 * f5;
        final float f8 = f3 * f5;
        final Vec3d vec3 = new Vec3d(playerX, playerY, playerZ);
        final Vec3d vec31 = vec3.addVector(f7 * dist, f6 * dist, f8 * dist);
        return world.rayTraceBlocks(vec3, vec31, onLiquid, ignoreNoAABB, alwaysLastHit);
    }

    public static void applyShipEmotesAOEHostile(final World world, final double x, final double y, final double z, final double range, final int emotesType) {
        if (world.isRemote) return;
        final AxisAlignedBB aabb = new AxisAlignedBB(x - range, y - range, z - range, x + range, y + range, z + range);
        final List<BasicEntityShipHostile> slist = world.getEntitiesWithinAABB(BasicEntityShipHostile.class, aabb);
        if (slist == null) return;
        for (final BasicEntityShipHostile s : slist) {
            if (s.isEntityAlive()) s.applyEmotesReaction(emotesType);
        }
    }

    public static void applyShipEmotesAOE(final World world, final double x, final double y, final double z, final double range, final int emotesType) {
        if (world.isRemote) return;
        final AxisAlignedBB aabb = new AxisAlignedBB(x - range, y - range, z - range, x + range, y + range, z + range);
        final List<BasicEntityShip> slist = world.getEntitiesWithinAABB(BasicEntityShip.class, aabb);
        if (slist == null) return;
        for (final BasicEntityShip s : slist) {
            if (s.isEntityAlive()) s.applyEmotesReaction(emotesType);
        }
    }

    public static void applyEmotesAOE(final List entlist, final int emotes) {
        if (entlist == null || entlist.isEmpty()) return;
        final BasicEntityShip firstEntity = (BasicEntityShip) entlist.get(0);
        final NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(firstEntity.dimension, firstEntity.posX, firstEntity.posY, firstEntity.posZ, 48.0);
        for (Object s : entlist) {
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle((BasicEntityShip)s, 36, ((BasicEntityShip)s).height * 0.6F, 0.0F, emotes), point);
        }
    }

    public static boolean updateWaypointMove(final IShipGuardian entity) {
        if (entity.getStateFlag(11) || entity.getGuardedPos(1) <= 0 || entity.getIsSitting() || entity.getIsLeashed() || entity.getIsRiding()) {
            return false;
        }
        final Entity host = (Entity) entity;
        final double distSq = host.getDistanceSq(entity.getGuardedPos(0) + 0.5D, entity.getGuardedPos(1), entity.getGuardedPos(2) + 0.5D);
        final BlockPos pos = new BlockPos(entity.getGuardedPos(0), entity.getGuardedPos(1), entity.getGuardedPos(2));
        final TileEntity tile = host.world.getTileEntity(pos);
        if (tile instanceof TileEntityCrane) {
            if (distSq < 25.0) {
                if (entity.getStateMinor(43) == 0) {
                    entity.setStateMinor(43, 1);
                    if (!host.getPassengers().isEmpty()) {
                        final Entity rider = host.getPassengers().get(0);
                        if (rider instanceof BasicEntityShip) {
                            if (entity instanceof BasicEntityMount) ((BasicEntityShip) rider).setStateMinor(43, 1);
                            rider.dismountRidingEntity();
                        }
                    }
                }
            } else if (entity.getStateMinor(6) > 0) {
                entity.getShipNavigate().tryMoveToXYZ(pos.getX() + 0.5, pos.getY() - 2.0, pos.getZ() + 0.5, 1.0);
            }
        } else {
            entity.setStateMinor(43, 0);
            if (!host.getPassengers().isEmpty() && host.getPassengers().get(0) instanceof BasicEntityShip) {
                ((BasicEntityShip) host.getPassengers().get(0)).setStateMinor(43, 0);
            }
        }
        if (tile instanceof TileEntityWaypoint) {
            if (entity.getStateMinor(26) > 0 && entity.getStateMinor(27) > 0) return false;
            if (distSq < 9.0) {
                try {
                    final boolean updatePos = EntityHelper.applyNextWaypoint((TileEntityWaypoint) tile, entity, true, 16);
                    if (updatePos && entity.getStateMinor(6) > 0) {
                        entity.setStateMinor(10, 2);
                        entity.getShipNavigate().tryMoveToXYZ(entity.getGuardedPos(0) + 0.5, entity.getGuardedPos(1), entity.getGuardedPos(2) + 0.5, 1.0);
                        if (entity.getStateMinor(26) > 0 && entity.getStateMinor(27) == 0) {
                            BasicEntityShip shipToMove = (entity instanceof BasicEntityMount) ? (BasicEntityShip) entity.getHostEntity() : (BasicEntityShip) entity;
                            if (shipToMove != null) {
                                EntityHelper.applyMovingToShipTeam(shipToMove, shipToMove.getGuardedPos(0) + 0.5, shipToMove.getGuardedPos(1), shipToMove.getGuardedPos(2) + 0.5);
                            }
                        }
                    }
                    return updatePos;
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            } else if ((entity.getTickExisted() & 0x7F) == 0 && entity.getStateMinor(6) > 0) {
                entity.getShipNavigate().tryMoveToXYZ(entity.getGuardedPos(0) + 0.5, entity.getGuardedPos(1), entity.getGuardedPos(2) + 0.5, 1.0);
            }
        }
        return false;
    }

    public static void applyMovingToShipTeam(final BasicEntityShip ship, final double gx, final double gy, final double gz) {
        final CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(ship.getPlayerUID());
        if (capa == null) return;
        final int[] teamslot = capa.checkIsInFormation(ship.getShipUID());
        if (teamslot == null || teamslot[0] < 0 || teamslot[1] != 0) return;
        final List<BasicEntityShip> ships = capa.getShipEntityAllList(teamslot[0]);
        if (ships == null || ships.isEmpty()) return;
        FormationHelper.applyFormationMoving(ships, ship.getStateMinor(26), MathHelper.floor(gx), (int) gy, MathHelper.floor(gz), false);
        for (final BasicEntityShip s : ships) {
            if (s != null && s.isEntityAlive() && s.getStateMinor(6) > 0) {
                s.getShipNavigate().tryMoveToXYZ(s.getGuardedPos(0) + 0.5, s.getGuardedPos(1), s.getGuardedPos(2) + 0.5, 1.0);
            }
        }
    }

    public static boolean applyNextWaypoint(final ITileWaypoint tile, final IShipGuardian entity, final boolean checkWpStay, final int checkDelay) {
        boolean changed = false;
        boolean timeout = !checkWpStay;
        if (checkWpStay) {
            final int wpstay = entity.getWpStayTime();
            final int staytimemax = Math.max(entity.getWpStayTimeMax(), tile.getWpStayTime());
            if (wpstay < staytimemax) {
                entity.setWpStayTime(wpstay + checkDelay);
            } else {
                timeout = true;
            }
        }
        if (timeout) {
            entity.setWpStayTime(0);
            final BlockPos next = tile.getNextWaypoint();
            final BlockPos last = tile.getLastWaypoint();
            final BlockPos shiplast = entity.getLastWaypoint();
            BlockPos targetPos = null;
            if (next.getY() > 0 && next.equals(shiplast)) {
                if (last.getY() > 0) targetPos = last;
                else if (next.getY() > 0) targetPos = next;
            } else if (next.getY() > 0) {
                targetPos = next;
            }
            if (targetPos != null) {
                EntityHelper.setGuardedPos((Entity) entity, targetPos.getX(), targetPos.getY(), targetPos.getZ());
                changed = true;
            }
            final BlockPos tilePos = ((TileEntity) tile).getPos();
            entity.setLastWaypoint(tilePos);
            final Entity host = (Entity) entity;
            if (!host.getPassengers().isEmpty() && host.getPassengers().get(0) instanceof BasicEntityShip) {
                ((BasicEntityShip) host.getPassengers().get(0)).setLastWaypoint(tilePos);
            }
        }
        return changed;
    }

    private static void setGuardedPos(final Entity host, final int x, final int y, final int z) {
        ((IShipGuardian) host).setGuardedPos(x, y, z, host.dimension, 1);
        if (!host.getPassengers().isEmpty() && host.getPassengers().get(0) instanceof IShipGuardian) {
            ((IShipGuardian) host.getPassengers().get(0)).setGuardedPos(x, y, z, host.dimension, 1);
        }
    }

    public static int getEntityNumber(final int type, final World world) {
        int count = 0;
        if (world != null) {
            for (final Entity ent : world.loadedEntityList) {
                if (type == 1) {
                    if (!ent.isNonBoss()) count++;
                } else if (type == 2) {
                    if (ent instanceof BasicEntityShipHostile) count++;
                } else {
                    if (ent instanceof BasicEntityShipHostile && ent.isNonBoss()) count++;
                }
            }
        }
        return count;
    }

    public static void clearMountSeat(final EntityLiving host) {
        if (host.isRiding()) {
            if (host.getRidingEntity() instanceof BasicEntityMount) {
                ((BasicEntityMount) host.getRidingEntity()).clearRider();
            }
            host.dismountRidingEntity();
        }
        host.removePassengers();
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getEntitiesWithinAABB(final World world, final Class cls, final AxisAlignedBB aabb, @Nullable final Predicate filter) {
        final List<Entity> allEntities = world.getEntitiesWithinAABB(Entity.class, aabb);
        final List<T> filteredList = new ArrayList<>();
        for (final Entity entity : allEntities) {
            if (cls.isInstance(entity) && (filter == null || filter.apply(entity))) {
                filteredList.add((T) entity);
            }
        }
        return filteredList;
    }

    public static <T extends EntityLivingBase> void travel(final T host, final float strafe, final float forward) {
        if (((IShipNavigator) host).canFly()) {
            final double d0 = host.posY;
            host.moveRelative(strafe, 0F, forward, ((IShipNavigator) host).getMoveSpeed() * 0.4F);
            host.move(MoverType.SELF, host.motionX, host.motionY, host.motionZ);
            host.motionX *= 0.91D;
            host.motionY *= 0.91D;
            host.motionZ *= 0.91D;
            if (host.collidedHorizontally && host.isOffsetPositionInLiquid(host.motionX, host.motionY + 0.6D - host.posY + d0, host.motionZ)) {
                host.motionY = 0.3D;
            }
        } else if (EntityHelper.checkEntityIsInLiquid(host)) {
            host.moveRelative(strafe, 0F, forward, ((IShipNavigator) host).getMoveSpeed() * 0.4F);
            host.move(MoverType.SELF, host.motionX, host.motionY, host.motionZ);
            host.motionX *= 0.8D;
            host.motionY *= 0.8D;
            host.motionZ *= 0.8D;
            final EnumFacing facing = host.getHorizontalFacing();
            final double checkXOffset = facing.getFrontOffsetX() * (host.width / 2.0F + 0.2F);
            final double checkYPos = host.posY + host.getEyeHeight() + 0.2D;
            final double checkZOffset = facing.getFrontOffsetZ() * (host.width / 2.0F + 0.2F);
            final BlockPos checkPosUpper = new BlockPos(host.posX + checkXOffset, checkYPos, host.posZ + checkZOffset);
            final IBlockState blockStateUpper = host.world.getBlockState(checkPosUpper);
            final Block blockUpper = blockStateUpper.getBlock();
            final boolean noSolidBlockAbove = blockUpper == Blocks.AIR || blockStateUpper.getMaterial().isLiquid();
            if (host.collidedHorizontally && noSolidBlockAbove) {
                host.motionY = 0.3D;
            }
        } else {
            final BlockPos.PooledMutableBlockPos pooledPos = BlockPos.PooledMutableBlockPos.retain(host.posX, host.getEntityBoundingBox().minY - 1.0D, host.posZ);
            float slipperiness = 0.91F;
            if (host.onGround) slipperiness = host.world.getBlockState(pooledPos).getBlock().slipperiness * 0.91F;
            final float groundFactor = 0.16277136F / (slipperiness * slipperiness * slipperiness);
            final float moveFactor = host.onGround ? ((IShipNavigator) host).getMoveSpeed() * groundFactor : host.jumpMovementFactor;
            if (((IShipNavigator) host).isJumping()) {
                host.motionY += ((IShipNavigator) host).getMoveSpeed() * ((IShipNavigator) host).getJumpSpeed() * 0.1F;
            }
            host.moveRelative(strafe, 0F, forward, moveFactor);
            if (host.onGround) slipperiness = host.world.getBlockState(pooledPos.setPos(host.posX, host.getEntityBoundingBox().minY - 1.0D, host.posZ)).getBlock().slipperiness * 0.91F;
            else slipperiness = 0.91F;
            if (host.isOnLadder()) {
                final float ladderSpeed = 0.15F;
                host.motionX = MathHelper.clamp(host.motionX, -ladderSpeed, ladderSpeed);
                host.motionZ = MathHelper.clamp(host.motionZ, -ladderSpeed, ladderSpeed);
                host.fallDistance = 0.0F;
                if (host.motionY < -0.15D) host.motionY = -0.15D;
            }
            host.move(MoverType.SELF, host.motionX, host.motionY, host.motionZ);
            if (host.collidedHorizontally && host.isOnLadder()) host.motionY = 0.4D;
            if (host.isPotionActive(MobEffects.LEVITATION)) {
                host.motionY += (0.05D * (host.getActivePotionEffect(MobEffects.LEVITATION).getAmplifier() + 1) - host.motionY) * 0.2D;
            } else {
                pooledPos.setPos(host.posX, 0.0D, host.posZ);
                if (host.world.isRemote && (!host.world.isBlockLoaded(pooledPos) || !host.world.getChunkFromBlockCoords(pooledPos).isLoaded())) {
                    host.motionY = (host.posY > 0.0D) ? -0.1D : 0.0D;
                } else if (!host.hasNoGravity()) {
                    host.motionY -= 0.08D;
                }
            }
            host.motionY *= 0.98D;
            host.motionX *= slipperiness;
            host.motionZ *= slipperiness;
            pooledPos.release();
        }
        host.prevLimbSwingAmount = host.limbSwingAmount;
        final double dx = host.posX - host.prevPosX;
        final double dz = host.posZ - host.prevPosZ;
        float f10 = MathHelper.sqrt(dx * dx + dz * dz) * 4.0F;
        if (f10 > 1.0F) f10 = 1.0F;
        host.limbSwingAmount += (f10 - host.limbSwingAmount) * 0.4F;
        host.limbSwing += host.limbSwingAmount;
    }

    public static ItemStack getPointerInUse(final EntityPlayer player) {
        if (player == null) return null;
        final ItemStack itemMain = player.getHeldItemMainhand();
        if (itemMain.getItem() == ModItems.PointerItem) return itemMain;
        final ItemStack itemOff = player.getHeldItemOffhand();
        if (itemOff.getItem() == ModItems.PointerItem) return itemOff;
        return null;
    }

    public static void handlePointerKeyInput() {
        final EntityPlayer player = ClientProxy.getClientPlayer();
        final GameSettings keySet = ClientProxy.getGameSetting();
        final ItemStack pointer = EntityHelper.getPointerInUse(player);
        if (pointer != null) {
            if (keySet.keyBindSprint.isKeyDown()) {
                for (int i = 0; i < keySet.keyBindsHotbar.length; ++i) {
                    if (keySet.keyBindsHotbar[i].isPressed()) {
                        LogHelper.debug("DEBUG: key input: pointer set team: " + i + " currItem: " + player.inventory.currentItem);
                        CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, (byte) 27, i, player.inventory.currentItem));
                        return;
                    }
                }
            } else if (player.getHeldItemMainhand() == pointer && keySet.keyBindPlayerList.isPressed()) {
                int meta = pointer.getMetadata();
                switch (meta) {
                    case 1: case 2: meta += 3; break;
                    case 3: case 4: case 5: meta -= 3; break;
                    default: meta = 3;
                }
                pointer.setItemDamage(meta);
                CommonProxy.channelG.sendToServer(new C2SGUIPackets(player, (byte) 24, meta));
            }
        }
    }

    public static boolean isNotHost(final IShipOwner host, final Entity target) {
        if (host == null || target == null) return false;
        if (target.equals(host)) return false;
        final Entity hostEntity = host.getHostEntity();
        if (hostEntity != null) {
            if (target.equals(hostEntity)) return false;
            if (target.equals(hostEntity.getRidingEntity())) return false;
            for (final Entity rider : hostEntity.getPassengers()) {
                if (target.equals(rider)) return false;
            }
        }
        return true;
    }

    public static int getMoraleLevel(final int m) {
        if (m > 5100) return 0;
        if (m > 3900) return 1;
        if (m > 2100) return 2;
        if (m > 900) return 3;
        return 4;
    }

    public static int getBodyArrayIDFromHeight(final int heightPercent, final BasicEntityShip ship) {
        if (ship == null) return -1;
        final byte[] heightArray = ship.isSitting() ? ship.getBodyHeightSit() : ship.getBodyHeightStand();
        for (int i = 0; i < heightArray.length; ++i) {
            if (heightPercent > heightArray[i]) {
                return i;
            }
        }
        return -1;
    }

    public static int[] getBodyRangeFromHeight(final int heightPercent, final BasicEntityShip ship) {
        if (ship == null) return new int[]{1, 0};
        final int hit = EntityHelper.getBodyArrayIDFromHeight(heightPercent, ship);
        final byte[] heightArray = ship.isSitting() ? ship.getBodyHeightSit() : ship.getBodyHeightStand();
        switch (hit) {
            case 0: return new int[]{120, heightArray[0]};
            case 1: case 2: case 3: case 4: case 5: return new int[]{heightArray[hit - 1], heightArray[hit]};
            default: return new int[]{heightArray[5], -20};
        }
    }

    public static Enums.BodyHeight getBodyIDFromHeight(final int heightPercent, final BasicEntityShip ship) {
        if (ship == null) return Enums.BodyHeight.LEG;
        switch (EntityHelper.getBodyArrayIDFromHeight(heightPercent, ship)) {
            case 0: return Enums.BodyHeight.TOP;
            case 1: return Enums.BodyHeight.HEAD;
            case 2: return Enums.BodyHeight.NECK;
            case 3: return Enums.BodyHeight.CHEST;
            case 4: return Enums.BodyHeight.BELLY;
            case 5: return Enums.BodyHeight.UBELLY;
            default: return Enums.BodyHeight.LEG;
        }
    }

    public static Enums.BodySide getHitAngleID(final int angle) {
        if (angle >= 250 && angle < 290) return Enums.BodySide.RIGHT;
        if (angle >= 110 && angle < 250) return Enums.BodySide.FRONT;
        if (angle >= 70 && angle < 110) return Enums.BodySide.LEFT;
        return Enums.BodySide.BACK;
    }

    public static int getHitBodyID(final BasicEntityShip ship) {
        return EntityHelper.getHitBodyID(ship.getBodyIDFromHeight(), ship.getHitAngleID());
    }

    public static int getHitBodyID(final Enums.BodyHeight h, final Enums.BodySide s) {
        switch (h) {
            case TOP: return 7;
            case HEAD: return (s == Enums.BodySide.FRONT) ? 4 : 8;
            case NECK: return 3;
            case CHEST: if (s == Enums.BodySide.FRONT) return 1; if (s == Enums.BodySide.BACK) return 5; return 10;
            case BELLY: if (s == Enums.BodySide.FRONT) return 6; if (s == Enums.BodySide.BACK) return 2; return 10;
            case UBELLY: return (s == Enums.BodySide.FRONT) ? 0 : 2;
            case LEG: default: return 9;
        }
    }

    public static Entity createShipEntity(final World world, final int classID, final NBTTagCompound nbt, final double px, final double py, final double pz, final boolean updateUID) {
        final String name = ShipCalc.getEntityToSpawnName(classID);
        final Entity ent = EntityList.createEntityByIDFromName(new ResourceLocation(name), world);
        if (ent == null) return null;
        ent.setPosition(px, py, pz);
        world.spawnEntity(ent);
        if (ent instanceof BasicEntityShip) {
            final BasicEntityShip ship = (BasicEntityShip) ent;
            ship.setHealth(ship.getMaxHealth());
            ship.isDead = false;
            ship.deathTime = 0;
            if (nbt != null) {
                ship.readFromNBT(nbt);
                if (ship.getHealth() < 1.0F) ship.setHealth(1.0F);
                ship.setStateFlag(10, true);
                ship.isDead = false;
                ship.deathTime = 0;
            }
            if (updateUID) ship.updateShipCacheDataWithoutNewID();
        }
        return ent;
    }

    public static void showNameTag(final BasicEntityShip ship) {
        if (ship == null || ship.unitNames == null) return;
        if (ConfigHandler.showTag || ClientProxy.getGameSetting().keyBindSprint.isKeyDown() || ship.getDistanceSq(ClientProxy.getClientPlayer()) < (ConfigHandler.nameTagDist * ConfigHandler.nameTagDist)) {
            final StringBuilder sb = new StringBuilder();
            final String ownerColor = (ship.getPlayerUID() == EntityHelper.getPlayerUID(ClientProxy.getClientPlayer())) ? TextFormatting.YELLOW.toString() : TextFormatting.GOLD.toString();
            sb.append(ownerColor);
            int strH = 0;
            int strLen = 1;
            if (!ship.unitNames.isEmpty()) {
                for (final String s : ship.unitNames) {
                    if (s != null && s.length() > 1) {
                        strH++;
                        final int tempLen = ClientProxy.getMineraft().getRenderManager().getFontRenderer().getStringWidth(s);
                        if (tempLen > strLen) strLen = tempLen;
                        sb.append(s).append("\n");
                    }
                }
            }
            if (ConfigHandler.debugMode) {
                final String uids = TextFormatting.GREEN + "UID " + ship.getShipUID();
                strH++;
                sb.append(uids);
                final int tempLen = ClientProxy.getMineraft().getRenderManager().getFontRenderer().getStringWidth(uids);
                if (tempLen > strLen) strLen = tempLen;
            }
            ParticleHelper.spawnAttackParticleAt(sb.toString(), 0.0, ship.height + 0.8, 0.0, (byte) 1, strH, strLen + 1, ship.getEntityId());
        }
    }

    public static void updateNameTag(final BasicEntityShip ship) {
        if (ship == null) return;
        final CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(ship.getPlayerUID());
        if (capa != null) {
            final List<Integer> tid = capa.getShipTeamIDArray(ship.getShipUID());
            final ArrayList<String> tname = new ArrayList<>();
            for (final int t : tid) {
                tname.add(capa.getUnitName(t));
            }
            ship.unitNames = tname;
            ship.sendSyncPacketUnitName();
        }
    }

    public static boolean applyTeleport(final IShipNavigator host, final double dist, final Vec3d tpPos) {
        if (!ConfigHandler.canTeleport) return false;
        if (host == null) return false;
        try {
            final Chunk c = ((Entity) host).world.getChunkProvider().getLoadedChunk(MathHelper.floor(tpPos.x) >> 4, MathHelper.floor(tpPos.z) >> 4);
            if (c == null) return false;
        } catch (final Exception e) {
            return false;
        }
        if (host instanceof BasicEntityMount) {
            final BasicEntityShip hostShip = (BasicEntityShip) ((BasicEntityMount) host).getHostEntity();
            if (dist > 1024.0) {
                EntityHelper.clearMountSeat((BasicEntityMount) host);
                if (hostShip != null) EntityHelper.clearMountSeat(hostShip);
            }
            ((BasicEntityMount) host).setDead();
            if (hostShip != null) {
                hostShip.setPositionAndUpdate(tpPos.x, tpPos.y, tpPos.z);
                EntityHelper.sendPositionSyncPacket(hostShip);
            }
            return true;
        }
        if (host instanceof EntityLiving) {
            final EntityLiving host2 = (EntityLiving) host;
            if (dist > 1024.0) EntityHelper.clearMountSeat(host2);
            host.getShipNavigate().clearPathEntity();
            host2.setPositionAndUpdate(tpPos.x, tpPos.y, tpPos.z);
            EntityHelper.sendPositionSyncPacket(host2);
            return true;
        }
        return false;
    }

    public static void sendPositionSyncPacket(final Entity ent) {
        final NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(ent.dimension, ent.posX, ent.posY, ent.posZ, 256.0);
        CommonProxy.channelE.sendToAllAround(new S2CEntitySync(ent, 0, (byte) 52), point);
    }

    public static NBTTagCompound saveShipDataToNBT(final BasicEntityShip ship, final boolean punish) {
        final NBTTagCompound nbt = new NBTTagCompound();
        final Attrs attrs = ship.getAttrs();
        final int[] attrsData = new int[7];
        attrsData[0] = punish ? Math.max(1, ship.getLevel() - 1) : ship.getLevel();
        for (int i = 0; i < 6; i++) attrsData[i + 1] = attrs.getAttrsBonus(i);
        final int[] stateData = {ship.getStateEmotion(0), 0, ship.getStateMinor(10), ship.getStateMinor(11), ship.getStateMinor(12), ship.getStateMinor(44), ship.getStateMinor(9)};
        final byte[] flagsData = {ship.getStateFlag(1) ? (byte) 1 : 0, ship.getStateFlag(3) ? (byte) 1 : 0, ship.getStateFlag(4) ? (byte) 1 : 0, ship.getStateFlag(5) ? (byte) 1 : 0, ship.getStateFlag(6) ? (byte) 1 : 0, ship.getStateFlag(7) ? (byte) 1 : 0, ship.getStateFlag(9) ? (byte) 1 : 0, ship.getStateFlag(12) ? (byte) 1 : 0, ship.getStateFlag(18) ? (byte) 1 : 0, ship.getStateFlag(19) ? (byte) 1 : 0, ship.getStateFlag(20) ? (byte) 1 : 0, ship.getStateFlag(21) ? (byte) 1 : 0, ship.getStateFlag(22) ? (byte) 1 : 0, ship.getStateFlag(23) ? (byte) 1 : 0, ship.getStateFlag(25) ? (byte) 1 : 0, ship.getStateFlag(26) ? (byte) 1 : 0};
        nbt.setString("owner", EntityHelper.getPetPlayerUUID(ship));
        nbt.setString("ownername", EntityHelper.getOwnerName(ship));
        nbt.setInteger("PlayerID", ship.getStateMinor(21));
        nbt.setTag("CpInv", ship.getCapaShipInventory().serializeNBT());
        nbt.setIntArray("Attrs", attrsData);
        nbt.setIntArray("Attrs2", stateData);
        nbt.setByteArray("Flags", flagsData);
        nbt.setInteger("ShipID", ship.getStateMinor(22));
        nbt.setString("customname", ship.getCustomNameTag());
        return nbt;
    }

    public static void spawnMobShip(final EntityPlayer player, final CapaTeitoku capa) {
        if (player == null || capa == null || player.world.getDifficulty() == EnumDifficulty.PEACEFUL) return;
        boolean canSpawn = !ConfigHandler.checkRing || capa.hasRing();
        final int blockX = (int) player.posX;
        final int blockZ = (int) player.posZ;
        final Biome biome = player.world.getBiomeForCoordsBody(new BlockPos(blockX, 0, blockZ));
        if (canSpawn && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.WATER) && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH)) {
            canSpawn = false;
        }
        final World w = player.world;
        final Random rng = player.getRNG();
        if (canSpawn && EntityHelper.getEntityNumber(0, w) <= ConfigHandler.mobSpawn[0] && rng.nextInt(100) <= ConfigHandler.mobSpawn[1]) {
            int groups = ConfigHandler.mobSpawn[2];
            for (int loop = 30 + groups * 30; groups > 0 && loop > 0; --loop) {
                final int offX = rng.nextInt(30) + 20;
                final int offZ = rng.nextInt(30) + 20;
                int spawnX = blockX;
                int spawnZ = blockZ;
                switch (rng.nextInt(4)) {
                    case 0: spawnX += offX; spawnZ += offZ; break;
                    case 1: spawnX -= offX; spawnZ -= offZ; break;
                    case 2: spawnX += offX; spawnZ -= offZ; break;
                    case 3: spawnX -= offX; spawnZ += offZ; break;
                    default:
                }
                final int groundY = w.provider.getAverageGroundLevel() - 2;
                final IBlockState blockY = w.getBlockState(new BlockPos(spawnX, groundY, spawnZ));
                LogHelper.debug("DEBUG: spawn mob ship: group: " + groups + " get block: " + blockY.getBlock().getLocalizedName() + " " + spawnX + " " + groundY + " " + spawnZ);
                if (blockY.getMaterial() != Material.WATER) continue;
                groups--;
                final int spawnY = BlockHelper.getToppestWaterHeight(w, spawnX, w.provider.getAverageGroundLevel() - 3, spawnZ);
                int shipNum = ConfigHandler.mobSpawn[3] > 0 ? ConfigHandler.mobSpawn[3] : 1;
                final int ranMax = ConfigHandler.mobSpawn[4] - shipNum;
                if (ranMax > 0) shipNum += rng.nextInt(ranMax + 1);
                for (int i = 0; i < shipNum; ++i) {
                    final Entity mobToSpawn = EntityList.createEntityByIDFromName(new ResourceLocation(ShipCalc.getRandomMobToSpawnName()), w);
                    if (mobToSpawn instanceof BasicEntityShipHostile) {
                        ((BasicEntityShipHostile) mobToSpawn).initAttrs(rng.nextInt(10) > 7 ? 1 : 0);
                        mobToSpawn.setPosition(spawnX + rng.nextDouble(), spawnY + 0.5, spawnZ + rng.nextDouble());
                        w.spawnEntity(mobToSpawn);
                    }
                }
            }
        }
    }

    public static void spawnBossShip(final EntityPlayer player, final CapaTeitoku capa) {
        if (player == null || capa == null || player.world.getDifficulty() == EnumDifficulty.PEACEFUL) return;
        final Biome biome = player.world.getBiomeForCoordsBody(new BlockPos(player.posX, 0, player.posZ));
        if ((BiomeDictionary.hasType(biome, BiomeDictionary.Type.WATER) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH)) && capa.hasRing()) {
            capa.setBossCooldown(capa.getBossCooldown() - 1);
        }
        if (capa.getBossCooldown() <= 0) {
            final World w = player.world;
            final Random rng = player.getRNG();
            capa.setBossCooldown(ConfigHandler.bossCooldown);
            if (rng.nextInt(4) != 0) return;
            LogHelper.debug("DEBUG: spawn boss: roll spawn success");
            for (int i = 0; i < 20; ++i) {
                final int offX = rng.nextInt(32) + 32;
                final int offZ = rng.nextInt(32) + 32;
                int spawnX = (int) player.posX;
                int spawnZ = (int) player.posZ;
                switch (rng.nextInt(4)) {
                    case 0: spawnX += offX; spawnZ += offZ; break;
                    case 1: spawnX -= offX; spawnZ -= offZ; break;
                    case 2: spawnX += offX; spawnZ -= offZ; break;
                    case 3: spawnX -= offX; spawnZ += offZ; break;
                    default:
                }
                final int groundY = w.provider.getAverageGroundLevel() - 2;
                final IBlockState blockY = w.getBlockState(new BlockPos(spawnX, groundY, spawnZ));
                LogHelper.debug("DEBUG: spawn boss: check block: " + blockY.getBlock().getLocalizedName() + " " + spawnX + " " + groundY + " " + spawnZ);
                if (blockY.getMaterial() != Material.WATER) continue;
                final int spawnY = BlockHelper.getToppestWaterHeight(w, spawnX, 63, spawnZ);
                final AxisAlignedBB aabb = new AxisAlignedBB(spawnX - 48.0, spawnY - 48.0, spawnZ - 48.0, spawnX + 48.0, spawnY + 48.0, spawnZ + 48.0);
                final List<BasicEntityShipHostile> listBoss = w.getEntitiesWithinAABB(BasicEntityShipHostile.class, aabb);
                long bossNum = listBoss.stream().filter(mob -> !mob.isNonBoss()).count();
                LogHelper.debug("DEBUG: spawn boss: check existed boss: " + bossNum + " all mob: " + listBoss.size());
                if (bossNum >= 2) continue;
                for (int j = 0; j < ConfigHandler.spawnBossNum; ++j) {
                    final Entity mobToSpawn = EntityList.createEntityByIDFromName(new ResourceLocation(ShipCalc.getRandomMobToSpawnName()), w);
                    if (mobToSpawn instanceof BasicEntityShipHostile) {
                        ((BasicEntityShipHostile) mobToSpawn).initAttrs(rng.nextInt(100) > 65 ? 3 : 2);
                        mobToSpawn.setPosition((double)spawnX + rng.nextInt(3), spawnY + 0.5, (double)spawnZ + rng.nextInt(3));
                        w.spawnEntity(mobToSpawn);
                    }
                }
                for (int j = 0; j < ConfigHandler.spawnMobNum; ++j) {
                    final Entity mobToSpawn = EntityList.createEntityByIDFromName(new ResourceLocation(ShipCalc.getRandomMobToSpawnName()), w);
                    if (mobToSpawn instanceof BasicEntityShipHostile) {
                        ((BasicEntityShipHostile) mobToSpawn).initAttrs(rng.nextInt(2));
                        mobToSpawn.setPosition((double)spawnX + rng.nextInt(3), spawnY + 0.5, (double)spawnZ + rng.nextInt(3));
                        w.spawnEntity(mobToSpawn);
                    }
                }
                final TextComponentTranslation spawnText = new TextComponentTranslation(rng.nextBoolean() ? "chat.shincolle:bossspawn1" : "chat.shincolle:bossspawn2");
                ServerProxy.getServer().sendMessage(new TextComponentString(TextFormatting.YELLOW + spawnText.getFormattedText() + TextFormatting.AQUA + " " + spawnX + " " + spawnY + " " + spawnZ));
                LogHelper.debug("DEBUG: spawn fleet " + spawnX + " " + spawnY + " " + spawnZ);
                break;
            }
        }
    }
}