package com.lulan.shincolle.utility;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.entity.other.EntityProjectileStatic;
import com.lulan.shincolle.item.BasicEntityItem;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CGUIPackets;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.tileentity.TileEntityCrane;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.tileentity.TileEntityVolCore;
import com.lulan.shincolle.tileentity.TileMultiGrudgeHeavy;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class PacketHelper {
    private PacketHelper() {}

    public static void sendArrayByte(ByteBuf buf, byte[] data) {
        if (data != null) {
            buf.writeInt(data.length);
            for (byte value : data) {
                buf.writeByte(value);
            }
        } else {
            buf.writeInt(-1);
        }
    }

    public static void sendArrayInt(ByteBuf buf, int[] data) {
        if (data != null) {
            buf.writeInt(data.length);
            for (int value : data) {
                buf.writeInt(value);
            }
        } else {
            buf.writeInt(-1);
        }
    }

    public static void sendArrayFloat(ByteBuf buf, float[] data) {
        if (data != null) {
            buf.writeInt(data.length);
            for (float value : data) {
                buf.writeFloat(value);
            }
        } else {
            buf.writeInt(-1);
        }
    }

    public static void sendMapInt(ByteBuf buf, Map<Integer, Integer> data) {
        if (data != null) {
            Iterator<Map.Entry<Integer, Integer>> iter = data.entrySet().iterator();
            buf.writeInt(data.size());
            while (iter.hasNext()) {
                Map.Entry<Integer, Integer> entry = iter.next();
                buf.writeInt(entry.getKey());
                buf.writeInt(entry.getValue());
            }
        } else {
            buf.writeInt(-1);
        }
    }

    public static void sendListInt(ByteBuf buf, List<Integer> data) {
        if (data != null) {
            Iterator<Integer> iter = data.iterator();
            buf.writeInt(data.size());
            while (iter.hasNext()) {
                buf.writeInt(iter.next().intValue());
            }
        } else {
            buf.writeInt(-1);
        }
    }

    public static void sendListString(ByteBuf buf, List<String> data) {
        if (data != null) {
            Iterator<String> iter = data.iterator();
            buf.writeInt(data.size());
            while (iter.hasNext()) {
                String str = iter.next();
                if (str == null) {
                    ByteBufUtils.writeUTF8String(buf, "");
                    continue;
                }
                ByteBufUtils.writeUTF8String(buf, str);
            }
        } else {
            buf.writeInt(-1);
        }
    }

    public static Map<Integer, Integer> readMapInt(ByteBuf buf) {
        HashMap<Integer, Integer> getMap = new HashMap<>();
        int size = buf.readInt();
        if (size > 0) {
            for (int i = 0; i < size; ++i) {
                getMap.put(buf.readInt(), buf.readInt());
            }
        }
        return getMap;
    }

    public static List<Integer> readListInt(ByteBuf buf) {
        ArrayList<Integer> getlist = new ArrayList<>();
        int size = buf.readInt();
        if (size > 0) {
            for (int i = 0; i < size; ++i) {
                getlist.add(buf.readInt());
            }
        }
        return getlist;
    }

    public static List<String> readListString(ByteBuf buf) {
        ArrayList<String> getlist = new ArrayList<>();
        int size = buf.readInt();
        if (size > 0) {
            for (int i = 0; i < size; ++i) {
                getlist.add(ByteBufUtils.readUTF8String(buf));
            }
        }
        return getlist;
    }

    public static void sendString(ByteBuf buf, String str) {
        if (str != null) {
            buf.writeInt(1);
            ByteBufUtils.writeUTF8String(buf, str);
        } else {
            buf.writeInt(-1);
        }
    }

    public static String readString(ByteBuf buf) {
        String str = null;
        int flag = buf.readInt();
        if (flag > 0) {
            str = ByteBufUtils.readUTF8String(buf);
        }
        return str;
    }

    public static double[] readDoubleArray(ByteBuf buf, int length) {
        double[] array = new double[length];
        for (int i = 0; i < length; ++i) {
            array[i] = buf.readDouble();
        }
        return array;
    }

    public static int[] readIntArray(ByteBuf buf) {
        int length = buf.readInt();
        if (length > 0) {
            return PacketHelper.readIntArray(buf, length);
        }
        return new int[0];
    }

    public static int[] readIntArray(ByteBuf buf, int length) {
        int[] array = new int[length];
        for (int i = 0; i < length; ++i) {
            array[i] = buf.readInt();
        }
        return array;
    }

    public static float[] readFloatArray(ByteBuf buf) {
        int length = buf.readInt();
        if (length > 0) {
            return PacketHelper.readFloatArray(buf, length);
        }
        return new float[0];
    }

    public static float[] readFloatArray(ByteBuf buf, int length) {
        float[] array = new float[length];
        for (int i = 0; i < length; ++i) {
            array[i] = buf.readFloat();
        }
        return array;
    }

    public static boolean[] readBooleanArray(ByteBuf buf, int length) {
        boolean[] array = new boolean[length];
        for (int i = 0; i < length; ++i) {
            array[i] = buf.readBoolean();
        }
        return array;
    }

    public static byte[] readByteArray(ByteBuf buf) {
        int length = buf.readInt();
        if (length > 0) {
            return PacketHelper.readByteArray(buf, length);
        }
        return new byte[0];
    }

    public static byte[] readByteArray(ByteBuf buf, int length) {
        byte[] array = new byte[length];
        for (int i = 0; i < length; ++i) {
            array[i] = buf.readByte();
        }
        return array;
    }

    public static List<String> getStringListFromStringMap(Map map) {
        List<String> list = new ArrayList<>();
        if (map != null) {
            map.forEach((k, v) -> list.add((String)v));
        }
        return list;
    }

    public static void setEntityByGUI(BasicEntityShip entity, int button, int value) {
        if (entity != null) {
            switch (button) {
                case 0: {
                    entity.setStateFlagI(3, value);
                    break;
                }
                case 1: {
                    entity.setStateFlagI(4, value);
                    break;
                }
                case 2: {
                    entity.setStateFlagI(5, value);
                    break;
                }
                case 3: {
                    entity.setStateFlagI(6, value);
                    break;
                }
                case 4: {
                    entity.setStateFlagI(7, value);
                    break;
                }
                case 5: {
                    entity.setStateMinor(10, value);
                    if (entity.getStateMinor(10) < entity.getStateMinor(11)) break;
                    entity.setStateMinor(11, value + 1);
                    break;
                }
                case 6: {
                    entity.setStateMinor(11, value);
                    if (entity.getStateMinor(11) > entity.getStateMinor(10)) break;
                    entity.setStateMinor(10, value - 1);
                    break;
                }
                case 7: {
                    entity.setStateMinor(12, value);
                    break;
                }
                case 8: {
                    entity.setStateFlagI(21, value);
                    break;
                }
                case 9: {
                    entity.setStateFlagI(9, value);
                    break;
                }
                case 10: {
                    entity.setStateFlagI(12, value);
                    break;
                }
                case 11: {
                    entity.setStateFlagI(18, value);
                    break;
                }
                case 12: {
                    entity.setStateFlagI(19, value);
                    break;
                }
                case 13: {
                    entity.setStateFlagI(20, value);
                    break;
                }
                case 14: {
                    entity.setStateFlagI(22, value);
                    break;
                }
                case 15: {
                    entity.getCapaShipInventory().setInventoryPage(value);
                    break;
                }
                case 16: {
                    entity.setStateFlagI(23, value);
                    break;
                }
                case 17: {
                    entity.setStateMinor(44, value);
                    break;
                }
                case 18: {
                    entity.setStateFlagI(25, value);
                    break;
                }
                case 19: {
                    entity.setStateMinor(9, value);
                    break;
                }
                case 20: {
                    entity.setStateFlagI(26, value);
                    break;
                }
                case 21: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[0], false);
                    break;
                }
                case 22: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[1], false);
                    break;
                }
                case 23: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[2], false);
                    break;
                }
                case 24: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[3], false);
                    break;
                }
                case 25: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[4], false);
                    break;
                }
                case 26: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[5], false);
                    break;
                }
                case 27: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[6], false);
                    break;
                }
                case 28: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[7], false);
                    break;
                }
                case 29: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[8], false);
                    break;
                }
                case 30: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[9], false);
                    break;
                }
                case 31: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[10], false);
                    break;
                }
                case 32: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[11], false);
                    break;
                }
                case 33: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[12], false);
                    break;
                }
                case 34: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[13], false);
                    break;
                }
                case 35: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[14], false);
                    break;
                }
                case 36: {
                    entity.setStateEmotion(0, entity.getStateEmotion(0) ^ Values.N.Pow2[15], false);
                    break;
                }
                case 37: {
                    entity.setStateMinor(40, value);
                    break;
                }
                case 38: {
                    entity.setStateMinor(41, value);
                    break;
                }
                case 39: {
                    entity.setStateFlagI(2, value);
                    break;
                }
                case 40: {
                    entity.setSitting(!entity.isSitting());
                    entity.sendSyncPacketEmotion();
                    break;
                }
                case 41: {
                    entity.setStateEmotion(1, value, true);
                    break;
                }
                case 42: {
                    entity.setStateEmotion(7, value, true);
                    break;
                }
                default:
            }
        } else {
            LogHelper.debug("DEBUG: set entity by GUI fail, entity null");
        }
    }

    public static void setEntityByCustomData(Entity entity, float[] value) {
        if (value == null || value.length <= 0) {
            return;
        }
        switch ((int)value[0]) {
            case 0: {
                EntityAbyssMissile ent = (EntityAbyssMissile)entity;
                ent.setProjectileType((int)value[1]);
                ent.moveType = EntityAbyssMissile.MoveType.fromId((int)value[2]);
                ent.velX = value[3];
                ent.velY = value[4];
                ent.velZ = value[5];
                ent.accY1 = value[7];
                ent.accY2 = value[8];
                ent.invisibleTicks = (int)value[9];
                break;
            }
            case 1: {
                EntityProjectileStatic ent = (EntityProjectileStatic)entity;
                ent.setProjectileType((int)value[1]);
                ent.lifeLength = (int)value[2];
                ent.pullForce = value[3];
                ent.range = value[4];
                break;
            }
            case 2: {
                EntityAbyssMissile ent = (EntityAbyssMissile)entity;
                ent.setProjectileType((int)value[1]);
                ent.moveType = EntityAbyssMissile.MoveType.fromId((int)value[2]);
                ent.velX = value[3];
                ent.velY = value[4];
                ent.velZ = value[5];
                ent.startMove = value[6] > 0.0f;
                ent.accY1 = value[8];
                ent.accY2 = value[9];
                break;
            }
            default:
        }
    }

    public static void setTileEntityByGUI(TileEntity tile, int button, int value, int value2) {
        if (tile instanceof TileEntitySmallShipyard) {
            TileEntitySmallShipyard tile2 = (TileEntitySmallShipyard)tile;
            tile2.setBuildType(value);
            if (value == 4 || value == 3) {
                int[] getMat = new int[]{0, 0, 0, 0};
                for (int i = 0; i < 4; ++i) {
                    if (tile2.getStackInSlot(i).isEmpty()) continue;
                    getMat[i] = tile2.getStackInSlot(i).getCount();
                }
                tile2.setBuildRecord(getMat);
            }
            return;
        }
        if (tile instanceof TileMultiGrudgeHeavy) {
            TileMultiGrudgeHeavy tile2 = (TileMultiGrudgeHeavy)tile;
            switch (button) {
                case 0: {
                    tile2.setBuildType(value);
                    break;
                }
                case 1: {
                    tile2.setInvMode(value);
                    break;
                }
                case 2: {
                    tile2.setSelectMat(value);
                    break;
                }
                case 3: {
                    TileEntityHelper.setLargeShipyardBuildMats((TileMultiGrudgeHeavy)tile, value, value2);
                    break;
                }
                default:
            }
        } else if (tile instanceof TileEntityCrane) {
            TileEntityCrane tile2 = (TileEntityCrane)tile;
            switch (button) {
                case 4: {
                    tile2.setField(6, value);
                    break;
                }
                case 5: {
                    tile2.setField(7, value);
                    break;
                }
                case 0: {
                    tile2.setField(2, value);
                    if (value != 0) break;
                    tile2.setShip(null);
                    break;
                }
                case 2: {
                    tile2.setField(3, value);
                    break;
                }
                case 3: {
                    tile2.setField(4, value);
                    break;
                }
                case 1: {
                    tile2.setField(5, value);
                    break;
                }
                case 6: {
                    tile2.setField(8, value);
                    break;
                }
                case 7: {
                    if (value > 2) {
                        value = 0;
                    }
                    tile2.setField(10, value);
                    break;
                }
                case 8: {
                    if (value > 2) {
                        value = 0;
                    }
                    tile2.setField(12, value);
                    break;
                }
                case 9: {
                    if (value > 2) {
                        value = 0;
                    }
                    tile2.setField(13, value);
                    break;
                }
                default:
            }
        } else if (tile instanceof TileEntityVolCore) {
            TileEntityVolCore tile2 = (TileEntityVolCore)tile;
            if(button == 0){
                tile2.setField(0, value);
            }
        } else {
            LogHelper.debug("DEBUG: set tile entity by GUI fail: tile: " + tile);
        }
    }

    public static void sendS2CEntitySync(int type, Object host, World world, BlockPos targetPos, EntityPlayer targetPlayer) {
        if (!world.isRemote) {
            NetworkRegistry.TargetPoint point = null;
            if (targetPos != null) {
                point = new NetworkRegistry.TargetPoint(world.provider.getDimension(), targetPos.getX(), targetPos.getY(), targetPos.getZ(), 64.0);
            }
            if(type == 0){
                if (point != null) {
                    CommonProxy.channelE.sendToAllAround(new S2CEntitySync(host, (byte)51), point);
                }
                if (targetPlayer == null) return;
                CommonProxy.channelE.sendTo(new S2CEntitySync(host, (byte)51), (EntityPlayerMP)targetPlayer);
            }
        }
    }

    public static void sendItemStack(ByteBuf buf, ItemStack stack) {
        if (stack == null) {
            buf.writeShort(-1);
        } else {
            buf.writeShort(Item.getIdFromItem(stack.getItem()));
            buf.writeByte(stack.getCount());
            buf.writeShort(stack.getMetadata());
            NBTTagCompound nbttagcompound = null;
            if (stack.getItem().isDamageable() || stack.getItem().getShareTag()) {
                nbttagcompound = stack.getItem().getNBTShareTag(stack);
            }
            PacketHelper.writeCompoundTag(buf, nbttagcompound);
        }
    }

    public static ItemStack readItemStack(ByteBuf buf) {
        ItemStack itemstack = ItemStack.EMPTY;
        short i = buf.readShort();
        if (i >= 0) {
            byte j = buf.readByte();
            short k = buf.readShort();
            itemstack = new ItemStack(Item.getItemById(i), j, k);
            itemstack.setTagCompound(PacketHelper.readCompoundTag(buf));
        }
        return itemstack;
    }

    public static void writeCompoundTag(ByteBuf buf, @Nullable NBTTagCompound nbt) {
        if (nbt == null) {
            buf.writeByte(0);
        } else {
            try {
                CompressedStreamTools.write(nbt, new ByteBufOutputStream(buf));
            }
            catch (IOException e) {
                LogHelper.info("EXCEPTION: NBT packet: send data fail: " + e);
                e.printStackTrace();
            }
        }
    }

    @Nullable
    public static NBTTagCompound readCompoundTag(ByteBuf buf) {
        int i = buf.readerIndex();
        byte b0 = buf.readByte();
        if (b0 == 0) {
            return null;
        }
        buf.readerIndex(i);
        try {
            return CompressedStreamTools.read(new ByteBufInputStream(buf), new NBTSizeTracker(0x200000L));
        }
        catch (IOException e) {
            LogHelper.info("EXCEPTION: NBT packet: get data fail: " + e);
            e.printStackTrace();
            return null;
        }
    }

    public static void syncEntityItemListToClient(EntityPlayer player) {
        if (player.world != null) {
            float[] data;
            List getlist = player.world.getEntitiesWithinAABB(BasicEntityItem.class, player.getEntityBoundingBox().expand(128.0, 256.0, 128.0));
            if (!getlist.isEmpty()) {
                data = new float[getlist.size() * 3];
                BasicEntityItem ei = null;
                for (int i = 0; i < getlist.size(); ++i) {
                    ei = (BasicEntityItem) getlist.get(i);
                    data[i * 3] = (float)ei.posX;
                    data[i * 3 + 1] = (float)ei.posY;
                    data[i * 3 + 2] = (float)ei.posZ;
                }
            } else {
                data = new float[]{0.0f};
            }
            CommonProxy.channelG.sendTo(new S2CGUIPackets((byte)102, data), (EntityPlayerMP)player);
        }
    }
}
