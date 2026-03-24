package com.lulan.shincolle.utility;

import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.tileentity.BasicTileMulti;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;

public class MulitBlockHelper {
    private static final byte[][][][] PATTERN = new byte[][][][]{new byte[][][]{new byte[][]{{1, 1, 1}, {1, -1, 1}, {-1, -1, -1}}, new byte[][]{{1, 1, 1}, {-1, -1, -1}, {-1, 2, -1}}, new byte[][]{{1, 1, 1}, {1, -1, 1}, {-1, -1, -1}}}};

    public static int checkMultiBlockForm(World world, int xCoord, int yCoord, int zCoord) {
        IBlockState state;
        BlockPos pos;
        Block block;
        byte blockType;
        int patternTemp;
        int patternMatch = 1;
        if (yCoord < 3) {
            return -1;
        }
        for (int x = 0; x < 3; ++x) {
            for (int y = 0; y < 3; ++y) {
                for (int z = 0; z < 3; ++z) {
                    TileEntity t;
                    pos = new BlockPos(xCoord - 1 + x, yCoord - 2 + y, zCoord - 1 + z);
                    state = world.getBlockState(pos);
                    block = state.getBlock();
                    blockType = -1;
                    if (block == ModBlocks.BlockPolymetal) {
                        blockType = 1;
                    }
                    if (block == ModBlocks.BlockGrudgeHeavy) {
                        blockType = 2;
                    }
                    LogHelper.debug("DEBUG: multi block check: pos " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + block.getLocalizedName() + " " + blockType);
                    patternTemp = 0;
                    for (int t2 = 0; t2 < PATTERN.length; ++t2) {
                        if (blockType != PATTERN[t2][x][y][z]) continue;
                        patternTemp = (int)(patternTemp + Math.pow(2.0, t2));
                    }
                    LogHelper.debug("DEBUG: check structure: type " + (patternMatch &= patternTemp) + " " + patternTemp);
                    if (patternMatch == 0) {
                        return -1;
                    }
                    if (blockType <= 0 || !((t = world.getTileEntity(pos)) instanceof BasicTileMulti) || !((BasicTileMulti)t).hasMaster()) continue;
                    return -1;
                }
            }
        }
        LogHelper.debug("DEBUG: check structure: type " + patternMatch);
        return patternMatch;
    }

    public static void setupStructure(World world, int xCoord, int yCoord, int zCoord, int type) {
        ArrayList<BasicTileMulti> tiles = new ArrayList<>();
        BlockPos pos;
        BlockPos masterPos = new BlockPos(xCoord, yCoord, zCoord);
        BasicTileMulti masterTile = null;
        BasicTileMulti tile2;
        TileEntity tile;
        LogHelper.debug("DEBUG: setup structure type: " + type);
        for (int x = xCoord - 1; x < xCoord + 2; ++x) {
            for (int y = yCoord - 2; y < yCoord + 1; ++y) {
                for (int z = zCoord - 1; z < zCoord + 2; ++z) {
                    boolean mflag;
                    pos = new BlockPos(x, y, z);
                    tile = world.getTileEntity(pos);
                    mflag = x == xCoord && y == yCoord && z == zCoord;
                    if (!(tile instanceof BasicTileMulti)) continue;
                    tile2 = (BasicTileMulti)tile;
                    tiles.add(tile2);
                    tile2.setIsMaster(mflag);
                    tile2.setHasMaster(true);
                    tile2.setStructType(type, world);
                    tile2.setMasterCoords(masterPos);
                    if (!mflag) continue;
                    masterTile = tile2;
                }
            }
        }
        for (BasicTileMulti te : tiles) {
            te.setMaster(masterTile);
        }
    }

    private static void resetTileMulti(BasicTileMulti parTile) {
        parTile.setMasterCoords(BlockPos.ORIGIN);
        parTile.setMaster(null);
        parTile.setHasMaster(false);
        parTile.setIsMaster(false);
        parTile.setStructType(0, parTile.getWorld());
    }

    public static void resetStructure(World world, int xCoord, int yCoord, int zCoord) {
        LogHelper.debug("DEBUG: reset struct: client? " + world.isRemote + " " + xCoord + " " + yCoord + " " + zCoord);
        for (int x = xCoord - 1; x < xCoord + 2; ++x) {
            for (int y = yCoord - 2; y < yCoord + 1; ++y) {
                for (int z = zCoord - 1; z < zCoord + 2; ++z) {
                    TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
                    if (!(tile instanceof BasicTileMulti)) continue;
                    MulitBlockHelper.resetTileMulti((BasicTileMulti)tile);
                }
            }
        }
    }
}
