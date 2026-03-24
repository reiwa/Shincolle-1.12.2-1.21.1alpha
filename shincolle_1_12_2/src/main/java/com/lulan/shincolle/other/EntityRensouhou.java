package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntitySummon;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipSummonAttack;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.Objects;

public class EntityRensouhou
extends BasicEntitySummon {
    private BasicEntityShip host2;

    public EntityRensouhou(World world) {
        super(world);
        this.setSize(0.3f, 0.7f);
    }

    @Override
    public void initAttrs(IShipAttackBase host, Entity target, int scaleLevel, float ... par2) {
        this.host2 = (BasicEntityShip)host;
        this.host = host;
        this.atkTarget = target;
        this.setScaleLevel(scaleLevel);
        this.posX = this.host2.posX + this.rand.nextDouble() * 3.0 - 1.5;
        this.posY = this.host2.posY + 0.0;
        this.posZ = this.host2.posZ + this.rand.nextDouble() * 3.0 - 1.5;
        if (!BlockHelper.checkBlockSafe(this.world, (int)this.posX, (int)this.posY, (int)this.posZ)) {
            this.posX = this.host2.posX;
            this.posY = this.host2.posY;
            this.posZ = this.host2.posZ;
        }
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.setPosition(this.posX, this.posY, this.posZ);
        this.shipAttrs = Attrs.copyAttrs(host.getAttrs());
        this.shipAttrs.setAttrsBuffed(0, host.getLevel() + host.getAttrs().getAttrsBuffed(0) * 0.2f);
        this.shipAttrs.setAttrsBuffed(1, host.getAttrs().getAttackDamage());
        this.shipAttrs.setAttrsBuffed(2, host.getAttrs().getAttackDamageHeavy());
        this.shipAttrs.setAttrsBuffed(3, host.getAttrs().getAttackDamageAir());
        this.shipAttrs.setAttrsBuffed(4, host.getAttrs().getAttackDamageAirHeavy());
        this.shipAttrs.setAttrsBuffed(5, host.getAttrs().getDefense() * 0.5f);
        this.shipAttrs.setAttrsBuffed(6, host.getAttrs().getAttackSpeed() * 0.75f - 0.25f);
        this.shipAttrs.setAttrsBuffed(7, host.getAttrs().getMoveSpeed() * 0.2f + 0.4f);
        this.shipAttrs.setAttrsBuffed(8, host.getAttrs().getAttackRange() + 1.0f);
        this.shipAttrs.setAttrsBuffed(15, this.shipAttrs.getAttrsBuffed(15) + 0.2f);
        this.getEntityAttribute(MAX_HP).setBaseValue(this.shipAttrs.getAttrsBuffed(0));
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(this.shipAttrs.getAttrsBuffed(7));
        this.getEntityAttribute(SWIM_SPEED).setBaseValue(this.shipAttrs.getAttrsBuffed(7));
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.5);
        if (this.getHealth() < this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
        this.shipNavigator = new ShipPathNavigate(this);
        this.shipMoveHelper = new ShipMoveHelper(this, 60.0f);
        this.setAIList();
    }

    @Override
    protected void setAIList() {
        this.clearAITasks();
        this.clearAITargetTasks();
        this.tasks.addTask(1, new EntityAIShipRangeAttack(this));
        this.setEntityTarget(this.atkTarget);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) {
            double motX = this.posX - this.prevPosX;
            double motZ = this.posZ - this.prevPosZ;
            if (motX != 0.0 || motZ != 0.0) {
                ParticleHelper.spawnAttackParticleAt(this.posX + motX * 1.5, this.posY, this.posZ + motZ * 1.5, -motX * 0.5, 0.0, -motZ * 0.5, (byte)15);
            }
        }
    }

    @Override
    public boolean getStateFlag(int flag) {
        if(flag == 12){
            if (this.host != null) {
                return this.host.getStateFlag(flag);
            }
        } else {
            return true;
        }
        return false;
    }

    @Override
    public void setStateFlag(int id, boolean flag) {
        this.headTilt = flag;
    }

    @Override
    public float getAttackBaseDamage(int type, Entity target) {
        return CombatHelper.modDamageByAdditionAttrs(this.host, target, this.shipAttrs.getAttackDamage(), 0);
    }

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        boolean isTargetHurt;
        if (this.host2 == null) {
            this.setDead();
            return false;
        }
        if (this.numAmmoLight > 0) {
            --this.numAmmoLight;
            if (this.numAmmoLight <= 0) {
                this.setDead();
            }
        }
        float atk = this.getAttackBaseDamage(1, target);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.applySoundAtAttacker(1);
        this.applyParticleAtAttacker(1, distVec);
        atk = CombatHelper.applyCombatRateToDamage(this, true, (float)distVec.d, atk);
        atk = CombatHelper.applyDamageReduceOnPlayer(target, atk);
        if (!TeamHelper.doFriendlyFire(this, target)) {
            atk = 0.0f;
        }
        if (isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this).setProjectile(), atk)) {
            if (!TeamHelper.checkSameOwner(this.getHostEntity(), target)) {
                BuffHelper.applyBuffOnTarget(target, this.getAttackEffectMap());
            }
            this.applySoundAtTarget(1);
            this.applyParticleAtTarget(1, target);
            if (ConfigHandler.canFlare) {
                this.host2.flareTarget(target);
            }
        }
        return isTargetHurt;
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        return false;
    }

    @Override
    public boolean useAmmoLight() {
        return true;
    }

    @Override
    public boolean useAmmoHeavy() {
        return false;
    }

    @Override
    public int getPlayerUID() {
        if (this.host != null) {
            return this.host.getPlayerUID();
        }
        return -1;
    }

    @Override
    public void setPlayerUID(int uid) {
    }

    @Override
    public int getDamageType() {
        return 5;
    }

    @Override
    public int getTextureID() {
        return 6;
    }

    @Override
    protected void returnSummonResource() {
        int numR;
        this.numAmmoLight -= 2;
        if (this.numAmmoLight < 0) {
            this.numAmmoLight = 0;
        }
        if ((numR = ((IShipSummonAttack) this.host).getNumServant()) < 6) {
            ((IShipSummonAttack) this.host).setNumServant(numR + 1);
        }
        this.host.setAmmoLight(this.host.getAmmoLight() + this.getAmmoLight() * ((BasicEntityShip)this.host).getAmmoConsumption());
    }

    @Override
    protected void setSizeWithScaleLevel() {
    }

    @Override
    protected void setAttrsWithScaleLevel() {
    }

    @Override
    public float getJumpSpeed() {
        return 1.5f;
    }
}
