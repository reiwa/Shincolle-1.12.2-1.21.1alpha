package com.lulan.shincolle.utility;

import com.lulan.shincolle.crafting.ShipCalc;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.C2SInputPackets;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.server.CacheDataShip;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CommandHelper {
    private CommandHelper() {}

    @SideOnly(value=Side.CLIENT)
    public static void processShipChangeOwner(int senderEID, int ownerEID) {
        EntityPlayer sender = EntityHelper.getEntityPlayerByID(senderEID, 0, true);
        if (sender != null) {
            RayTraceResult hitObj = EntityHelper.getPlayerMouseOverEntity(32.0, 1.0f);
            if (hitObj != null && hitObj.entityHit instanceof BasicEntityShip) {
                sender.sendMessage(new TextComponentTranslation("chat.shincolle:command.command").appendSibling(new TextComponentString(" shipchangeowner: ship: " + TextFormatting.AQUA + hitObj.entityHit)));
                CommonProxy.channelI.sendToServer(new C2SInputPackets((byte)3, ownerEID, hitObj.entityHit.getEntityId(), hitObj.entityHit.world.provider.getDimension()));
            } else {
                sender.sendMessage(new TextComponentTranslation("chat.shincolle:command.notship"));
            }
        }
    }

    @SideOnly(value=Side.CLIENT)
    public static void processShowShipInfo(int senderEID) {
        RayTraceResult hitObj;
        EntityPlayer sender = EntityHelper.getEntityPlayerByID(senderEID, 0, true);
        if (sender != null && (hitObj = EntityHelper.getPlayerMouseOverEntity(32.0, 1.0f)) != null && hitObj.entityHit instanceof BasicEntityShip) {
            BasicEntityShip ship = (BasicEntityShip)hitObj.entityHit;
            sender.sendMessage(new TextComponentTranslation("chat.shincolle:command.command").appendSibling(new TextComponentString(" user: " + TextFormatting.LIGHT_PURPLE + sender.getName() + TextFormatting.RESET + " UID: " + TextFormatting.AQUA + EntityHelper.getPlayerUID(sender) + TextFormatting.RESET + " UUID: " + TextFormatting.GOLD + sender.getUniqueID())));
            sender.sendMessage(new TextComponentString("CustomName: " + TextFormatting.AQUA + ship.getCustomNameTag()));
            sender.sendMessage(new TextComponentString("EntityID: " + TextFormatting.GOLD + ship.getEntityId()));
            sender.sendMessage(new TextComponentString("UID: " + TextFormatting.GREEN + ship.getShipUID()));
            sender.sendMessage(new TextComponentString("Owner UID: " + TextFormatting.RED + ship.getPlayerUID()));
            sender.sendMessage(new TextComponentString("Owner UUID: " + TextFormatting.YELLOW + EntityHelper.getPetPlayerUUID(ship)));
            sender.sendMessage(new TextComponentString("Morale: " + TextFormatting.YELLOW + ship.getMorale()));
        }
    }

    @SideOnly(value=Side.CLIENT)
    public static void processSetShipAttrs(int[] cmdData) {
        RayTraceResult hitObj;
        EntityPlayer sender = EntityHelper.getEntityPlayerByID(cmdData[0], 0, true);
        if (sender != null && (hitObj = EntityHelper.getPlayerMouseOverEntity(16.0, 1.0f)) != null && hitObj.entityHit instanceof BasicEntityShip) {
            BasicEntityShip ship = (BasicEntityShip)hitObj.entityHit;
            if (cmdData.length == 8) {
                sender.sendMessage(new TextComponentTranslation("chat.shincolle:command.command").appendSibling(new TextComponentString(" shipattrs: LV: " + TextFormatting.LIGHT_PURPLE + cmdData[1] + TextFormatting.RESET + " BonusValue: " + TextFormatting.RED + cmdData[2] + " " + cmdData[3] + " " + cmdData[4] + " " + cmdData[5] + " " + cmdData[6] + " " + cmdData[7])));
                sender.sendMessage(new TextComponentString("" + TextFormatting.AQUA + ship));
                CommonProxy.channelI.sendToServer(new C2SInputPackets((byte)4, ship.getEntityId(), ship.world.provider.getDimension(), cmdData[1], cmdData[2], cmdData[3], cmdData[4], cmdData[5], cmdData[6], cmdData[7]));
            } else if (cmdData.length == 2) {
                sender.sendMessage(new TextComponentTranslation("chat.shincolle:command.command").appendSibling(new TextComponentString(" shipattrs: LV: " + TextFormatting.LIGHT_PURPLE + cmdData[1])));
                sender.sendMessage(new TextComponentString("" + TextFormatting.AQUA + ship));
                CommonProxy.channelI.sendToServer(new C2SInputPackets((byte)4, ship.getEntityId(), ship.world.provider.getDimension(), cmdData[1]));
            }
        }
    }

    public static void processSendShipList(ByteBuf buf, int page) {
        HashMap<Integer, CacheDataShip> map = ServerProxy.getAllShipWorldData();
        ArrayList<BasicEntityShip> ships = new ArrayList<>();
        map.forEach((uid, data) -> {
            Entity ent;
            if (data != null && (ent = EntityHelper.getEntityByID(data.entityID, data.worldID, false)) instanceof BasicEntityShip) {
                ships.add((BasicEntityShip)ent);
            }
        });
        for (BasicEntityShip s : ships) {
            s.updateShipCacheDataWithoutNewID();
        }
        int size = map.size();
        int numPerPage = ConfigHandler.shipNumPerPage;
        int maxPage = (size - 1) / numPerPage + 1;
        if (page <= 0) {
            numPerPage = 30000;
            maxPage = (size - 1) / numPerPage + 1;
            page = 1;
        }
        buf.writeInt(maxPage);
        buf.writeInt(page);
        int check = (page - 1) * numPerPage;
        if (page < 1 || page > maxPage) {
            buf.writeInt(0);
            return;
        }
        if (page == maxPage) {
            buf.writeInt(size - check);
        } else {
            buf.writeInt(numPerPage);
        }
        int index = 0;
        int div = 0;
        --page;
        for (Map.Entry<Integer, CacheDataShip> entry : map.entrySet()) {
            div = index / numPerPage;
            ++index;
            if (div == page) {
                NBTTagCompound nbt;
                int uid2 = entry.getKey();
                int pid = 0;
                int level = -1;
                String ownerName = null;
                CacheDataShip data2 = entry.getValue();
                Entity entity = EntityHelper.getEntityByID(data2.entityID, data2.worldID, false);
                if (data2.entityNBT != null && (nbt = (NBTTagCompound)data2.entityNBT.getTag("ShipExtProps")) != null) {
                    pid = nbt.getInteger("PlayerUID");
                    ownerName = nbt.getString("Owner");
                    NBTTagCompound nbt2 = (NBTTagCompound)nbt.getTag("Minor");
                    if (nbt2 != null) {
                        level = nbt2.getInteger("Level");
                    }
                }
                buf.writeInt(uid2);
                buf.writeInt(data2.worldID);
                buf.writeInt(data2.classID);
                buf.writeInt(data2.posX);
                buf.writeInt(data2.posY);
                buf.writeInt(data2.posZ);
                buf.writeInt(level);
                buf.writeInt(pid);
                buf.writeBoolean(data2.isDead);
                buf.writeBoolean(entity != null);
                PacketHelper.sendString(buf, ownerName);
            }
        }
    }

    @SideOnly(value=Side.CLIENT)
    public static void processGetShipList(int[] data1, int[] data2, boolean[] data3, String[] data4) {
        EntityPlayer sender = ClientProxy.getClientPlayer();
        sender.sendMessage(new TextComponentTranslation("chat.shincolle:command.command").appendSibling(new TextComponentString(TextFormatting.DARK_GREEN + " ship: page ( " + TextFormatting.AQUA + data1[1] + TextFormatting.DARK_GREEN + " / " + data1[0] + " )")));
        if (data1[2] > 0) {
            for (int i = 0; i < data1[2]; ++i) {
                String shipClassName = I18n.format("entity." + ShipCalc.getEntityToSpawnName(data2[i * 8 + 2]) + ".name");
                sender.sendMessage(new TextComponentString("  UID: " + TextFormatting.AQUA + data2[i * 8] + TextFormatting.RESET + "  WID: " + TextFormatting.DARK_PURPLE + data2[i * 8 + 1] + TextFormatting.RESET + "  D/E: " + TextFormatting.RED + data3[i * 2] + TextFormatting.RESET + "/" + TextFormatting.LIGHT_PURPLE + data3[i * 2 + 1] + TextFormatting.RESET + "  Cls: " + TextFormatting.YELLOW + shipClassName + TextFormatting.RESET));
                sender.sendMessage(new TextComponentString("       Pos( " + TextFormatting.GRAY + data2[i * 8 + 3] + " " + data2[i * 8 + 4] + " " + data2[i * 8 + 5] + TextFormatting.RESET + " )  Lv: " + TextFormatting.GOLD + data2[i * 8 + 6] + TextFormatting.RESET + "  Owner: " + TextFormatting.GREEN + data2[i * 8 + 7] + " " + data4[i] + TextFormatting.RESET));
            }
        }
    }
}
