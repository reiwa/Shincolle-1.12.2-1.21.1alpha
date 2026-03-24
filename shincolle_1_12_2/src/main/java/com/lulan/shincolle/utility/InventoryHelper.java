package com.lulan.shincolle.utility;

import baubles.api.cap.BaublesCapabilities;
import baubles.api.cap.IBaublesItemHandler;
import com.lulan.shincolle.capability.CapaShipInventory;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.Values;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.oredict.OreDictionary;

import java.util.HashSet;

public class InventoryHelper {

    private InventoryHelper() {}

    public static boolean checkInventoryAmount(IInventory inv, ItemStack[] tempStacks, boolean[] modeStacks, boolean checkMetadata, boolean checkNbt, boolean checkOredict, boolean excess) {
        if (inv == null || tempStacks == null || tempStacks.length != 9) {
            return true;
        }
        boolean hasTemplate = false;
        for (ItemStack stack : tempStacks) {
            if (!stack.isEmpty()) {
                hasTemplate = true;
                break;
            }
        }
        if (!hasTemplate) {
            return true;
        }
        for (int i = 0; i < 9; ++i) {
            if (!tempStacks[i].isEmpty() && !modeStacks[i]) {
                int currentAmount = calcItemStackAmount(inv, tempStacks[i], checkMetadata, checkNbt, checkOredict);
                if (excess) {
                    if (currentAmount < tempStacks[i].getCount()) {
                        return false;
                    }
                } else {
                    if (currentAmount > tempStacks[i].getCount()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean checkFluidContainer(IInventory inv, FluidStack targetFluid, boolean checkFull) {
        if (inv == null) {
            return true;
        }
        if (inv instanceof CapaShipInventory) {
            CapaShipInventory shipInv = (CapaShipInventory) inv;
            for (int i = 6; i < shipInv.getSizeInventoryPaged(); ++i) {
                if (!checkFluidContainer(shipInv.getStackInSlotWithPageCheck(i), targetFluid, checkFull)) {
                    return false;
                }
            }
        } else if (inv instanceof TileEntityChest) {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                if (!checkFluidContainer(inv.getStackInSlot(i), targetFluid, checkFull)) {
                    return false;
                }
            }
            TileEntityChest chest2 = TileEntityHelper.getAdjChest((TileEntityChest) inv);
            if (chest2 != null) {
                for (int i = 0; i < chest2.getSizeInventory(); ++i) {
                    if (!checkFluidContainer(chest2.getStackInSlot(i), targetFluid, checkFull)) {
                        return false;
                    }
                }
            }
        } else {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                if (!checkFluidContainer(inv.getStackInSlot(i), targetFluid, checkFull)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean checkFluidContainer(ItemStack stack, FluidStack targetFluid, boolean checkFull) {
        if (!stack.isEmpty() && stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
            IFluidHandlerItem fh = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
            if (fh != null) {
                IFluidTankProperties[] tanks = fh.getTankProperties();
                if (tanks != null) {
                    for (IFluidTankProperties tank : tanks) {
                        FluidStack fstack = tank.getContents();
                        if (checkFull) {
                            if (tank.canFill() && (fstack == null || (fstack.amount < tank.getCapacity() && (targetFluid == null || targetFluid.isFluidEqual(fstack))))) {
                                return false;
                            }
                        } else {
                            if (tank.canDrain() && fstack != null && fstack.amount > 0) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public static boolean checkInventoryFull(IInventory inv) {
        if (inv == null) {
            return true;
        }
        if (inv instanceof CapaShipInventory) {
            CapaShipInventory shipInv = (CapaShipInventory) inv;
            for (int i = 6; i < shipInv.getSizeInventoryPaged(); ++i) {
                if (shipInv.getStackInSlotWithPageCheck(i).isEmpty()) {
                    return false;
                }
            }
        } else if (inv instanceof TileEntityChest) {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                if (inv.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            TileEntityChest chest2 = TileEntityHelper.getAdjChest((TileEntityChest) inv);
            if (chest2 != null) {
                for (int i = 0; i < chest2.getSizeInventory(); ++i) {
                    if (chest2.getStackInSlot(i).isEmpty()) {
                        return false;
                    }
                }
            }
        } else {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                if (inv.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean checkInventoryEmpty(IInventory inv, ItemStack[] tempStacks, boolean[] modeStacks, boolean checkMetadata, boolean checkNbt, boolean checkOredict) {
        if (inv == null) {
            return true;
        }
        boolean noTempItem = true;
        if (tempStacks != null && tempStacks.length == 9 && modeStacks != null && modeStacks.length == 9) {
            for (int i = 0; i < 9; ++i) {
                if (!tempStacks[i].isEmpty()) {
                    noTempItem = false;
                    if (!modeStacks[i] && matchTargetItem(inv, tempStacks[i], checkMetadata, checkNbt, checkOredict)) {
                        return false;
                    }
                }
            }
        }
        if (noTempItem) {
            return isAllSlotNull(inv);
        }
        return true;
    }

    public static boolean isAllSlotNull(IInventory inv) {
        if (inv instanceof CapaShipInventory) {
            CapaShipInventory shipInv = (CapaShipInventory) inv;
            for (int i = 6; i < shipInv.getSizeInventoryPaged(); ++i) {
                if (!shipInv.getStackInSlotWithPageCheck(i).isEmpty()) {
                    return false;
                }
            }
        } else if (inv instanceof TileEntityChest) {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                if (!inv.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
            TileEntityChest chest2 = TileEntityHelper.getAdjChest((TileEntityChest) inv);
            if (chest2 != null) {
                for (int i = 0; i < chest2.getSizeInventory(); ++i) {
                    if (!chest2.getStackInSlot(i).isEmpty()) {
                        return false;
                    }
                }
            }
        } else {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                if (!inv.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static int calcItemStackAmount(IInventory inv, ItemStack temp, boolean checkMetadata, boolean checkNbt, boolean checkOredict) {
        int targetAmount = 0;
        if (inv instanceof CapaShipInventory) {
            CapaShipInventory shipInv = (CapaShipInventory) inv;
            for (int i = 6; i < shipInv.getSizeInventoryPaged(); ++i) {
                if (matchTargetItem(shipInv.getStackInSlotWithPageCheck(i), temp, checkMetadata, checkNbt, checkOredict)) {
                    targetAmount += shipInv.getStackInSlotWithPageCheck(i).getCount();
                }
            }
        } else if (inv instanceof TileEntityChest) {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                if (matchTargetItem(inv.getStackInSlot(i), temp, checkMetadata, checkNbt, checkOredict)) {
                    targetAmount += inv.getStackInSlot(i).getCount();
                }
            }
            TileEntityChest chest2 = TileEntityHelper.getAdjChest((TileEntityChest) inv);
            if (chest2 != null) {
                for (int i = 0; i < chest2.getSizeInventory(); ++i) {
                    if (matchTargetItem(chest2.getStackInSlot(i), temp, checkMetadata, checkNbt, checkOredict)) {
                        targetAmount += chest2.getStackInSlot(i).getCount();
                    }
                }
            }
        } else {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                if (matchTargetItem(inv.getStackInSlot(i), temp, checkMetadata, checkNbt, checkOredict)) {
                    targetAmount += inv.getStackInSlot(i).getCount();
                }
            }
        }
        return targetAmount;
    }

    public static boolean matchTargetItem(IInventory inv, ItemStack temp, boolean checkMetadata, boolean checkNbt, boolean checkOredict) {
        if (inv instanceof CapaShipInventory) {
            CapaShipInventory shipInv = (CapaShipInventory) inv;
            for (int i = 6; i < shipInv.getSizeInventoryPaged(); ++i) {
                if (matchTargetItem(shipInv.getStackInSlotWithPageCheck(i), temp, checkMetadata, checkNbt, checkOredict)) {
                    return true;
                }
            }
        } else if (inv instanceof TileEntityChest) {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                if (matchTargetItem(inv.getStackInSlot(i), temp, checkMetadata, checkNbt, checkOredict)) {
                    return true;
                }
            }
            TileEntityChest chest2 = TileEntityHelper.getAdjChest((TileEntityChest) inv);
            if (chest2 != null) {
                for (int i = 0; i < chest2.getSizeInventory(); ++i) {
                    if (matchTargetItem(chest2.getStackInSlot(i), temp, checkMetadata, checkNbt, checkOredict)) {
                        return true;
                    }
                }
            }
        } else {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                if (matchTargetItem(inv.getStackInSlot(i), temp, checkMetadata, checkNbt, checkOredict)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int matchTargetItemExceptSlots(IInventory inv, ItemStack temp, boolean checkMetadata, boolean checkNbt, boolean checkOredict, int[] exceptSlots) {
        if (temp.isEmpty()) {
            return -1;
        }
        if (inv instanceof CapaShipInventory) {
            CapaShipInventory shipInv = (CapaShipInventory) inv;
            for (int i = 6; i < shipInv.getSizeInventoryPaged(); ++i) {
                if (CalcHelper.checkIntNotInArray(i, exceptSlots) && matchTargetItem(shipInv.getStackInSlotWithPageCheck(i), temp, checkMetadata, checkNbt, checkOredict)) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                if (CalcHelper.checkIntNotInArray(i, exceptSlots) && matchTargetItem(inv.getStackInSlot(i), temp, checkMetadata, checkNbt, checkOredict)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static ItemStack getAndRemoveItem(IInventory inv, ItemStack temp, int number, boolean checkMetadata, boolean checkNbt, boolean checkOredict, int[] exceptSlots) {
        if (temp.isEmpty() || number <= 0) {
            return ItemStack.EMPTY;
        }
        int numToGet = Math.min(number, 64);
        ItemStack resultStack = ItemStack.EMPTY;
        boolean lockedOnItem = false;
        java.util.List<IInventory> invs = new java.util.ArrayList<>();
        invs.add(inv);
        if (inv instanceof TileEntityChest) {
            TileEntityChest adj = TileEntityHelper.getAdjChest((TileEntityChest) inv);
            if (adj != null) {
                invs.add(adj);
            }
        }
        while (numToGet > 0) {
            int slot = -1;
            IInventory currentInv = null;
            ItemStack stackInSlot = ItemStack.EMPTY;
            for (IInventory i : invs) {
                slot = matchTargetItemExceptSlots(i, lockedOnItem ? resultStack : temp, lockedOnItem || checkMetadata, lockedOnItem || checkNbt, !lockedOnItem && checkOredict, exceptSlots);
                if (slot != -1) {
                    currentInv = i;
                    if (currentInv instanceof CapaShipInventory) {
                        stackInSlot = ((CapaShipInventory) currentInv).getStackInSlotWithPageCheck(slot);
                    } else {
                        stackInSlot = currentInv.getStackInSlot(slot);
                    }
                    break;
                }
            }
            if (slot == -1 || currentInv == null || stackInSlot.isEmpty()) {
                break;
            }
            if (!lockedOnItem) {
                resultStack = stackInSlot.copy();
                resultStack.setCount(0);
                lockedOnItem = true;
            }
            int takeAmount = Math.min(numToGet, stackInSlot.getCount());
            stackInSlot.shrink(takeAmount);
            resultStack.grow(takeAmount);
            numToGet -= takeAmount;
            if (currentInv instanceof CapaShipInventory) {
                ((CapaShipInventory) currentInv).setInventorySlotWithPageCheck(slot, stackInSlot);
            } else {
                currentInv.setInventorySlotContents(slot, stackInSlot);
            }
        }
        return resultStack;
    }

    public static boolean matchTargetItem(ItemStack target, ItemStack temp, boolean checkMetadata, boolean checkNbt, boolean checkOredict) {
        if (temp.isEmpty() || target.isEmpty()) {
            return false;
        }
        if (target.getItem() == temp.getItem()) {
            if (checkMetadata && target.getItemDamage() != temp.getItemDamage()) {
                return false;
            }
            return !checkNbt || ItemStack.areItemStackTagsEqual(target, temp);
        }
        if (checkOredict) {
            int[] oreIDsTarget = OreDictionary.getOreIDs(target);
            if (oreIDsTarget.length > 0) {
                int[] oreIDsTemp = OreDictionary.getOreIDs(temp);
                if (oreIDsTemp.length > 0) {
                    for (int id1 : oreIDsTarget) {
                        for (int id2 : oreIDsTemp) {
                            if (id1 == id2) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean getItemMode(int slotID, int stackMode) {
        return ((stackMode >> slotID) & 1) == 1;
    }

    public static int setItemMode(int slotID, int stackMode, boolean notMode) {
        int slot = 1 << slotID;
        if (notMode) {
            stackMode |= slot;
        } else {
            stackMode &= ~slot;
        }
        return stackMode;
    }

    public static int[] getSlotsFromSide(ISidedInventory te, ItemStack stack, int side, int type) {
        if (te == null) {
            return new int[0];
        }
        HashSet<Integer> slots = new HashSet<>();
        int padbit = type * 6;
        for (int i = 0; i < 6; ++i) {
            int tarbit = i + padbit;
            if ((side & Values.N.Pow2[tarbit]) == Values.N.Pow2[tarbit]) {
                EnumFacing face = EnumFacing.getFront(i);
                int[] slotsForFace = te.getSlotsForFace(face);
                if (slotsForFace != null) {
                    for (int s : slotsForFace) {
                        boolean canInsert = type != 1 && te.canInsertItem(s, stack, face);
                        boolean canExtract = type == 1 && te.canExtractItem(s, stack, face);
                        if (canInsert || canExtract) {
                            slots.add(s);
                        }
                    }
                }
            }
        }
        if (!slots.isEmpty()) {
            return CalcHelper.intSetToArray(slots);
        }
        return new int[0];
    }

    public static boolean moveItemstackToInv(IInventory inv, ItemStack moveitem, int[] toSlots) {
        if (moveitem.isEmpty()) {
            return false;
        }
        if (inv instanceof CapaShipInventory) {
            return mergeItemStack(inv, moveitem, toSlots);
        } else if (inv instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) inv;
            if (mergeItemStack(chest, moveitem, toSlots)) {
                return true;
            }
            TileEntityChest chest2 = TileEntityHelper.getAdjChest(chest);
            if (chest2 != null) {
                return mergeItemStack(chest2, moveitem, toSlots);
            }
            return false;
        } else {
            return mergeItemStack(inv, moveitem, toSlots);
        }
    }

    public static boolean mergeItemStack(IInventory inv, ItemStack itemstack, int[] slots) {
        if (itemstack.isEmpty()) return false;
        boolean movedItem = false;
        int start = 0;
        int end;
        boolean useSlots = slots != null;
        if (useSlots) {
            end = slots.length;
        } else if (inv instanceof CapaShipInventory) {
            start = 6;
            end = ((CapaShipInventory) inv).getSizeInventoryPaged();
        } else {
            end = inv.getSizeInventory();
        }
        if (itemstack.isStackable()) {
            for (int i = start; i < end && !itemstack.isEmpty(); ++i) {
                int slot = useSlots ? slots[i] : i;
                ItemStack slotstack = (inv instanceof CapaShipInventory) ? ((CapaShipInventory) inv).getStackInSlotWithPageCheck(slot) : inv.getStackInSlot(slot);
                if (!slotstack.isEmpty() && slotstack.getItem() == itemstack.getItem() && (!itemstack.getHasSubtypes() || itemstack.getMetadata() == slotstack.getMetadata()) && ItemStack.areItemStackTagsEqual(itemstack, slotstack)) {
                    int total = slotstack.getCount() + itemstack.getCount();
                    int maxStack = itemstack.getMaxStackSize();
                    if (total <= maxStack) {
                        itemstack.setCount(0);
                        slotstack.setCount(total);
                        movedItem = true;
                    } else if (slotstack.getCount() < maxStack) {
                        itemstack.shrink(maxStack - slotstack.getCount());
                        slotstack.setCount(maxStack);
                        movedItem = true;
                    }
                }
            }
        }
        if (!itemstack.isEmpty()) {
            for (int i = start; i < end && !itemstack.isEmpty(); ++i) {
                int slot = useSlots ? slots[i] : i;
                ItemStack slotstack = (inv instanceof CapaShipInventory) ? ((CapaShipInventory) inv).getStackInSlotWithPageCheck(slot) : inv.getStackInSlot(slot);
                if (slotstack.isEmpty() && inv.isItemValidForSlot(slot, itemstack)) {
                    if (inv instanceof CapaShipInventory) {
                        ((CapaShipInventory) inv).setInventorySlotWithPageCheck(slot, itemstack.copy());
                    } else {
                        inv.setInventorySlotContents(slot, itemstack.copy());
                    }
                    itemstack.setCount(0);
                    movedItem = true;
                }
            }
        }
        return movedItem;
    }

    @Optional.Method(modid = "Baubles")
    public static boolean checkRingInBaubles(EntityPlayer player) {
        IBaublesItemHandler bb = player.getCapability(BaublesCapabilities.CAPABILITY_BAUBLES, null);
        if (bb != null) {
            for (int i = 0; i < bb.getSlots(); ++i) {
                ItemStack stack = bb.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() == ModItems.MarriageRing) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean checkItemInShipInventory(CapaShipInventory inv, Item item, int meta, int minSlot, int maxSlot) {
        if (inv != null) {
            for (int i = minSlot; i < maxSlot; ++i) {
                ItemStack stack = inv.getStackInSlotWithPageCheck(i);
                if (!stack.isEmpty() && stack.getItem() == item && stack.getMetadata() == meta) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean tryFillContainer(CapaShipInventory inv, FluidStack fs) {
        if (inv == null || fs == null || fs.amount <= 0) {
            return false;
        }
        int amountMovedTotal = 0;
        FluidStack fluidToFill = fs.copy();
        for (int i = 6; i < inv.getSizeInventoryPaged() && fluidToFill.amount > 0; ++i) {
            ItemStack sourceStack = inv.getStackInSlotWithPageCheck(i);
            if (sourceStack.isEmpty()) {
                continue;
            }
            ItemStack singleItemToProcess = sourceStack.copy();
            singleItemToProcess.setCount(1);
            IFluidHandlerItem handler = FluidUtil.getFluidHandler(singleItemToProcess);
            if (handler != null && handler.fill(fluidToFill, false) > 0) {
                int filledAmount = handler.fill(fluidToFill, true);
                if (filledAmount <= 0) continue;
                ItemStack filledContainer = handler.getContainer();
                sourceStack.shrink(1);
                amountMovedTotal += filledAmount;
                fluidToFill.amount -= filledAmount;
                InventoryHelper.mergeItemStack(inv, filledContainer, null);
                if (!filledContainer.isEmpty()) {
                    sourceStack.grow(1);
                    amountMovedTotal -= filledAmount;
                    fluidToFill.amount += filledAmount;
                    continue;
                }
                i--;
            }
        }
        if (amountMovedTotal > 0 && inv.getHost() != null) {
            fs.amount -= amountMovedTotal;
            if (inv.getHost().getShipType() == 7) {
                inv.getHost().addShipExp(ConfigHandler.expGain[6]);
            }
            NetworkRegistry.TargetPoint tp = new NetworkRegistry.TargetPoint(inv.getHost().dimension, inv.getHost().posX, inv.getHost().posY, inv.getHost().posZ, 48.0);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(inv.getHost(), 21, 0.0, 0.1, 0.0), tp);
            return true;
        }
        return false;
    }

    public static FluidStack tryDrainContainer(CapaShipInventory inv, FluidStack targetFluid, int maxDrain) {
        if (inv == null || maxDrain <= 0) {
            return null;
        }
        FluidStack drainTotal = null;
        int remainingDrain = maxDrain;
        for (int i = 6; i < inv.getSizeInventoryPaged() && remainingDrain > 0; ++i) {
            ItemStack sourceStack = inv.getStackInSlotWithPageCheck(i);
            if (sourceStack.isEmpty()) {
                continue;
            }
            ItemStack singleItemToProcess = sourceStack.copy();
            singleItemToProcess.setCount(1);
            IFluidHandlerItem handler = FluidUtil.getFluidHandler(singleItemToProcess);
            if (handler != null) {
                FluidStack drained;
                if (targetFluid != null) {
                    FluidStack toDrain = targetFluid.copy();
                    toDrain.amount = remainingDrain;
                    drained = handler.drain(toDrain, false);
                } else {
                    drained = handler.drain(remainingDrain, false);
                }
                if (drained != null && drained.amount > 0) {
                    handler.drain(drained, true);
                    ItemStack emptyContainer = handler.getContainer();
                    sourceStack.shrink(1);
                    if (drainTotal == null) {
                        drainTotal = drained.copy();
                        if (targetFluid == null) {
                            targetFluid = drained.copy();
                        }
                    } else {
                        drainTotal.amount += drained.amount;
                    }
                    remainingDrain -= drained.amount;
                    InventoryHelper.mergeItemStack(inv, emptyContainer, null);
                    if (!emptyContainer.isEmpty()) {
                        sourceStack.grow(1);
                        drainTotal.amount -= drained.amount;
                        remainingDrain += drained.amount;
                        continue;
                    }
                    i--;
                }
            }
        }
        return drainTotal;
    }

    public static boolean moveItemstackToInv(Entity host, IInventory inv, ItemStack moveitem) {
        if (host == null || host.world == null || inv == null || moveitem.isEmpty()) {
            return false;
        }
        moveItemstackToInv(inv, moveitem, null);
        if (!moveitem.isEmpty()) {
            dropItemOnGround(host, moveitem);
        }
        return true;
    }

    public static void dropItemOnGround(Entity host, ItemStack stack) {
        if (!stack.isEmpty()) {
            EntityItem entityitem = new EntityItem(host.world, host.posX, host.posY, host.posZ, stack);
            entityitem.motionX = host.world.rand.nextGaussian() * 0.08;
            entityitem.motionY = host.world.rand.nextGaussian() * 0.05 + 0.2;
            entityitem.motionZ = host.world.rand.nextGaussian() * 0.08;
            host.world.spawnEntity(entityitem);
        }
    }
}