package com.lulan.shincolle.utility;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class EnchantHelper {

    private EnchantHelper() {}

    private static final Random RAND = new Random();
    private static final List<int[]> EnchantTable = new ArrayList<>();

    public static float[] calcEnchantEffect(final ItemStack stack) {
        final float[] ench = new float[21];
        if (stack.isEmpty()) return ench;
        final NBTTagList nbttaglist = stack.getEnchantmentTagList();
        if (nbttaglist != null) {
            for (int i = 0; i < nbttaglist.tagCount(); ++i) {
                final NBTTagCompound tag = nbttaglist.getCompoundTagAt(i);
                final short id = tag.getShort("id");
                final short lv = tag.getShort("lvl");
                switch (id) {
                    case 0: ench[0] += 0.1F * lv; break;
                    case 1: case 4: ench[0] += 0.05F * lv; break;
                    case 2: ench[7] += 0.1F * lv; ench[20] -= 0.1F * lv; break;
                    case 3: ench[0] += 0.05F * lv; ench[20] += 0.1F * lv; break;
                    case 5: ench[14] += 0.15F * lv; break;
                    case 6: case 8: ench[7] += 0.05F * lv; ench[15] += 0.25F * lv; break;
                    case 7: ench[13] += 0.15F * lv; break;
                    case 9: ench[9] += 0.25F * lv; break;
                    case 16: case 48: ench[1] += 0.08F * lv; break;
                    case 17: case 18: ench[1] += 0.08F * lv; break;
                    case 19: case 49: ench[8] += 0.15F * lv; ench[20] += 0.05F * lv; break;
                    case 20: case 50: ench[10] += 0.25F * lv; ench[11] += 0.25F * lv; break;
                    case 21: case 35: case 61: ench[16] += 0.25F * lv; break;
                    case 32: ench[6] += 0.1F * lv; break;
                    case 33: ench[17] += 0.25F * lv; break;
                    case 34: ench[5] += 0.2F * lv; break;
                    case 51: ench[18] += 0.25F * lv; break;
                    case 62: ench[12] += 0.25F * lv; break;
                    case 70: ench[19] += 0.5F * lv; break;
                    default:
                        if (Enchantment.getEnchantmentByID(id) != null) {
                            for (int j = 0; j < ench.length; j++) {
                                ench[j] += 0.01F * lv;
                            }
                        }
                }
            }
        }
        return ench;
    }

    public static int calcEnchantNumber(final ItemStack stack) {
        int number = 0;
        if (stack.isEmpty()) return 0;
        final NBTTagList nbttaglist = stack.getEnchantmentTagList();
        if (nbttaglist != null) {
            for (int i = 0; i < nbttaglist.tagCount(); ++i) {
                final NBTTagCompound tag = nbttaglist.getCompoundTagAt(i);
                number += tag.getShort("lvl");
            }
        }
        return number;
    }

    public static void applyRandomEnchantToEquip(final ItemStack stack, final int enchType, final int enchLv) {
        if (stack.isEmpty() || enchLv <= 0 || enchType < 0 || enchType >= EnchantTable.size()) return;
        int enchNum = 0;
        final int ranNum = RAND.nextInt(10);
        if (enchLv == 1) {
            enchNum = ranNum > 5 ? 1 : 0;
        } else if (enchLv == 2) {
            enchNum = ranNum > 6 ? 2 : (ranNum > 3 ? 1 : 0);
        }
        if (enchNum <= 0) return;
        final int[] enchs = EnchantTable.get(enchType);
        final HashMap<Enchantment, Integer> enchmap = new HashMap<>();
        for (int i = 0; i < enchNum; ++i) {
            final Enchantment ench = Enchantment.getEnchantmentByID(enchs[RAND.nextInt(enchs.length)]);
            if (ench != null) {
                if (enchmap.containsKey(ench)) {
                    int lv = enchmap.get(ench) + 1;
                    if (lv > ench.getMaxLevel()) lv = ench.getMaxLevel();
                    enchmap.replace(ench, lv);
                } else {
                    enchmap.put(ench, 1);
                }
            }
        }
        if (!enchmap.isEmpty()) {
            EnchantmentHelper.setEnchantments(enchmap, stack);
        }
    }

    static {
        EnchantTable.add(new int[]{5, 7, 9, 16, 17, 18, 19, 20, 21, 32, 35, 48, 49, 50, 51, 61});
        EnchantTable.add(new int[]{0, 1, 3, 4, 33, 34, 70});
        EnchantTable.add(new int[]{2, 6, 8, 33, 62, 70});
    }
}