package com.lulan.shincolle.utility;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.IShipOwner;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.tileentity.TileEntityLightBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.HashSet;
import java.util.Random;

public class BlockHelper {
    private BlockHelper() {}

    private static final Random rand = new Random();
    public static final FluidStack SampleFluidLava = new FluidStack(FluidRegistry.LAVA, 1000);

    public static int[] getSafeBlockWithin5x5(final World world, final int x, final int y, final int z) {
        return BlockHelper.getSafeBlockWithinRange(world, x, y, z, 3, 4, 3);
    }

    public static int[] getSafeBlockWithinRange(final World world, final int x, final int y, final int z, final int ranX, final int ranY, final int ranZ) {
        final int xLimit = ranX * 2;
        final int yLimit = ranY * 2;
        final int zLimit = ranZ * 2;
        int offsetX = 0;
        int offsetY = 0;
        int offsetZ = 0;
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int i = 0; i <= xLimit; ++i) {
            offsetX += (i % 2 == 0) ? -i : i;
            offsetZ = 0;
            for (int j = 0; j <= zLimit; ++j) {
                offsetZ += (j % 2 == 0) ? -j : j;
                offsetY = 0;
                for (int k = 0; k <= yLimit; ++k) {
                    offsetY += (k % 2 == 0) ? -k : k;
                    final int currentX = x + offsetX;
                    final int currentY = y + offsetY;
                    final int currentZ = z + offsetZ;
                    if (BlockHelper.checkBlockSafe(world, currentX, currentY, currentZ)) {
                        for (int fallDist = 0; fallDist <= Math.abs(offsetY); ++fallDist) {
                            if (BlockHelper.checkBlockCanStandAt(world.getBlockState(mutablePos.setPos(currentX, currentY - fallDist - 1, currentZ)))) {
                                return new int[]{currentX, currentY - fallDist, currentZ};
                            }
                        }
                    }
                }
            }
        }
        LogHelper.debug("DEBUG: find block fail");
        return new int[0];
    }

    public static boolean checkBlockSafe(final World world, final BlockPos pos) {
        final IBlockState state = world.getBlockState(pos);
        return state.getBlock().isPassable(world, pos) || BlockHelper.checkBlockSafe(state);
    }

    public static boolean checkBlockSafe(final World world, final int x, final int y, final int z) {
        return BlockHelper.checkBlockSafe(world, new BlockPos(x, y, z));
    }

    public static boolean checkBlockSafe(final IBlockState state) {
        final Block block = state.getBlock();
        return block == Blocks.AIR || state.getMaterial() == Material.AIR || block == ModBlocks.BlockWaypoint || BlockHelper.checkBlockIsLiquid(state);
    }

    public static boolean checkBlockIsLiquid(final IBlockState state) {
        if (state == null) return false;
        final Block block = state.getBlock();
        return state.getMaterial().isLiquid() || block instanceof IFluidBlock || block instanceof BlockLiquid;
    }

    public static boolean checkBlockIsLiquid(final IBlockState state, final int level) {
        if (state == null) return false;
        final Block block = state.getBlock();
        if (block instanceof BlockLiquid) {
            return state.getValue(BlockLiquid.LEVEL) == level;
        }
        return state.getMaterial().isLiquid() || block instanceof IFluidBlock;
    }

    public static boolean checkBlockNearbyIsLiquid(final World world, final int x, final int y, final int z, final int range) {
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int ox = -range; ox <= range; ++ox) {
            for (int oy = -range; oy <= range; ++oy) {
                for (int oz = -range; oz <= range; ++oz) {
                    if (BlockHelper.checkBlockIsLiquid(world.getBlockState(mutablePos.setPos(x + ox, y + oy, z + oz)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean checkBlockNearbyIsSameMaterial(final World world, final Material target, final int x, final int y, final int z, final int rangeXZ, final int rangeY) {
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 3; ++i) {
            final int offsetX = rand.nextInt(rangeXZ * 2 + 1) - rangeXZ;
            final int offsetZ = rand.nextInt(rangeXZ * 2 + 1) - rangeXZ;
            for (int offsetY = 0; offsetY < rangeY; ++offsetY) {
                if (world.getBlockState(mutablePos.setPos(x + offsetX, y - offsetY, z + offsetZ)).getMaterial() != target) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean checkBlockCanStandAt(final IBlockState state) {
        if (state == null) return false;
        final Material mat = state.getMaterial();
        if (mat == Material.AIR || mat == Material.FIRE) return false;
        return mat.blocksMovement() || BlockHelper.checkBlockIsLiquid(state);
    }

    public static double[] findRandomPosition(final Entity host, final Entity target, final double minDist, final double randDist, final int mode) {
        for (int i = 0; i < 25; ++i) {
            final double rdx = rand.nextDouble() * randDist + minDist;
            final double rdy = rand.nextDouble() * randDist * 0.5;
            final double rdz = rand.nextDouble() * randDist + minDist;
            double newX = 0;
            double newZ = 0;
            final double newY = target.posY + target.height * 0.75 + rdy;
            switch (mode) {
                case 0:
                    switch (rand.nextInt(4)) {
                        case 0: newX = target.posX + rdx; newZ = target.posZ - rdz; break;
                        case 1: newX = target.posX - rdx; newZ = target.posZ + rdz; break;
                        case 2: newX = target.posX - rdx; newZ = target.posZ - rdz; break;
                        case 3: newX = target.posX + rdx; newZ = target.posZ + rdz; break;
                        default:
                    }
                    break;
                case 1:
                    newX = host.posX > target.posX ? target.posX - rdx : target.posX + rdx;
                    newZ = host.posZ > target.posZ ? target.posZ - rdz : target.posZ + rdz;
                    break;
                case 2:
                    newX = host.motionX < 0.0 ? target.posX - rdx : target.posX + rdx;
                    newZ = host.motionZ < 0.0 ? target.posZ - rdz : target.posZ + rdz;
                    break;
                default:
            }
            if (BlockHelper.checkBlockSafe(host.world, (int)newX, (int)newY, (int)newZ)) {
                return new double[]{newX, newY, newZ};
            }
        }
        return new double[]{target.posX, target.posY + 2.0, target.posZ};
    }

    public static BlockPos findRandomSafePos(final Entity target) {
        if (target == null) return BlockPos.ORIGIN;
        final BlockPos basePos = target.getPosition();
        final BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 20; ++i) {
            final float angle = rand.nextFloat() * (float)Math.PI * 2.0F;
            final double newX = basePos.getX() + MathHelper.cos(angle) * 6.0D;
            final double newZ = basePos.getZ() + MathHelper.sin(angle) * 6.0D;
            testPos.setPos(newX, basePos.getY(), newZ);
            if (BlockHelper.checkBlockSafe(target.world, testPos)) {
                return testPos.toImmutable();
            }
        }
        return basePos;
    }

    public static BlockPos findTopSafePos(final Entity target) {
        if (target == null) return BlockPos.ORIGIN;
        final BlockPos basePos = target.getPosition();
        final BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();
        for (int dy = (int)target.height + 7; dy > 0; --dy) {
            testPos.setPos(basePos.getX(), basePos.getY() + dy, basePos.getZ());
            if (BlockHelper.checkBlockSafe(target.world, testPos)) {
                return testPos.toImmutable();
            }
        }
        return basePos;
    }

    @SideOnly(value=Side.CLIENT)
    public static RayTraceResult getPlayerMouseOverBlockOnWater(final double dist, final float partialTicks) {
        Entity viewer = ClientProxy.getMineraft().getRenderViewEntity();
        if (viewer != null && viewer.getRidingEntity() instanceof BasicEntityMount && !ClientProxy.isViewPlayer) {
            final Entity host = ((BasicEntityMount)viewer.getRidingEntity()).getHostEntity();
            if (host != null) viewer = host;
        }
        return BlockHelper.getMouseOverBlock(viewer, dist, partialTicks, true, false, false);
    }

    @SideOnly(value=Side.CLIENT)
    public static RayTraceResult getPlayerMouseOverBlockThroughWater(final double dist, final float partialTicks) {
        Entity viewer = ClientProxy.getMineraft().getRenderViewEntity();
        if (viewer != null && viewer.getRidingEntity() instanceof BasicEntityMount && !ClientProxy.isViewPlayer) {
            final Entity host = ((BasicEntityMount)viewer.getRidingEntity()).getHostEntity();
            if (host != null) viewer = host;
        }
        return BlockHelper.getMouseOverBlock(viewer, dist, partialTicks, false, true, true);
    }

    @SideOnly(value=Side.CLIENT)
    public static RayTraceResult getMouseOverBlock(final Entity viewer, final double dist, final float partialTicks, final boolean stopOnLiquid, final boolean ignoreBlockWithoutBoundingBox, final boolean returnLastUncollidableBlock) {
        if (viewer == null) return null;
        final Vec3d eyePos = viewer.getPositionEyes(partialTicks);
        final Vec3d lookVec = viewer.getLook(partialTicks);
        final Vec3d endPos = eyePos.add(new Vec3d(lookVec.x * dist, lookVec.y * dist, lookVec.z * dist));
        return viewer.world.rayTraceBlocks(eyePos, endPos, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock);
    }

    public static void placeLightBlock(final World world, final BlockPos pos) {
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int i = -1; i <= 1; ++i) {
            for (int j = 1; j <= 2; ++j) {
                for (int k = -1; k <= 1; ++k) {
                    mutablePos.setPos(pos.getX() + i, pos.getY() + j, pos.getZ() + k);
                    if (world.getBlockState(mutablePos).getMaterial() == Material.AIR) {
                        world.setBlockState(mutablePos, ModBlocks.BlockLightAir.getDefaultState(), 2);
                        final TileEntity tile = world.getTileEntity(mutablePos);
                        if (tile instanceof TileEntityLightBlock) {
                            ((TileEntityLightBlock)tile).type = 0;
                            ((TileEntityLightBlock)tile).tick = 1;
                        }
                        return;
                    }
                }
            }
        }
    }

    public static void updateNearbyLightBlock(final World world, final BlockPos pos) {
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int i = -1; i <= 1; ++i) {
            for (int j = 1; j <= 2; ++j) {
                for (int k = -1; k <= 1; ++k) {
                    mutablePos.setPos(pos.getX() + i, pos.getY() + j, pos.getZ() + k);
                    final TileEntity tile = world.getTileEntity(mutablePos);
                    if (tile instanceof TileEntityLightBlock) {
                        ((TileEntityLightBlock)tile).tick = 1;
                        return;
                    }
                }
            }
        }
    }

    public static HashSet<ChunkPos> getChunksWithinRange(final int x, final int z, final int mode) {
        final HashSet<ChunkPos> chunks = new HashSet<>();
        switch (mode) {
            case 1:
                chunks.add(new ChunkPos(x, z));
                break;
            case 2:
                for (int i = -1; i <= 1; ++i) {
                    for (int j = -1; j <= 1; ++j) {
                        chunks.add(new ChunkPos(x + i, z + j));
                    }
                }
                break;
            default:
        }
        return chunks;
    }

    public static int getToppestWaterHeight(final World world, final int x, int y, final int z) {
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(x, y, z);
        if (checkBlockIsLiquid(world.getBlockState(mutablePos))) {
            while (y < 255 && checkBlockIsLiquid(world.getBlockState(mutablePos.add(0,1,0)))) {
                y++;
            }
            return y;
        }
        return y - 1;
    }

    public static boolean checkTileOwner(final Entity target, final TileEntity tile) {
        if (target == null || tile == null || !(tile instanceof IShipOwner)) {
            return false;
        }
        final int playerUID = EntityHelper.getPlayerUID(target);
        final int tileOwnerUID = ((IShipOwner)tile).getPlayerUID();
        return playerUID != -1 && playerUID == tileOwnerUID;
    }

    public static BlockPos getNearbyLiquid(final Entity host, final boolean checkHostPos, final boolean useRandom, final int radius, final int depth) {
        if (host == null) return null;
        final World world = host.world;
        final BlockPos basePos = host.getPosition();
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int dy = 1; dy >= -2; --dy) {
            final int currentY = basePos.getY() + dy;
            if (useRandom) {
                final int maxTry = radius * radius * 2 + 1;
                for (int i = 0; i < maxTry; i++) {
                    final int dx = rand.nextInt(radius * 2 + 1) - radius;
                    final int dz = rand.nextInt(radius * 2 + 1) - radius;
                    mutablePos.setPos(basePos.getX() + dx, currentY, basePos.getZ() + dz);
                    if (checkBlockIsLiquid(world.getBlockState(mutablePos), 0) && (depth <= 0 || checkDepth(world, mutablePos, depth))) {
                        return mutablePos.toImmutable();
                    }
                }
            } else {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx == 0 && dz == 0) continue;
                        mutablePos.setPos(basePos.getX() + dx, currentY, basePos.getZ() + dz);
                        if (checkBlockIsLiquid(world.getBlockState(mutablePos), 0) && (depth <= 0 || checkDepth(world, mutablePos, depth))) {
                            return mutablePos.toImmutable();
                        }
                    }
                }
            }
        }
        if (checkHostPos) {
            for (int dy = 1; dy >= -1; --dy) {
                mutablePos.setPos(basePos.getX(), basePos.getY() + dy, basePos.getZ());
                if (checkBlockIsLiquid(world.getBlockState(mutablePos), 0) && (depth <= 0 || checkDepth(world, mutablePos, depth))) {
                    return mutablePos.toImmutable();
                }
            }
        }
        return null;
    }

    private static boolean checkDepth(final World world, final BlockPos pos, final int depth) {
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int d = 1; d <= depth; ++d) {
            if (!checkBlockIsLiquid(world.getBlockState(mutablePos.setPos(pos.getX(), pos.getY() - d, pos.getZ())), 0)) {
                return false;
            }
        }
        return true;
    }
}