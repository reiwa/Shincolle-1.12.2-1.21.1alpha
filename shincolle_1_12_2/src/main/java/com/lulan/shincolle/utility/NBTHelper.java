package com.lulan.shincolle.utility;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.ArrayList;
import java.util.List;

public class NBTHelper {
    private NBTHelper() {}

    public static void saveIntListToNBT(NBTTagCompound save, String tagName, List<Integer> ilist) {
        if (save != null) {
            if (ilist != null && !ilist.isEmpty()) {
                int[] intary = CalcHelper.intListToArray(ilist);
                save.setIntArray(tagName, intary);
            } else {
                save.setIntArray(tagName, new int[0]);
            }
        } else {
            LogHelper.debug("DEBUG: NBT helper: save nbt fail: tag is null ");
        }
    }

    public static List<String> loadStringTagArrayList(NBTTagCompound nbt, String tagName) {
        NBTTagList nameTags = nbt.getTagList(tagName, 8);
        List<String> nameList = new ArrayList<>();
        for (int i = 0; i < nameTags.tagCount(); ++i) {
            String str = nameTags.getStringTagAt(i);
            if (str.isEmpty()) continue;
            nameList.add(str);
        }
        return nameList;
    }

    public static NBTTagCompound saveStringTagArrayList(NBTTagCompound nbt, String tagName, List<String> strs) {
        if (strs != null) {
            NBTTagList tagList = new NBTTagList();
            for (String name : strs) {
                if (name == null || name.isEmpty()) {
                    name = " ";
                }
                tagList.appendTag(new NBTTagString(name));
            }
            nbt.setTag(tagName, tagList);
        }
        return nbt;
    }
}
