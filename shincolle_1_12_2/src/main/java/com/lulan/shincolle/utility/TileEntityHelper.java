package com.lulan.shincolle.utility;

import com.lulan.shincolle.tileentity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class TileEntityHelper {
    private TileEntityHelper() {}

    public static boolean decrItemFuel(BasicTileInventory tile) {
        ITileFurnace furnace = (ITileFurnace) tile;
        if (furnace.getPowerRemained() >= furnace.getPowerMax()) {
            return false;
        }
        float fuelMagni = furnace.getFuelMagni();
        for (int i = 0; i < tile.getSizeInventory(); ++i) {
            ItemStack stack = tile.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            int fuelValue = TileEntityFurnace.getItemBurnTime(stack);
            if (fuelValue > 0) {
                int burnPower = (int)(fuelValue * fuelMagni);
                if (burnPower > 0 && furnace.getPowerRemained() + burnPower < furnace.getPowerMax()) {
                    furnace.setPowerRemained(furnace.getPowerRemained() + burnPower);
                    ItemStack container = stack.getItem().getContainerItem(stack);
                    stack.shrink(1);
                    tile.setInventorySlotContents(i, stack.isEmpty() ? container : stack);
                    return true;
                }
            }

            if (stack.getCount() == 1 && stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
                IFluidHandlerItem fluidHandler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
                FluidStack fluid = fluidHandler.drain(1000, false);
                if (checkLiquidIsLava1000(fluid)) {
                    int burnPower = (int)(20000 * fuelMagni);
                    if (burnPower > 0 && furnace.getPowerRemained() + burnPower < furnace.getPowerMax()) {
                        fluidHandler.drain(1000, true);
                        furnace.setPowerRemained(furnace.getPowerRemained() + burnPower);
                        tile.setInventorySlotContents(i, fluidHandler.getContainer());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean decrLiquidFuel(ITileLiquidFurnace tile) {
        if (tile.getPowerMax() - tile.getPowerRemained() >= 800 && tile.getFluidFuelAmount() >= 40) {
            int amount = tile.consumeFluidFuel(40) * 20;
            amount = (int) (amount * tile.getFuelMagni());
            tile.setPowerRemained(tile.getPowerRemained() + amount);
            return true;
        }
        return false;
    }

    public static int getItemFuelValue(ItemStack fuel) {
        int fuelx = TileEntityFurnace.getItemBurnTime(fuel);
        if (fuelx <= 0 && fuel.getCount() == 1 && fuel.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
            IFluidHandlerItem fluidHandler = fuel.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
            FluidStack fluid = fluidHandler.drain(1000, false);
            if (checkLiquidIsLava1000(fluid)) {
                fuelx = 20000;
            }
        }
        return fuelx;
    }

    public static boolean checkLiquidIsLava1000(FluidStack fluid) {
        return checkLiquidIsLavaWithAmount(fluid, 1000);
    }

    public static boolean checkLiquidIsLavaWithAmount(FluidStack fluid, int amount) {
        return fluid != null && checkLiquidIsLava(fluid.getFluid()) && fluid.amount == amount;
    }

    public static boolean checkLiquidIsLava(Fluid fluid) {
        return fluid != null && fluid == FluidRegistry.LAVA;
    }

    public static void setLargeShipyardBuildMats(TileMultiGrudgeHeavy tile, int matType, int value) {
        if (tile == null) {
            return;
        }
        int[] amounts = {1000, 100, 10, 1};
        int num = amounts[value % 4];
        boolean stockToBuild = value <= 3;
        if (stockToBuild) {
            num = Math.min(num, tile.getMatStock(matType));
            num = Math.min(num, 1000 - tile.getMatBuild(matType));
            tile.addMatStock(matType, -num);
            tile.addMatBuild(matType, num);
        } else {
            num = Math.min(num, tile.getMatBuild(matType));
            tile.addMatBuild(matType, -num);
            tile.addMatStock(matType, num);
        }
    }

    public static void pairingWaypointAndChest(EntityPlayer player, int pid, World w, BlockPos posWp, BlockPos posChest) {
        if (pid <= 0) {
            return;
        }
        TileEntity pos1 = w.getTileEntity(posWp);
        TileEntity pos2 = w.getTileEntity(posChest);
        if (pos1 instanceof ITileWaypoint && pos2 instanceof IInventory) {
            ITileWaypoint wp = (ITileWaypoint) pos1;
            if (wp.getPlayerUID() != pid) {
                player.sendMessage(new TextComponentTranslation("chat.shincolle:wrongowner").appendText(" " + wp.getPlayerUID()));
                return;
            }
            wp.setPairedChest(posChest);
            TextComponentTranslation text = new TextComponentTranslation("chat.shincolle:wrench.setwp");
            text.getStyle().setColor(TextFormatting.AQUA);
            player.sendMessage(text.appendText(" " + TextFormatting.GREEN + posWp.getX() + " " + posWp.getY() + " " + posWp.getZ() + TextFormatting.AQUA + " & " + TextFormatting.GOLD + posChest.getX() + " " + posChest.getY() + " " + posChest.getZ()));
        }
    }

    public static void pairingWaypoints(EntityPlayer player, int pid, World w, BlockPos posF, BlockPos posT) {
        if (pid <= 0) {
            return;
        }
        TileEntity tileF = w.getTileEntity(posF);
        TileEntity tileT = w.getTileEntity(posT);
        if (tileF instanceof ITileWaypoint && tileT instanceof ITileWaypoint) {
            ITileWaypoint wpFrom = (ITileWaypoint) tileF;
            ITileWaypoint wpTo = (ITileWaypoint) tileT;
            if (wpFrom.getPlayerUID() != pid) {
                player.sendMessage(new TextComponentTranslation("chat.shincolle:wrongowner").appendText(" " + wpFrom.getPlayerUID()));
                return;
            }
            wpFrom.setNextWaypoint(posT);
            if (!wpTo.getNextWaypoint().equals(posF)) {
                wpTo.setLastWaypoint(posF);
            }
            TextComponentTranslation text = new TextComponentTranslation("chat.shincolle:wrench.setwp");
            text.getStyle().setColor(TextFormatting.YELLOW);
            player.sendMessage(text.appendText(" " + TextFormatting.GREEN + posF.getX() + " " + posF.getY() + " " + posF.getZ() + TextFormatting.AQUA + " --> " + TextFormatting.GOLD + posT.getX() + " " + posT.getY() + " " + posT.getZ()));
        }
    }

    public static TileEntityChest getAdjChest(TileEntityChest chest) {
        if (chest == null || chest.isInvalid()) {
            return null;
        }
        chest.checkForAdjacentChests();
        TileEntityChest chest2 = chest.adjacentChestXNeg;
        if (chest2 == null) chest2 = chest.adjacentChestXPos;
        if (chest2 == null) chest2 = chest.adjacentChestZNeg;
        if (chest2 == null) chest2 = chest.adjacentChestZPos;
        if (chest2 != null && chest2.isInvalid()) {
            return null;
        }
        return chest2;
    }
}