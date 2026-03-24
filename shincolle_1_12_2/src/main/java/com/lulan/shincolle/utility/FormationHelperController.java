package com.lulan.shincolle.utility;

import com.lulan.shincolle.capability.CapaControllerSavedValues;
import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.other.EntityFleetController;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class FormationHelperController {
    private FormationHelperController() {}

    public static void removeOldController(EntityPlayer player, String key) {
        CapaControllerSavedValues data = CapaControllerSavedValues.get(player.world);
        Integer oldControllerUID = data.getControllerUID(key);
        if (oldControllerUID != null) {
            for (Entity entity : player.world.loadedEntityList) {
                if (entity instanceof EntityFleetController) {
                    EntityFleetController controller = (EntityFleetController) entity;
                    if (controller.getOwnerUID() == oldControllerUID && !controller.isDead) {
                        controller.setDead();
                        break;
                    }
                }
            }
            data.removeController(key);
        }
    }

    public static void spawnControllerAtFlagship(EntityPlayer player, int teamMode, Vec3d target, float maxspd) {
        if (player.world.isRemote) return;
        CapaControllerSavedValues data = CapaControllerSavedValues.get(player.world);
        String key = EntityHelper.getPlayerUID(player) + "_" + teamMode;
        removeOldController(player, key);
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
        if (capa == null) return;
        List<BasicEntityShip> ships = capa.getShipEntityByMode(teamMode);
        if (ships.isEmpty()) return;
        BasicEntityShip flagship = ships.get(0);
        if (flagship != null) {
            World world = flagship.world;
            EntityFleetController controller = new EntityFleetController(world, player, teamMode,
                    flagship.posX, flagship.posY, flagship.posZ, -flagship.rotationYaw,
                    target.x, target.y, target.z, maxspd, 5.0f);
            world.spawnEntity(controller);
            data.addController(key, controller.getOwnerUID());
            for (int i = 0; i < ships.size(); i++) {
                if (ships.get(i) != null) {
                    ships.get(i).onFleetLeaderSpawned(i == 0 ? 1 : 0);
                }
            }
        }
    }
}
