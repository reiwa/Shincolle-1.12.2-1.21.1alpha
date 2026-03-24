package com.lulan.shincolle.utility;

import com.lulan.shincolle.capability.CapaShipInventory;
import com.lulan.shincolle.config.ConfigMining;
import com.lulan.shincolle.crafting.InventoryCraftingFake;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.other.EntityShipFishingHook;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.item.BasicItem;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.tileentity.TileEntityWaypoint;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.lwjgl.Sys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskHelper {
    public static void onUpdateTask(BasicEntityShip host) {
        if (host.getStateFlag(2) || !host.isEntityAlive()) {
            return;
        }
        switch (host.getStateMinor(40)) {
            case 1:
                if (ConfigHandler.enableTask[0]) onUpdateCooking(host);
                break;
            case 2:
                if (ConfigHandler.enableTask[1]) onUpdateFishing(host);
                break;
            case 3:
                if (ConfigHandler.enableTask[2]) onUpdateMining(host);
                break;
            case 4:
                if (ConfigHandler.enableTask[3]) onUpdateCrafting(host);
                break;
            default:
        }
    }

    public static void onUpdateCrafting(BasicEntityShip host) {
        if (host == null) return;
        ItemStack paper = host.getHeldItemMainhand();
        if (paper.isEmpty() || paper.getItem() != ModItems.RecipePaper || !paper.hasTagCompound()) return;
        BlockPos wpPos = new BlockPos(host.getGuardedPos(0), host.getGuardedPos(1), host.getGuardedPos(2));
        if (wpPos.getY() <= 0 || host.getGuardedPos(4) != 1 || host.world.provider.getDimension() != host.getGuardedPos(3)) return;
        TileEntity te = host.world.getTileEntity(wpPos);
        if (!(te instanceof TileEntityWaypoint)) return;
        BlockPos chestPos = ((TileEntityWaypoint) te).getPairedChest();
        if (chestPos == null || chestPos.getY() <= 0) return;
        te = host.world.getTileEntity(chestPos);
        if (!(te instanceof IInventory)) return;
        IInventory chest = (IInventory) te;
        if (host.getDistanceSq(chestPos) > 25.0D) {
            host.getShipNavigate().tryMoveToXYZ(wpPos.getX() + 0.5D, wpPos.getY() + 0.5D, wpPos.getZ() + 0.5D, 1.0D);
            return;
        }
        InventoryCraftingFake recipe = new InventoryCraftingFake(3, 3);
        NBTTagList tagList = paper.getTagCompound().getTagList("Recipe", 10);
        for (int i = 0; i < 9; ++i) {
            NBTTagCompound itemTags = tagList.getCompoundTagAt(i);
            int slot = itemTags.getInteger("Slot");
            if (slot < 0 || slot >= 9) continue;
            recipe.setInventorySlotContents(slot, new ItemStack(itemTags));
        }
        for (int i = 0; i < 9; ++i) {
            if(recipe.getStackInSlot(i) == null){
                recipe.setInventorySlotContents(i, ItemStack.EMPTY);
            }
        }
        IRecipe iRecipe = CraftingManager.findMatchingRecipe(recipe, host.world);
        if (iRecipe == null) return;
        ItemStack result = iRecipe.getCraftingResult(recipe);
        if (result.isEmpty()) return;
        int maxCraft = host.getLevel() / 20 + 1;
        int taskSide = host.getStateMinor(41);
        boolean checkMetadata = (taskSide & Values.N.Pow2[18]) != 0;
        boolean checkOredict = (taskSide & Values.N.Pow2[19]) != 0;
        boolean checkNbt = (taskSide & Values.N.Pow2[20]) != 0;
        boolean craftedSomething = false;
        for (int craftCount = 0; craftCount < maxCraft; ++craftCount) {
            InventoryCraftingFake recipeTemp = new InventoryCraftingFake(3, 3);
            boolean hasAllMaterials = true;
            for (int i = 0; i < 9; ++i) {
                ItemStack requiredStack = recipe.getStackInSlot(i);
                if (requiredStack.isEmpty()) continue;
                ItemStack material = InventoryHelper.getAndRemoveItem(chest, requiredStack, 1, checkMetadata, checkNbt, checkOredict, null);
                if (material.isEmpty() || material.getCount() < requiredStack.getCount()) {
                    if (!material.isEmpty()) InventoryHelper.moveItemstackToInv(host, chest, material);
                    hasAllMaterials = false;
                    break;
                }
                recipeTemp.setInventorySlotContents(i, material);
            }
            if (!hasAllMaterials) {
                for(int i = 0; i < 9; ++i) {
                    ItemStack collectedMaterial = recipeTemp.getStackInSlot(i);
                    if (collectedMaterial != null && !collectedMaterial.isEmpty()) {
                        InventoryHelper.moveItemstackToInv(host, chest, collectedMaterial);
                    }
                }
                break;
            }
            for(int i=0; i<9; i++){
                if(recipeTemp.getStackInSlot(i) == null){
                    recipeTemp.setInventorySlotContents(i, ItemStack.EMPTY);
                }
            }
            IRecipe searchRecipe = CraftingManager.findMatchingRecipe(recipeTemp, host.world);
            if (searchRecipe == null || !InventoryHelper.matchTargetItem(searchRecipe.getCraftingResult(recipeTemp), result, true, true, false)) {
                for(int i = 0; i < 9; ++i) {
                    ItemStack collectedMaterial = recipeTemp.getStackInSlot(i);
                    if (collectedMaterial != null && !collectedMaterial.isEmpty()) {
                        InventoryHelper.moveItemstackToInv(host, chest, collectedMaterial);
                    }
                }
                break;
            }
            craftedSomething = true;
            InventoryHelper.moveItemstackToInv(host, chest, searchRecipe.getCraftingResult(recipeTemp));
            NonNullList<ItemStack> remainList = searchRecipe.getRemainingItems(recipeTemp);
            for (ItemStack remainStack : remainList) {
                if (!remainStack.isEmpty()) InventoryHelper.moveItemstackToInv(host, chest, remainStack);
            }
        }
        if (craftedSomething) {
            host.addShipExp(ConfigHandler.expGainTask[3]);
            host.decrGrudgeNum(ConfigHandler.consumeGrudgeTask[3]);
            host.addMorale(-10);
            host.swingArm(EnumHand.MAIN_HAND);
            if (host.getRNG().nextInt(5) == 0) {
                host.applyParticleEmotion(host.getRNG().nextInt(5) * 6 + 1);
            }
        }
    }

    public static void onUpdateMining(BasicEntityShip host) {
        if (host == null) return;
        ItemStack pickaxe = host.getHeldItemMainhand();
        if (pickaxe.isEmpty() || !isToolEffective(pickaxe, 0, 0)) return;
        if (Math.abs(host.motionX) > 0.1D || Math.abs(host.motionZ) > 0.1D || host.motionY > 0.1D) return;
        if ((host.getTickExisted() & 63) == 0) {
            host.getShipNavigate().tryMoveToXYZ(host.posX + host.getRNG().nextInt(9) - 4.0D, host.posY + host.getRNG().nextInt(5) - 2.0D, host.posZ + host.getRNG().nextInt(9) - 4.0D, 1.0D);
            return;
        }
        if (host.getRNG().nextInt(5) > 2) {
            host.swingArm(EnumHand.MAIN_HAND);
            if (host.getRNG().nextInt(10) > 8) {
                host.applyParticleEmotion(host.getRNG().nextInt(5));
            }
        }
        if ((host.ticksExisted & 31) == 0 && host.ticksExisted - host.getStateTimer(15) > ConfigHandler.tickMining[0] + host.getRNG().nextInt(ConfigHandler.tickMining[1])) {
            int stoneCount = 0;
            boolean canMine = false;
            BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();
            for (int dy = -3; dy < 5 && !canMine; ++dy) {
                for (int dx = -3; dx < 4 && !canMine; ++dx) {
                    for (int dz = -3; dz < 4; ++dz) {
                        mutPos.setPos(host.posX + dx, host.posY + dy, host.posZ + dz);
                        if (host.world.getBlockState(mutPos).getMaterial() == Material.ROCK) {
                            if (++stoneCount > 120) {
                                canMine = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (canMine) {
                generateMiningResult(host);
                host.addShipExp(ConfigHandler.expGainTask[2]);
                host.decrGrudgeNum(ConfigHandler.consumeGrudgeTask[2]);
                host.addMorale(-200);
                host.applyParticleEmotion(host.getRNG().nextInt(5));
                host.swingArm(EnumHand.MAIN_HAND);
                host.setStateTimer(15, host.ticksExisted);
            }
        }
    }

    public static void onUpdateFishing(BasicEntityShip host) {
        if (host == null) return;
        ItemStack rod = host.getHeldItemMainhand();
        if (rod.isEmpty() || rod.getItem() != Items.FISHING_ROD) return;
        BlockPos pos = new BlockPos(host.getGuardedPos(0), host.getGuardedPos(1), host.getGuardedPos(2));
        if (pos.getY() <= 0 || host.getGuardedPos(4) != 1 || host.world.provider.getDimension() != host.getGuardedPos(3)) return;
        if (host.getDistanceSq(pos) > 10.0D) {
            host.getShipNavigate().tryMoveToXYZ(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 1.0D);
            return;
        }
        if (Math.abs(host.motionX) > 0.1D || Math.abs(host.motionZ) > 0.1D || host.motionY > 0.1D) return;
        BlockPos liquidPos = BlockHelper.getNearbyLiquid(host, false, true, 5, 3);
        if (liquidPos == null) return;
        if (host.fishHook == null || host.fishHook.isDead) {
            host.swingArm(EnumHand.MAIN_HAND);
            EntityShipFishingHook hook = new EntityShipFishingHook(host.world, host);
            host.world.spawnEntity(hook);
            host.fishHook = hook;
            host.applyParticleEmotion(host.getRNG().nextInt(4) + 1);
            return;
        }
        if (host.fishHook.ticksExisted > ConfigHandler.tickFishing[0] + host.getRNG().nextInt(ConfigHandler.tickFishing[1])) {
            generateFishingResult(host);
            host.fishHook.setDead();
            host.addShipExp(ConfigHandler.expGainTask[1]);
            host.decrGrudgeNum(ConfigHandler.consumeGrudgeTask[1]);
            host.addMorale(300);
            host.applyParticleEmotion(host.getRNG().nextInt(5));
            host.swingArm(EnumHand.MAIN_HAND);
        } else if (host.fishHook.ticksExisted > ConfigHandler.tickFishing[0] + ConfigHandler.tickFishing[1] + 20) {
            host.fishHook.setDead();
        }
    }

    public static void onUpdateCooking(BasicEntityShip host) {
        if (host == null) return;
        ItemStack mainStack = host.getHeldItemMainhand();
        if (mainStack.isEmpty() || !canItemStackSmelt(mainStack)) return;
        BlockPos wpPos = new BlockPos(host.getGuardedPos(0), host.getGuardedPos(1), host.getGuardedPos(2));
        if (wpPos.getY() <= 0 || host.getGuardedPos(4) != 1 || host.world.provider.getDimension() != host.getGuardedPos(3)) return;
        TileEntity te = host.world.getTileEntity(wpPos);
        if (!(te instanceof TileEntityWaypoint)) return;
        BlockPos furnacePos = ((TileEntityWaypoint) te).getPairedChest();
        if (furnacePos == null || furnacePos.getY() <= 0) return;
        te = host.world.getTileEntity(furnacePos);
        if (!(te instanceof ISidedInventory)) return;
        ISidedInventory furnace = (ISidedInventory) te;
        if (host.getDistanceSq(furnacePos) > 25.0D) {
            host.getShipNavigate().tryMoveToXYZ(wpPos.getX() + 0.5D, wpPos.getY() + 0.5D, wpPos.getZ() + 0.5D, 1.0D);
            return;
        }
        CapaShipInventory inv = host.getCapaShipInventory();
        int taskSide = host.getStateMinor(41);
        boolean checkMeta = (taskSide & Values.N.Pow2[18]) != 0;
        boolean checkOredict = (taskSide & Values.N.Pow2[19]) != 0;
        boolean checkNbt = (taskSide & Values.N.Pow2[20]) != 0;
        boolean swing = false;
        ItemStack materialToMove = InventoryHelper.getAndRemoveItem(inv, mainStack, 64, checkMeta, checkNbt, checkOredict, null);
        if (!materialToMove.isEmpty()) {
            swing = true;
            int[] inSlots = InventoryHelper.getSlotsFromSide(furnace, materialToMove, taskSide, 0);
            InventoryHelper.moveItemstackToInv(furnace, materialToMove, inSlots);
            if (!materialToMove.isEmpty()) InventoryHelper.moveItemstackToInv(host, inv, materialToMove);
        }
        ItemStack fuelToMove = InventoryHelper.getAndRemoveItem(inv, host.getHeldItemOffhand(), 64, checkMeta, checkNbt, checkOredict, null);
        if (!fuelToMove.isEmpty()) {
            swing = true;
            int[] fuSlots = InventoryHelper.getSlotsFromSide(furnace, fuelToMove, taskSide, 2);
            InventoryHelper.moveItemstackToInv(furnace, fuelToMove, fuSlots);
            if (!fuelToMove.isEmpty()) InventoryHelper.moveItemstackToInv(host, inv, fuelToMove);
        }
        int[] outSlots = InventoryHelper.getSlotsFromSide(furnace, ItemStack.EMPTY, taskSide, 1);
        if (outSlots.length > 0) {
            ItemStack resultStack = FurnaceRecipes.instance().getSmeltingResult(mainStack);
            for (int id : outSlots) {
                ItemStack tempStack = furnace.getStackInSlot(id);
                if (!tempStack.isEmpty() && InventoryHelper.matchTargetItem(tempStack, resultStack, true, true, false)) {
                    ItemStack takenStack = furnace.decrStackSize(id, tempStack.getCount());
                    swing = true;
                    InventoryHelper.moveItemstackToInv(host, inv, takenStack);
                    host.addShipExp(ConfigHandler.expGainTask[0]);
                    host.decrGrudgeNum(ConfigHandler.consumeGrudgeTask[0]);
                    host.addMorale(100);
                    float failChance = (ConfigHandler.maxLevel - host.getLevel()) / (float) ConfigHandler.maxLevel * 0.2F + 0.05F;
                    if (host.getRNG().nextFloat() < failChance) {
                        EntityItem entityitem = new EntityItem(host.world, furnacePos.getX() + 0.5D, furnacePos.getY() + 1.0D, furnacePos.getZ() + 0.5D, new ItemStack(Items.COAL, 1, 1));
                        host.world.spawnEntity(entityitem);
                        host.applyEmotesReaction(6);
                    } else if (host.getRNG().nextInt(7) == 0) {
                        host.applyParticleEmotion(host.getRNG().nextInt(5));
                    }
                    break;
                }
            }
        }
        if (swing) {
            host.swingArm(EnumHand.MAIN_HAND);
        }
    }

    public static boolean canItemStackSmelt(ItemStack stack) {
        return !stack.isEmpty() && !FurnaceRecipes.instance().getSmeltingResult(stack).isEmpty();
    }

    public static List<ConfigMining.ItemEntry> getMiningLootList(int worldid, int biomeid, int lvShip, int lvHeight, int lvTool) {
        ArrayList<ConfigMining.ItemEntry> tempList = new ArrayList<>();
        Map<Integer, List<ConfigMining.ItemEntry>> worldMap = ConfigMining.MININGMAP.get(worldid);
        if (worldMap != null) {
            if (worldMap.containsKey(biomeid)) tempList.addAll(worldMap.get(biomeid));
            if (worldMap.containsKey(-999999)) tempList.addAll(worldMap.get(-999999));
        }
        Map<Integer, List<ConfigMining.ItemEntry>> allWorldsMap = ConfigMining.MININGMAP.get(999999);
        if (allWorldsMap != null) {
            if (allWorldsMap.containsKey(biomeid)) tempList.addAll(allWorldsMap.get(biomeid));
            if (allWorldsMap.containsKey(-999999)) tempList.addAll(allWorldsMap.get(-999999));
        }
        ArrayList<ConfigMining.ItemEntry> resultList = new ArrayList<>();
        for (ConfigMining.ItemEntry item : tempList) {
            if (lvShip >= item.lvShip && lvHeight <= item.lvHeight && lvTool >= item.lvTool) {
                resultList.add(item);
            }
        }
        return resultList;
    }

    public static void generateMiningResult(EntityLivingBase host) {
        if (!(host instanceof BasicEntityShip)) return;
        BasicEntityShip ship = (BasicEntityShip) host;
        ItemStack pickaxe = ship.getHeldItemMainhand();
        if (pickaxe.isEmpty()) return;
        List<ConfigMining.ItemEntry> lootList = getMiningLootList(ship.world.provider.getDimension(), Biome.getIdForBiome(ship.world.getBiome(ship.getPosition())), ship.getLevel(), (int) ship.posY, pickaxe.getItem().getHarvestLevel(pickaxe, "pickaxe", null, null));
        if (lootList.isEmpty()) return;
        int totalWeight = 0;
        for (ConfigMining.ItemEntry entry : lootList) {
            totalWeight += entry.weight;
        }
        if (totalWeight <= 0) return;
        int roll = ship.getRNG().nextInt(totalWeight);
        ConfigMining.ItemEntry resultEntry = null;
        for (ConfigMining.ItemEntry entry : lootList) {
            roll -= entry.weight;
            if (roll < 0) {
                resultEntry = entry;
                break;
            }
        }
        if (resultEntry == null) return;
        Item item = Item.getByNameOrId(resultEntry.itemName);
        if (item == null) return;
        int metadata = (resultEntry.itemMeta <= 0 && item instanceof BasicItem) ? ship.getRNG().nextInt(((BasicItem) item).getTypes()) : resultEntry.itemMeta;
        int stacksize = resultEntry.min;
        if (resultEntry.max > resultEntry.min) {
            stacksize += ship.getRNG().nextInt(resultEntry.max - resultEntry.min + 1);
        }
        if (resultEntry.enchant > 0.0F) {
            int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, pickaxe);
            int luck = BuffHelper.getPotionLevel(host, 26);
            stacksize *= (1.0F + (fortune + luck) * resultEntry.enchant);
        }
        ItemStack stack = new ItemStack(item, stacksize, metadata);
        InventoryHelper.moveItemstackToInv(ship, ship.getCapaShipInventory(), stack);
    }

    public static void generateFishingResult(EntityLivingBase host) {
        if (!(host.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) host.world;
        float luck = 0.0F;
        BasicEntityShip ship = null;
        if (host instanceof BasicEntityShip) {
            ship = (BasicEntityShip) host;
            int luckEnch = Math.max(EnchantmentHelper.getEnchantmentLevel(Enchantments.LUCK_OF_THE_SEA, ship.getHeldItemMainhand()), EnchantmentHelper.getEnchantmentLevel(Enchantments.LUCK_OF_THE_SEA, ship.getHeldItemOffhand()));
            luck = luckEnch + BuffHelper.getPotionLevel(host, 26) + (float) ship.getLevel() / (float) ConfigHandler.maxLevel * 1.5F;
        } else if (!(host instanceof IShipAttackBase)) {
            return;
        }
        LootContext.Builder builder = new LootContext.Builder(world);
        builder.withLuck(luck).withLootedEntity(host);
        if (ship != null) {
            EntityPlayer player = EntityHelper.getEntityPlayerByUID(ship.getPlayerUID());
            if (player != null) builder.withPlayer(player);
        }
        List<ItemStack> loot = world.getLootTableManager().getLootTableFromLocation(LootTableList.GAMEPLAY_FISHING).generateLootForPools(host.getRNG(), builder.build());
        for (ItemStack itemstack : loot) {
            if (host instanceof BasicEntityShip) {
                InventoryHelper.moveItemstackToInv(host, ((BasicEntityShip) host).getCapaShipInventory(), itemstack);
            } else {
                InventoryHelper.dropItemOnGround(host, itemstack);
            }
        }
    }

    public static boolean isToolEffective(ItemStack stack, int targetType, int targetLevel) {
        if (stack.isEmpty()) return false;
        String type;
        switch (targetType) {
            case 1: type = "shovel"; break;
            case 2: type = "axe"; break;
            default: type = "pickaxe"; break;
        }
        return stack.getItem().getHarvestLevel(stack, type, null, null) >= targetLevel;
    }

    public static void onUpdatePumping(BasicEntityShip ship) {
        int delay;
        int level = ship.getLevel();
        if (level >= 145) delay = 3;
        else if (level >= 115) delay = 7;
        else if (level >= 75) delay = 15;
        else if (level >= 30) delay = 31;
        else delay = 63;
        boolean hasDrum = ship.getShipType() == 7 && ship.getStateFlag(1) || InventoryHelper.checkItemInShipInventory(ship.getCapaShipInventory(), ModItems.EquipDrum, 1, 0, 6);
        if ((ship.ticksExisted & delay) == 0 && hasDrum) {
            pumpLiquid(ship);
        }
        if ((ship.ticksExisted & 3) == 0 && hasDrum) {
            pumpXP(ship);
        }
    }

    private static void pumpLiquid(BasicEntityShip ship) {
        BlockPos pos = BlockHelper.getNearbyLiquid(ship, true, false, 3, 0);
        EntityPlayer player = EntityHelper.getEntityPlayerByUID(ship.getPlayerUID());
        if (pos == null || player == null || !player.isAllowEdit() || !ship.world.isBlockModifiable(player, pos)) return;
        IBlockState state = ship.world.getBlockState(pos);
        FluidStack fs = null;
        if (state.getBlock() instanceof BlockLiquid && state.getValue(BlockLiquid.LEVEL) == 0) {
            if (state.getMaterial() == Material.WATER) fs = new FluidStack(FluidRegistry.WATER, 1000);
            else if (state.getMaterial() == Material.LAVA) fs = new FluidStack(FluidRegistry.LAVA, 1000);
        } else if (state.getBlock() instanceof IFluidBlock) {
            IFluidBlock fb = (IFluidBlock) state.getBlock();
            if (fb.canDrain(ship.world, pos)) fs = fb.drain(ship.world, pos, false);
        }
        if (fs != null && InventoryHelper.tryFillContainer(ship.getCapaShipInventory(), fs.copy())) {
            int fluidType = (fs.getFluid() == FluidRegistry.LAVA) ? 1 : ((fs.getFluid() == FluidRegistry.WATER) ? 0 : -1);
            int checkDepth = (fluidType >= 0) ? ConfigHandler.infLiquid[fluidType] : 10;
            if (!BlockHelper.checkBlockNearbyIsSameMaterial(ship.world, state.getMaterial(), pos.getX(), pos.getY(), pos.getZ(), 3, checkDepth)) {
                ship.world.setBlockState(pos, Blocks.AIR.getDefaultState(), 11);
            }
            if (ship.getRNG().nextInt(3) == 0) {
                ship.playSound(SoundEvents.ITEM_BUCKET_FILL, 0.5F, ship.getRNG().nextFloat() * 0.4F + 0.8F);
            }
            if (state.getBlock() instanceof IFluidBlock) ((IFluidBlock) state.getBlock()).drain(ship.world, pos, true);
        }
    }

    private static void pumpXP(BasicEntityShip ship) {
        CapaShipInventory inv = ship.getCapaShipInventory();
        int botId = InventoryHelper.matchTargetItemExceptSlots(inv, new ItemStack(Items.GLASS_BOTTLE), false, false, false, null);
        List<EntityXPOrb> getlist = ship.world.getEntitiesWithinAABB(EntityXPOrb.class, ship.getEntityBoundingBox().grow(7.0D));
        if (!getlist.isEmpty()) {
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(ship.dimension, ship.posX, ship.posY, ship.posZ, 64.0D);
            for (EntityXPOrb xp : getlist) {
                if (ship.getDistanceSq(xp) > 9.0D) {
                    Dist4d pullvec = CalcHelper.getDistanceFromA2B(ship, xp);
                    xp.addVelocity(pullvec.x * -0.25D, pullvec.y * -0.25D, pullvec.z * -0.25D);
                    CommonProxy.channelE.sendToAllAround(new S2CEntitySync(xp, 0, (byte) 54), point);
                } else {
                    ship.world.playSound(null, ship.posX, ship.posY, ship.posZ, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.1F, 0.5F * ((ship.getRNG().nextFloat() - ship.getRNG().nextFloat()) * 0.7F + 1.8F));
                    if (xp.xpValue > 0) ship.setStateMinor(42, ship.getStateMinor(42) + xp.xpValue);
                    xp.setDead();
                }
            }
        }
        if (botId >= 0 && ship.getStateMinor(42) >= 8) {
            ship.setStateMinor(42, ship.getStateMinor(42) - 8);
            inv.decrStackSize(botId, 1);
            InventoryHelper.moveItemstackToInv(ship, inv, new ItemStack(Items.EXPERIENCE_BOTTLE));
        }
    }
}