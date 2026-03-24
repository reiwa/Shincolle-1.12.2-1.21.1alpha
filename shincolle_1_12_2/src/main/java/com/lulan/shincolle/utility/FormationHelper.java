package com.lulan.shincolle.utility;

import com.lulan.shincolle.block.BlockDesk;
import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.other.EntitySeat;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.unitclass.AttrsAdv;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.lulan.shincolle.utility.FormationHelperController.removeOldController;
import static com.lulan.shincolle.utility.FormationHelperController.spawnControllerAtFlagship;

public class FormationHelper {
    private FormationHelper() {}

    public static void setFormationID(int[] parms) {
        EntityPlayer player = EntityHelper.getEntityPlayerByID(parms[0], parms[1], false);
        if (player == null) return;
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
        if (capa != null) {
            FormationHelper.setFormationID(player, capa.getCurrentTeamID(), parms[2]);
        }
    }

    public static void setFormationID(EntityPlayer player, int teamID, int formatID) {
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
        if (capa == null) return;
        int shipCount = capa.getNumberOfShip(teamID);
        if (shipCount > 4 && formatID > 0) {
            FormationHelper.setFormationForShip(capa, teamID);
            capa.setFormatID(teamID, formatID);
            List<BasicEntityShip> ships = capa.getShipEntityByMode(2);
            if (!ships.isEmpty()) {
                BasicEntityShip flagship = ships.get(0);
                if (flagship.getStateMinor(24) != 2 && !flagship.getStateFlag(11)) {
                    FormationHelper.applyFormationMoving(ships, formatID, MathHelper.floor(flagship.posX), (int) flagship.posY, MathHelper.floor(flagship.posZ), false);
                }
            }
        } else {
            FormationHelper.setFormationForShip(capa, teamID);
            capa.setFormatID(teamID, 0);
            player.sendMessage(new TextComponentTranslation("chat.shincolle:formation.notenough"));
        }
        capa.sendSyncPacket(1);
    }

    public static void setFormationForShip(CapaTeitoku capa, int teamID) {
        for (int i = 0; i < 6; ++i) {
            BasicEntityShip ship = capa.getShipEntity(teamID, i);
            if (ship == null) continue;
            ship.setUpdateFlag(0, true);
            int shipUID = ship.getShipUID();
            for (int otherTeamID = 0; otherTeamID < 9; ++otherTeamID) {
                if (otherTeamID == teamID) continue;
                int[] teamCheckResult = capa.checkIsInTeam(shipUID, otherTeamID);
                if (teamCheckResult[1] <= 0) continue;
                capa.setFormatID(otherTeamID, 0);
                for (int j = 0; j < 6; ++j) {
                    BasicEntityShip otherShip = capa.getShipEntity(otherTeamID, j);
                    if (otherShip != null) {
                        otherShip.setUpdateFlag(0, true);
                    }
                }
            }
        }
    }

    public static float getFormationMOV(BasicEntityShip ship) {
        float mov = 0.0f;
        if (ship != null && ship.getStateMinor(26) > 0) {
            AttrsAdv attrs = (AttrsAdv) ship.getAttrs();
            if (attrs != null) {
                mov = attrs.getMinMOV() + attrs.getAttrsFormation(7) * (float) ConfigHandler.scaleShip[4];
            }
        }
        if (mov > ConfigHandler.limitShipAttrs[7]) {
            mov = (float) ConfigHandler.limitShipAttrs[7];
        } else if (mov < 0.0f) {
            mov = 0.0f;
        }
        return mov;
    }

    public static float[] getFormationBuffValue(int formationID, int slotID) {
        float[] fvalue = Values.FormationAttrs.get(formationID * 10 + slotID);
        return fvalue != null ? Arrays.copyOf(fvalue, fvalue.length) : AttrsAdv.getResetFormationValue();
    }

    public static void applyFormationMoving(List<BasicEntityShip> ships, int formatID, int x, int y, int z, boolean samePosChecking) {
        if (ships.isEmpty() || ships.get(0) == null) return;
        BasicEntityShip flagShip = ships.get(0);
        EntityPlayer owner = EntityHelper.getEntityPlayerByUID(flagShip.getPlayerUID());
        if (owner == null || owner.dimension != flagShip.dimension) return;

        int[] oldPosition = null;
        if (flagShip.getGuardedPos(1) > 0 && flagShip.getGuardedPos(4) >= 0) {
            oldPosition = new int[]{flagShip.getGuardedPos(0), flagShip.getGuardedPos(1), flagShip.getGuardedPos(2)};
        }

        boolean[] faceXP = getFormationDirection(x, z, flagShip.posX, flagShip.posZ);
        int[] newPos = {x, y, z};

        for (BasicEntityShip ship : ships) {
            if (ship == null) continue;
            switch (formatID) {
                case 1: case 4:
                    newPos = setFormationPosAndApplyGuardPos1(ship, formatID, faceXP[0], faceXP[1], newPos[0], newPos[1], newPos[2]);
                    break;
                case 2: case 3: case 5:
                    setFormationPosAndApplyGuardPos2(ship, formatID, faceXP[0], faceXP[1], newPos[0], newPos[1], newPos[2]);
                    break;
                default:
                    applyShipGuard(ship, x, y, z, true);
                    break;
            }
            ship.applyEmotesReaction(5);
            CommonProxy.channelE.sendTo(new S2CEntitySync(ship, (byte) 3), (EntityPlayerMP) owner);
        }

        if (samePosChecking && oldPosition != null && flagShip.getGuardedPos(0) == oldPosition[0] && flagShip.getGuardedPos(1) == oldPosition[1] && flagShip.getGuardedPos(2) == oldPosition[2]) {
            for (BasicEntityShip ship : ships) {
                if (ship == null) continue;
                ship.setGuardedPos(-1, -1, -1, 0, 0);
                ship.setGuardedEntity(null);
                ship.setStateFlag(11, true);
            }
        }
    }

    public static int[] setFormationPosAndApplyGuardPos1(BasicEntityShip ship, int formatType, boolean alongX, boolean faceP, int baseX, int baseY, int baseZ) {
        int[] safeBasePos = BlockHelper.getSafeBlockWithin5x5(ship.world, baseX, baseY, baseZ);
        if (safeBasePos != null) {
            applyShipGuard(ship, safeBasePos[0], safeBasePos[1], safeBasePos[2], true);
            return calculateNextBasePosition(formatType, alongX, faceP, safeBasePos[0], safeBasePos[1], safeBasePos[2]);
        }
        applyShipGuard(ship, baseX, baseY, baseZ, true);
        return new int[]{baseX, baseY, baseZ};
    }

    private static int[] calculateNextBasePosition(int formatType, boolean alongX, boolean faceP, int x, int y, int z) {
        switch (formatType) {
            case 4: return nextEchelonPos(faceP, x, y, z);
            case 1: default: return nextLineAheadPos(alongX, faceP, x, y, z);
        }
    }

    public static void setFormationPosAndApplyGuardPos2(BasicEntityShip ship, int formatType, boolean alongX, boolean faceP, int baseX, int baseY, int baseZ) {
        int formatPosId = ship.getStateMinor(27);
        if (formatPosId < 0 || formatPosId > 5) {
            formatPosId = 0;
        }
        int[] targetPos = calculateTargetPosition(formatType, formatPosId, alongX, faceP, baseX, baseY, baseZ);
        findSafePosAndApplyGuard(ship, targetPos[0], targetPos[1], targetPos[2], baseX, baseY, baseZ);
    }

    private static int[] calculateTargetPosition(int formatType, int formatPosId, boolean alongX, boolean faceP, int x, int y, int z) {
        switch (formatType) {
            case 2: return nextDoubleLinePos(alongX, faceP, formatPosId, x, y, z);
            case 3: return nextDiamondPos(alongX, faceP, formatPosId, x, y, z);
            case 5: return nextLineAbreastPos(alongX, formatPosId, x, y, z);
            default: return new int[]{x, y, z};
        }
    }

    private static void findSafePosAndApplyGuard(BasicEntityShip ship, int targetX, int targetY, int targetZ, int fallbackX, int fallbackY, int fallbackZ) {
        int[] safePos = BlockHelper.getSafeBlockWithin5x5(ship.world, targetX, targetY, targetZ);
        if (safePos != null) {
            applyShipGuard(ship, safePos[0], safePos[1], safePos[2], true);
        } else {
            applyShipGuard(ship, fallbackX, fallbackY, fallbackZ, true);
        }
    }

    public static int[] nextLineAheadPos(boolean alongX, boolean faceP, int x, int y, int z) {
        int[] pos = {x, y, z};
        int offset = faceP ? -3 : 3;
        if (alongX) {
            pos[0] += offset;
        } else {
            pos[2] += offset;
        }
        return pos;
    }

    public static int[] nextDoubleLinePos(boolean alongX, boolean faceP, int formatPos, int x, int y, int z) {
        int[] pos = {x, y, z};
        switch (formatPos) {
            case 1: if (alongX) pos[2] += 3; else pos[0] += 3; break;
            case 2: if (alongX) pos[0] += faceP ? 3 : -3; else pos[2] += faceP ? 3 : -3; break;
            case 3: if (alongX) { pos[0] += faceP ? 3 : -3; pos[2] += 3; } else { pos[0] += 3; pos[2] += faceP ? 3 : -3; } break;
            case 4: if (alongX) pos[0] += faceP ? -3 : 3; else pos[2] += faceP ? -3 : 3; break;
            case 5: if (alongX) { pos[0] += faceP ? -3 : 3; pos[2] += 3; } else { pos[0] += 3; pos[2] += faceP ? -3 : 3; } break;
            default:
        }
        return pos;
    }

    public static int[] nextDiamondPos(boolean alongX, boolean faceP, int formatPos, int x, int y, int z) {
        int[] pos = {x, y, z};
        switch (formatPos) {
            case 1: if (alongX) pos[0] += faceP ? 5 : -5; else pos[2] += faceP ? 5 : -5; break;
            case 2: if (alongX) { pos[0] += faceP ? 1 : -1; pos[2] -= 4; } else { pos[0] -= 4; pos[2] += faceP ? 1 : -1; } break;
            case 3: if (alongX) { pos[0] += faceP ? 1 : -1; pos[2] += 4; } else { pos[0] += 4; pos[2] += faceP ? 1 : -1; } break;
            case 4: if (alongX) pos[0] += faceP ? -3 : 3; else pos[2] += faceP ? -3 : 3; break;
            case 5: if (alongX) pos[0] += faceP ? 2 : -2; else pos[2] += faceP ? 2 : -2; break;
            default:
        }
        return pos;
    }

    public static int[] nextEchelonPos(boolean faceP, int x, int y, int z) {
        int[] pos = {x, y, z};
        int offset = faceP ? -2 : 2;
        pos[0] += offset;
        pos[2] += offset;
        return pos;
    }

    public static int[] nextLineAbreastPos(boolean alongX, int formatPos, int x, int y, int z) {
        int[] pos = {x, y, z};
        int offset;
        switch (formatPos) {
            case 1: offset = 3; break;
            case 2: offset = -3; break;
            case 3: offset = 6; break;
            case 4: offset = -6; break;
            case 5: offset = 9; break;
            default: offset = 0; break;
        }
        if (alongX) pos[2] += offset; else pos[0] += offset;
        return pos;
    }

    public static boolean[] getFormationDirection(double toX, double toZ, double fromX, double fromZ) {
        boolean[] face = new boolean[2];
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        face[0] = CalcHelper.isAbsGreater(dx, dz);
        face[1] = face[0] ? (dx >= 0.0) : (dz >= 0.0);
        return face;
    }

    public static double[] getFormationGuardingPos(IShipAttackBase host, Entity target, double oldX, double oldZ) {
        int formatID = host.getStateMinor(26);
        if (formatID <= 0) return new double[]{target.posX, target.posY, target.posZ};

        int formatPos = host.getStateMinor(27);
        if (formatPos < 0 || formatPos > 5) formatPos = 0;

        double[] currentPos = {target.posX, target.posY, target.posZ};
        boolean[] faceXP = getFormationDirection(target.posX, target.posZ, oldX, oldZ);
        int[] tempPos = calcFormationPos(formatID, formatPos, currentPos, faceXP);
        int[] safePos = BlockHelper.getSafeBlockWithin5x5(target.world, tempPos[0], tempPos[1], tempPos[2]);
        if (safePos.length >= 3) {
            return new double[]{safePos[0], safePos[1], safePos[2]};
        }
        return currentPos;
    }

    public static int[] calcFormationPos(int formatID, int formatPos, double[] flagshipPos, boolean[] faceXP) {
        int[] newPos = {MathHelper.floor(flagshipPos[0]), (int) (flagshipPos[1] + 0.5), MathHelper.floor(flagshipPos[2])};
        if (formatPos == 0) return newPos;

        switch (formatID) {
            case 1:
                for (int i = 0; i < formatPos; ++i) newPos = nextLineAheadPos(faceXP[0], faceXP[1], newPos[0], newPos[1], newPos[2]);
                break;
            case 4:
                for (int i = 0; i < formatPos; ++i) newPos = nextEchelonPos(faceXP[1], newPos[0], newPos[1], newPos[2]);
                break;
            case 2:
                newPos = nextDoubleLinePos(faceXP[0], faceXP[1], formatPos, newPos[0], newPos[1], newPos[2]);
                break;
            case 3:
                newPos = nextDiamondPos(faceXP[0], faceXP[1], formatPos, newPos[0], newPos[1], newPos[2]);
                break;
            case 5:
                newPos = nextLineAbreastPos(faceXP[0], formatPos, newPos[0], newPos[1], newPos[2]);
                break;
            default:
        }
        return newPos;
    }

    public static void applyShipGuard(BasicEntityShip ship, int x, int y, int z, boolean forceSet) {
        if (ship == null) return;
        if (!forceSet && ship.getGuardedPos(0) == x && ship.getGuardedPos(1) == y && ship.getGuardedPos(2) == z && ship.getGuardedPos(3) == ship.world.provider.getDimension()) {
            ship.setGuardedPos(-1, -1, -1, 0, 0);
            ship.setGuardedEntity(null);
            ship.setStateFlag(11, true);
        } else {
            ship.setSitting(false);
            ship.setGuardedEntity(null);
            ship.setGuardedPos(x, y, z, ship.world.provider.getDimension(), 1);
            ship.setStateFlag(11, false);
            if (!ship.getStateFlag(2)) {
                ship.applyEmotesReaction(5);
                Entity ridingEntity = ship.getRidingEntity();
                if (ridingEntity instanceof BasicEntityMount) {
                    ((BasicEntityMount) ridingEntity).getShipNavigate().tryMoveToXYZ(x, y, z, 1.0);
                    ((BasicEntityMount) ridingEntity).getLookHelper().setLookPosition(x, y, z, 30.0f, 40.0f);
                } else {
                    ship.getShipNavigate().tryMoveToXYZ(x, y, z, 1.0);
                    ship.getLookHelper().setLookPosition(x, y, z, 30.0f, 40.0f);
                }
            }
        }
    }

    public static void applyShipGuardEntity(BasicEntityShip ship, Entity guarded) {
        if (ship == null || guarded == null) return;
        Entity currentGuarded = ship.getGuardedEntity();
        if (currentGuarded != null && currentGuarded.getEntityId() == guarded.getEntityId()) {
            ship.setGuardedPos(-1, -1, -1, 0, 0);
            ship.setGuardedEntity(null);
            ship.setStateFlag(11, true);
        } else {
            ship.setSitting(false);
            ship.setGuardedPos(-1, -1, -1, guarded.world.provider.getDimension(), 2);
            ship.setGuardedEntity(guarded);
            ship.setStateFlag(11, false);
            if (!ship.getStateFlag(2)) {
                ship.applyEmotesReaction(5);
                Entity ridingEntity = ship.getRidingEntity();
                if (ridingEntity instanceof BasicEntityMount) {
                    ((BasicEntityMount) ridingEntity).getShipNavigate().tryMoveToEntityLiving(guarded, 1.0);
                } else {
                    ship.getShipNavigate().tryMoveToEntityLiving(guarded, 1.0);
                }
            }
        }
    }

    private static void applyToNearbyTeamShips(EntityPlayer player, int teamMeta, Consumer<BasicEntityShip> action) {
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
        if (capa == null) return;
        int playerDim = player.world.provider.getDimension();
        List<BasicEntityShip> ships = capa.getShipEntityByMode(teamMeta);
        ships.stream()
                .filter(ship -> ship != null && ship.world.provider.getDimension() == playerDim && player.getDistanceSq(ship) < 4096.0)
                .forEach(action);
    }

    public static void applyTeamAttack(EntityPlayer player, int meta, Entity target) {
        applyToNearbyTeamShips(player, meta, ship -> {
            ship.setSitting(false);
            ship.setEntityTarget(target);
            ship.applyEmotesReaction(5);
        });
    }

    public static void applyTeamSit(int[] parms) {
        EntityPlayer player = EntityHelper.getEntityPlayerByID(parms[0], parms[1], false);
        if (player == null) return;
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
        if (capa == null) return;

        int shipUID = parms[3];
        if (capa.checkIsInCurrentTeam(shipUID) < 0) {
            BasicEntityShip targetShip = EntityHelper.getShipByUID(shipUID);
            if (targetShip != null) {
                targetShip.setEntitySit(!targetShip.isSitting());
                targetShip.setRiderAndMountSit();
            }
        } else {
            List<BasicEntityShip> ships = capa.getShipEntityByMode(parms[2]);
            if (!ships.isEmpty() && ships.get(0) != null) {
                boolean shouldSit = !ships.get(0).isSitting();
                applyToNearbyTeamShips(player, parms[2], s -> {
                    s.setEntitySit(shouldSit);
                    s.setRiderAndMountSit();
                });
            }
        }
    }

    public static void applyTeamGuard(int[] parms) {
        EntityPlayer player = EntityHelper.getEntityPlayerByID(parms[0], parms[1], false);
        if (player == null) return;
        Entity target = EntityHelper.getEntityByID(parms[4], parms[1], false);
        if (target == null) return;
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
        if (capa == null) return;

        List<BasicEntityShip> ships = capa.getShipEntityByMode(parms[2]);
        if (ships.isEmpty()) return;

        int formatID = capa.getFormatIDCurrentTeam();
        int teamMode = parms[2];
        boolean useSimpleGuard = teamMode < 2 && formatID <= 0 || teamMode == 2 && (formatID <= 0 || ships.size() > 4);

        if (useSimpleGuard) {
            for (BasicEntityShip ship : ships) {
                if (ship != null && ship.world.provider.getDimension() == parms[1] && player.getDistanceSq(ship) < 4096.0) {
                    applyShipGuardEntity(ship, target);
                    CommonProxy.channelE.sendTo(new S2CEntitySync(ship, (byte) 3), (EntityPlayerMP) player);
                }
            }
        }
    }

    public static void applySummonShipsToDesk(EntityPlayer player, int[] parms) {
        if (!(player instanceof EntityPlayerMP)) return;
        World world = player.world;
        BlockPos deskPos = new BlockPos(parms[0], parms[1], parms[2]);
        IBlockState deskState = world.getBlockState(deskPos);
        if (!(deskState.getBlock() instanceof BlockDesk)) return;
        EnumFacing facing = deskState.getValue(BlockDesk.FACING);
        EnumFacing spawnDir = facing.getOpposite();
        EnumFacing leftDir = spawnDir.rotateYCCW();
        EnumFacing rightDir = spawnDir.rotateY();
        BlockPos refPos = deskPos.offset(spawnDir, 3).offset(leftDir, 1);
        BlockPos leftCheck = deskPos.offset(leftDir);
        if (world.getBlockState(leftCheck).getBlock() instanceof BlockDesk) {
            refPos = refPos.offset(leftDir, 1);
        }
        int totalShips = 0;
        for (int i = 3; i < parms.length; i++) {
            Entity e = world.getEntityByID(parms[i]);
            if (e instanceof BasicEntityShip) totalShips++;
        }
        final int maxPerRow = 4;
        final int horizontalSpacing = 1;
        final int depthSpacing = 1;
        int col = 0;
        int row = 0;
        for (int i = 3; i < parms.length; i++) {
            Entity entity = world.getEntityByID(parms[i]);
            if (!(entity instanceof BasicEntityShip)) continue;
            if (totalShips == 1) {
                col = 1;
            } else if (col >= maxPerRow) {
                row++;
                col = 0;
            }
            BlockPos spawnBlock = refPos.offset(rightDir, col * horizontalSpacing).offset(spawnDir, row * depthSpacing);
            double spawnX = spawnBlock.getX() + 0.5;
            double spawnY = deskPos.getY() + 1.0;
            double spawnZ = spawnBlock.getZ() + 0.5;
            BlockPos checkPos = new BlockPos(spawnX, spawnY, spawnZ);
            if (!world.isAirBlock(checkPos) || !world.isAirBlock(checkPos.up())) {
                row++;
                col = 0;
                spawnBlock = refPos.offset(rightDir, col * horizontalSpacing).offset(spawnDir, row * depthSpacing);
                spawnX = spawnBlock.getX() + 0.5;
                spawnZ = spawnBlock.getZ() + 0.5;
            }
            applyShipGuard((BasicEntityShip) entity, MathHelper.floor(spawnX), (int) spawnY, MathHelper.floor(spawnZ), false);
            CommonProxy.channelE.sendTo(new S2CEntitySync(entity, (byte) 3), (EntityPlayerMP) player);
            col++;
        }
    }

    public static void applyTeamMove(int[] parms) {
        EntityPlayer player = EntityHelper.getEntityPlayerByID(parms[0], parms[1], false);
        if (player == null) return;
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
        if (capa == null) return;
        List<BasicEntityShip> ships = capa.getShipEntityByMode(parms[2]);
        if (ships.isEmpty()) return;
        int moveType = parms[3];
        Consumer<BasicEntityShip> dismountIfNeeded = ship -> {
            if (ship.isRiding() && ship.getRidingEntity() instanceof EntitySeat) {
                ship.dismountRidingEntity();
            }
        };
        if (moveType == 2 && ships.size() >= 5) {
            float maxspd = 10;
            for (BasicEntityShip ship : ships) {
                ship.setStateFlag(27, true);
                if(ship.getMoveSpeed() < maxspd){
                    maxspd = ship.getMoveSpeed();
                }
            }
            int teamID = capa.getCurrentTeamID();
            spawnControllerAtFlagship(player, teamID, new Vec3d(parms[4], parms[5], parms[6]), maxspd);
        } else {
            int formatID = capa.getFormatIDCurrentTeam();
            int teamMode = parms[2];
            if(ships.get(0).getStateFlag(27)){
                String key = EntityHelper.getPlayerUID(player) + "_" + capa.getCurrentTeamID();
                removeOldController(player, key);
                for (BasicEntityShip ship : ships) {
                    ship.setStateFlag(27, false);
                }
            }
            if (teamMode == 1 && formatID > 0 && ships.size() > 4) {
                ships.forEach(dismountIfNeeded);
                applyFormationMoving(ships, formatID, parms[4], parms[5], parms[6], true);
            } else {
                for (BasicEntityShip ship : ships) {
                    if (ship != null && ship.world.provider.getDimension() == parms[1] && player.getDistanceSq(ship) < 4096.0) {
                        dismountIfNeeded.accept(ship);
                        applyShipGuard(ship, parms[4], parms[5], parms[6], false);
                        CommonProxy.channelE.sendTo(new S2CEntitySync(ship, (byte) 3), (EntityPlayerMP) player);
                    }
                }
            }
        }
    }

    public static void applyTeamSelect(int[] parms) {
        EntityPlayer player = EntityHelper.getEntityPlayerByID(parms[0], parms[1], false);
        if (player == null) return;
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
        if (capa == null) return;

        int shipIndex = capa.checkIsInCurrentTeam(parms[3]);
        if (shipIndex >= 0) {
            switch (parms[2]) {
                case 0:
                    capa.clearSelectStateCurrentTeam();
                    capa.setSelectStateCurrentTeam(shipIndex, true);
                    break;
                case 1:
                    capa.setSelectStateCurrentTeam(shipIndex, !capa.getSelectStateCurrentTeam(shipIndex));
                    break;
                default:
            }
        }
        capa.sendSyncPacket(0);
    }
}