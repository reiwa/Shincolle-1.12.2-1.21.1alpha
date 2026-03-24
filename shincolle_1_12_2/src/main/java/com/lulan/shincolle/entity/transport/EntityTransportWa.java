package com.lulan.shincolle.entity.transport;

import com.lulan.shincolle.ai.EntityAIShipPickItem;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.utility.EmotionHelper;
import com.lulan.shincolle.utility.TeamHelper;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.List;

public class EntityTransportWa
extends BasicEntityShipSmall {
    public EntityTransportWa(World world) {
        super(world);
        this.setSize(0.7f, 1.53f);
        this.setStateMinor(19, 7);
        this.setStateMinor(20, 16);
        this.setStateMinor(25, 0);
        this.setStateMinor(13, 3);
        this.setGrudgeConsumption(ConfigHandler.consumeGrudgeShip[10]);
        this.setAmmoConsumption(ConfigHandler.consumeAmmoShip[10]);
        this.ModelPos = new float[]{-3.0f, 20.0f, 0.0f, 45.0f};
        this.StateFlag[13] = false;
        this.StateFlag[14] = false;
        this.StateFlag[15] = false;
        this.StateFlag[16] = false;
        this.StateFlag[24] = true;
        this.postInit();
    }

    @Override
    public EntityAgeable createChild(EntityAgeable ageable) {
        return null;
    }

    @Override
    public int getEquipType() {
        return 1;
    }

    @Override
    public void setAIList() {
        super.setAIList();
        this.tasks.addTask(5, new EntityAIShipPickItem(this, 8.0f));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            if (this.ticksExisted % 128 == 0 && this.rand.nextInt(4) == 0) {
                this.applyParticleEmotion(2);
            }
        } else if (this.ticksExisted % 128 == 0) {
            if (!this.isMorph) {
                if (this.getStateMinor(6) <= 5400) {
                    this.consumeSupplyItems(0);
                }
                if (this.getStateMinor(4) <= 540) {
                    this.consumeSupplyItems(1);
                }
                if (this.getStateMinor(5) <= 270) {
                    this.consumeSupplyItems(2);
                }
            }
            if (this.ticksExisted % 256 == 0 && !this.getStateFlag(2)) {
                int supCount = this.getLevel() / 50 + 1;
                double range = 2.0 + this.getAttrs().getAttackRange() * 0.5;
                boolean canSupply = false;
                List<BasicEntityShip> slist = null;
                NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
                slist = this.world.getEntitiesWithinAABB(BasicEntityShip.class, this.getEntityBoundingBox().expand(range, range, range));
                for (BasicEntityShip s : slist) {
                    if (supCount <= 0) break;
                    if (!TeamHelper.checkSameOwner(this, s)) continue;
                    if (this.getStateMinor(6) > 5400 && s.getStateMinor(6) < 2700) {
                        canSupply = true;
                        this.addGrudge(-5400);
                        s.addGrudge((int)(5400.0f * s.getAttrs().getAttrsBuffed(17)));
                    }
                    if (this.getStateMinor(4) >= 540 && s.getStateMinor(4) < 270) {
                        canSupply = true;
                        this.addAmmoLight(-540);
                        s.addAmmoLight((int)(540.0f * s.getAttrs().getAttrsBuffed(18)));
                    }
                    if (this.getStateMinor(5) >= 270 && s.getStateMinor(5) < 135) {
                        canSupply = true;
                        this.addAmmoHeavy(-270);
                        s.addAmmoHeavy((int)(270.0f * s.getAttrs().getAttrsBuffed(18)));
                    }
                    if (!canSupply) continue;
                    this.addShipExp(ConfigHandler.expGain[6] * 20);
                    CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, s, 0.75, 0.0, 0.0, 4, false), point);
                    --supCount;
                }
            }
        }
    }

    private void consumeSupplyItems(int type) {
        switch (type) {
            case 0: {
                if (this.decrSupplies(4)) {
                    if (ConfigHandler.consumptionLevel == 0) {
                        this.addGrudge((int)(3000.0f * this.getAttrs().getAttrsBuffed(17)));
                        break;
                    }
                    this.addGrudge((int)(300.0f * this.getAttrs().getAttrsBuffed(17)));
                    break;
                }
                if (!this.decrSupplies(5)) break;
                if (ConfigHandler.consumptionLevel == 0) {
                    this.addGrudge((int)(27000.0f * this.getAttrs().getAttrsBuffed(17)));
                    break;
                }
                this.addGrudge((int)(2700.0f * this.getAttrs().getAttrsBuffed(17)));
                break;
            }
            case 1: {
                if (this.decrSupplies(0)) {
                    if (ConfigHandler.consumptionLevel == 0) {
                        this.addAmmoLight((int)(300.0f * this.getAttrs().getAttrsBuffed(18)));
                        break;
                    }
                    this.addAmmoLight((int)(30.0f * this.getAttrs().getAttrsBuffed(18)));
                    break;
                }
                if (!this.decrSupplies(2)) break;
                if (ConfigHandler.consumptionLevel == 0) {
                    this.addAmmoLight((int)(2700.0f * this.getAttrs().getAttrsBuffed(18)));
                    break;
                }
                this.addAmmoLight((int)(270.0f * this.getAttrs().getAttrsBuffed(18)));
                break;
            }
            case 2: {
                if (this.decrSupplies(1)) {
                    if (ConfigHandler.consumptionLevel == 0) {
                        this.addAmmoHeavy((int)(150.0f * this.getAttrs().getAttrsBuffed(18)));
                        break;
                    }
                    this.addAmmoHeavy((int)(15.0f * this.getAttrs().getAttrsBuffed(18)));
                    break;
                }
                if (!this.decrSupplies(3)) break;
                if (ConfigHandler.consumptionLevel == 0) {
                    this.addAmmoHeavy((int)(1350.0f * this.getAttrs().getAttrsBuffed(18)));
                    break;
                }
                this.addAmmoHeavy((int)(135.0f * this.getAttrs().getAttrsBuffed(18)));
                break;
            }
            default:
        }
    }

    @Override
    public void calcShipAttributesAddEquip() {
        super.calcShipAttributesAddEquip();
        this.StateMinor[36] = 2;
    }

    @Override
    public double getMountedYOffset() {
        if (EmotionHelper.checkModelState(1, this.getStateEmotion(0))) {
            if (this.isSitting()) {
                return this.height * 0.5f;
            }
            return this.height * 0.64f;
        }
        if (this.isSitting()) {
            return 0.0;
        }
        return this.height * 0.64f;
    }
}
