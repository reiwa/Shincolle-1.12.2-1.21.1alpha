package com.lulan.shincolle.utility;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipOwner;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.team.TeamData;
import com.lulan.shincolle.tileentity.BasicTileInventory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import com.lulan.shincolle.entity.other.EntityFleetController;

import java.util.Comparator;
import java.util.Optional;
import java.util.List;

public class TeamHelper {
    private TeamHelper() {}

    public static TeamData getTeamDataByUID(int uid) {
        if (uid > 0) {
            return ServerProxy.getTeamData(uid);
        }
        return null;
    }

    public static boolean doFriendlyFire(IShipOwner attacker, Entity target) {
        if (attacker != null && target != null) {
            int ida = attacker.getPlayerUID();
            int idb = EntityHelper.getPlayerUID(target);
            if (ConfigHandler.friendlyFire) {
                return (ida <= 0 && ida >= -1) || ida != idb;
            } else {
                if (idb < -1 && ida == idb) {
                    return false;
                }
                if (ida >= -1 && target instanceof EntityPlayer) {
                    return false;
                }
                return !TeamHelper.checkIsAlly(ida, idb);
            }
        }
        return true;
    }

    public static boolean checkIsAlly(Entity host, Entity target) {
        if (host != null && target != null) {
            int hostID = EntityHelper.getPlayerUID(host);
            int tarID = EntityHelper.getPlayerUID(target);
            return TeamHelper.checkIsAlly(hostID, tarID);
        }
        return false;
    }

    public static boolean checkIsAlly(int hostPID, int tarPID) {
        if (hostPID < -1 && tarPID < -1) {
            return true;
        }
        if (hostPID < -1 && tarPID > 0 || hostPID > 0 && tarPID < -1) {
            return false;
        }
        if (hostPID > 0 && tarPID > 0) {
            if (hostPID == tarPID) {
                return true;
            }
            TeamData hostTeam = TeamHelper.getTeamDataByUID(hostPID);
            TeamData tarTeam = TeamHelper.getTeamDataByUID(tarPID);
            if (hostTeam != null && tarTeam != null) {
                List<Integer> alist = hostTeam.getTeamAllyList();
                return alist.contains(tarTeam.getTeamID());
            }
        }
        return false;
    }

    public static boolean checkIsBanned(Entity host, Entity target) {
        if (host != null && target != null) {
            int hostID = EntityHelper.getPlayerUID(host);
            int tarID = EntityHelper.getPlayerUID(target);
            return TeamHelper.checkIsBanned(hostID, tarID);
        }
        return false;
    }

    public static boolean checkIsBanned(int hostPID, int tarPID) {
        if (hostPID < -1 && tarPID < -1) {
            return false;
        }
        if (hostPID < -1 && tarPID > 0 || hostPID > 0 && tarPID < -1) {
            return true;
        }
        if (hostPID > 0 && tarPID > 0) {
            TeamData hostTeam = TeamHelper.getTeamDataByUID(hostPID);
            TeamData tarTeam = TeamHelper.getTeamDataByUID(tarPID);
            if (hostTeam != null && tarTeam != null) {
                List<Integer> alist = hostTeam.getTeamBannedList();
                return alist.contains(tarTeam.getTeamID());
            }
        }
        return false;
    }

    public static boolean checkSameOwner(Entity enta, Entity entb) {
        int ida = EntityHelper.getPlayerUID(enta);
        int idb = EntityHelper.getPlayerUID(entb);
        if (!(ida <= 0 && ida >= -1 || idb <= 0 && idb >= -1)) {
            return ida == idb;
        }
        return false;
    }

    public static void updateTeamList(EntityPlayer player, CapaTeitoku capa) {
        BasicEntityShip getent = null;
        for (int i = 0; i < 6; ++i) {
            getent = EntityHelper.getShipByUID(capa.getSIDCurrentTeam(i));
            if (getent != null) {
                if (TeamHelper.checkSameOwner(getent, player)) {
                    capa.addShipEntityToCurrentTeam(i, getent);
                    continue;
                }
                capa.addShipEntityToCurrentTeam(i, null);
                continue;
            }
            if (capa.getSIDCurrentTeam(i) > 0) continue;
            capa.addShipEntityToCurrentTeam(i, null);
        }
    }

    public static boolean isUsableByPlayer(BasicTileInventory tile, EntityPlayer player) {
        if (tile == null || player == null) {
            return false;
        }
        int tid = 0;
        tid = tile.getPlayerUID();
        if (BlockHelper.checkTileOwner(player, tile) || EntityHelper.checkOP(player) || TeamHelper.checkIsAlly(tid, EntityHelper.getPlayerUID(player))) {
            return tile.getWorld().getTileEntity(tile.getPos()) == tile && !tile.isInvalid() && player.getDistanceSq(tile.getPos().getX() + 0.5, tile.getPos().getY() + 0.5, tile.getPos().getZ() + 0.5) <= 64.0;
        }
        return false;
    }

    public static Optional<EntityFleetController> getActiveFleetController(BasicEntityShip ship) {
        if (ship == null) {
            return Optional.empty();
        }
        int ownerUID = ship.getPlayerUID();
        EntityPlayer ownerPlayer = EntityHelper.getEntityPlayerByUID(ship.getPlayerUID());
        if (ownerPlayer == null) {
            return Optional.empty();
        }
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(ownerPlayer);
        if (capa == null) {
            return Optional.empty();
        }
        int teamID = capa.getCurrentTeamID();
        return ship.world
                .getEntitiesWithinAABB(
                        EntityFleetController.class,
                        ship.getEntityBoundingBox().grow(128.0D)
                )
                .stream()
                .filter(c -> ownerUID == c.getOwnerUID() && teamID == c.getTeamMode())
                .min(Comparator.comparingDouble(c -> c.getDistanceSq(ship)));
    }
}