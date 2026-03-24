package com.lulan.shincolle.entity;

import com.google.common.base.Predicate;
import com.lulan.shincolle.ai.EntityAIShipAircraftAttack;
import com.lulan.shincolle.ai.EntityAIShipOpenDoor;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.entity.other.EntityAirplane;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.ClientProxy;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Dist4d;
import com.lulan.shincolle.reference.unitclass.MissileData;
import com.lulan.shincolle.utility.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.Collections;
import java.util.List;

public abstract class BasicEntityAirplane extends BasicEntitySummon implements IShipFlyable {

    private static final int LIFETIME_TICKS = 1200;
    private static final int HOST_CHECK_TIMEOUT = 20;
    private static final int INITIAL_BOOST_DURATION = 34;
    private static final double INITIAL_BOOST_SPEED = 0.375;
    private static final int TARGETING_INTERVAL = 16;
    private static final int RETURN_HOME_CHECK_INTERVAL = 16;
    private static final double MAX_RETURN_HOME_DIST_SQ = 4096.0;
    private static final double TARGETING_RANGE_NORMAL = 24.0;
    private static final double TARGETING_RANGE_AIR_ONLY = 32.0;
    private static final float STEP_HEIGHT = 7.0f;
    private static final float JUMP_SPEED = 2.0f;
    private static final int DEATH_TIME_BURNING = 30;
    private static final int DEATH_TIME_LAVA_PARTICLE = 89;
    private static final int DEATH_TIME_EXPLOSION = 90;
    private static final double GRAVITY_ON_DEATH = 0.08;
    private static final int AMMO_RETURN_PENALTY_LIGHT = 3;
    private static final int AMMO_RETURN_PENALTY_HEAVY = 1;

    protected boolean backHome = false;
    protected boolean canEntityFindTarget = true;
    protected TargetHelper.Sorter targetSorter = null;
    protected Predicate<Entity> targetSelector = null;
    protected Vec3d deadMotion = Vec3d.ZERO;

    protected BasicEntityAirplane(World world) {
        super(world);
        this.stepHeight = STEP_HEIGHT;
    }

    @Override
    protected void setAIList() {
        this.clearAITasks();
        this.clearAITargetTasks();
        this.tasks.addTask(1, new EntityAIShipAircraftAttack(this));
        this.tasks.addTask(11, new EntityAIShipOpenDoor(this, true));
        this.setEntityTarget(this.atkTarget);
    }

    @Override
    public void onUpdate() {
        if (this.world.isRemote) {
            updateClientLogic();
        } else {
            updateServerLogic();
        }

        super.onUpdate();
    }

    private void updateServerLogic() {
        if (this.host == null || this.getPlayerUID() <= 0 || !((Entity) this.host).isEntityAlive()) {
            this.setDead();
            return;
        }
        if (this.backHome) {
            handleReturnToHome();
        } else {
            handleInitialBoost();
            handleTargeting();
            checkLifetime();
        }
    }

    private void updateClientLogic() {
        if (this.ticksExisted % 2 == 0) {
            updateRotation();
        }
    }

    private void handleReturnToHome() {
        if (!this.isEntityAlive()) return;
        Entity hostEntity = (Entity) this.host;
        if (this.getDistanceSq(hostEntity) > Math.pow(2.0 + hostEntity.height, 2)) {
            if (this.ticksExisted % RETURN_HOME_CHECK_INTERVAL == 0) {
                this.getShipNavigate().tryMoveToXYZ(hostEntity.posX, hostEntity.posY + hostEntity.height + 1.0, hostEntity.posZ, 1.0);
                if (this.getShipNavigate().noPath() && this.getDistanceSq(hostEntity) >= MAX_RETURN_HOME_DIST_SQ) {
                    this.returnSummonResource();
                    this.setDead();
                }
            }
        } else {
            this.returnSummonResource();
            this.setDead();
        }
    }

    private void handleInitialBoost() {
        if (this.ticksExisted < INITIAL_BOOST_DURATION && this.getEntityTarget() != null) {
            double distX = this.getEntityTarget().posX - this.posX;
            double distZ = this.getEntityTarget().posZ - this.posZ;
            double distSqrt = MathHelper.sqrt(distX * distX + distZ * distZ);
            if (distSqrt > 1.0E-4) {
                this.motionX = distX / distSqrt * INITIAL_BOOST_SPEED;
                this.motionZ = distZ / distSqrt * INITIAL_BOOST_SPEED;
                this.motionY = 0.1;
            }
        }
    }

    private void handleTargeting() {
        if (this.ticksExisted % TARGETING_INTERVAL != 0 || !this.canFindTarget()) {
            return;
        }
        boolean findNewTarget = this.ticksExisted >= LIFETIME_TICKS || this.getEntityTarget() == null || !this.getEntityTarget().isEntityAlive();
        if (this.ticksExisted >= HOST_CHECK_TIMEOUT && findNewTarget) {
            findAndSetNewTarget();
        }
    }

    private void findAndSetNewTarget() {
        Entity newTarget = null;
        List<Entity> list = null;

        if (this.host.getStateFlag(19)) {
            list = this.world.getEntitiesWithinAABB(BasicEntityAirplane.class,
                    this.getEntityBoundingBox().expand(TARGETING_RANGE_AIR_ONLY, TARGETING_RANGE_AIR_ONLY, TARGETING_RANGE_AIR_ONLY),
                    this.targetSelector);
        }

        if (list == null || list.isEmpty()) {
            list = this.world.getEntitiesWithinAABB(Entity.class,
                    this.getEntityBoundingBox().expand(TARGETING_RANGE_NORMAL, TARGETING_RANGE_NORMAL, TARGETING_RANGE_NORMAL),
                    this.targetSelector);
        }

        if (list != null && !list.isEmpty()) {
            Collections.sort(list, this.targetSorter);
            newTarget = list.get(0);
        }

        if (list == null || list.isEmpty()) {
            newTarget = this.host.getEntityTarget();
        }

        if (newTarget != null && newTarget.isEntityAlive()) {
            this.setEntityTarget(newTarget);
            this.backHome = false;
        } else {
            this.setEntityTarget(null);
            this.backHome = true;
        }
    }

    private void checkLifetime() {
        if (this.ticksExisted >= LIFETIME_TICKS) {
            this.setEntityTarget(null);
            this.backHome = true;
        }
    }

    private void updateRotation() {
        float[] degree = CalcHelper.getLookDegree(this.posX - this.prevPosX, this.posY - this.prevPosY, this.posZ - this.prevPosZ, true);
        this.rotationYaw = degree[0];
        this.rotationPitch = degree[1];
    }

    @Override
    public void travel(float strafe, float forward, float vertical) {
        this.moveRelative(strafe, 0.0F, forward, this.shipAttrs.getMoveSpeed() * 0.4F);
        this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.91D;
        this.motionY *= 0.91D;
        this.motionZ *= 0.91D;

        if (this.collidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + 0.6D, this.motionZ)) {
            this.motionY += 0.2D;
        }

        this.prevLimbSwingAmount = this.limbSwingAmount;
        double dx = this.posX - this.prevPosX;
        double dz = this.posZ - this.prevPosZ;
        float f4 = MathHelper.sqrt(dx * dx + dz * dz) * 4F;

        if (f4 > 1F) {
            f4 = 1F;
        }

        this.limbSwingAmount += (f4 - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }

    @Override
    protected void onDeathUpdate() {
        ++this.deathTime;
        this.motionY -= GRAVITY_ON_DEATH;
        if (this.deathTime == 1) {
            this.deadMotion = new Vec3d(this.motionX, this.motionY, this.motionZ);
            this.motionX = this.deadMotion.x;
            this.motionZ = this.deadMotion.z;
        }
        if (this.world.isRemote) {
            spawnDeathParticles();
        }
        if (!this.world.isRemote && this.deathTime >= DEATH_TIME_EXPLOSION) {
            explodeAndDie();
        }
    }

    private void spawnDeathParticles() {
        if ((this.ticksExisted & 1) == 0) {
            int maxPar = (int) ((3 - ClientProxy.getMineraft().gameSettings.particleSetting) * 1.8F);
            double range = this.width * 0.5;
            for (int i = 0; i < maxPar; ++i) {
                ParticleHelper.spawnAttackParticleAt(this.posX - range + this.rand.nextDouble() * range * 2.0, this.posY + this.height * 0.3 + this.rand.nextDouble() * 0.3, this.posZ - range + this.rand.nextDouble() * range * 2.0, 1.5, 0.0, 0.0, (byte) 43);
            }
        }
        if (this.deathTime >= DEATH_TIME_LAVA_PARTICLE) {
            for (int i = 0; i < 12; ++i) {
                float ran1 = this.width * (this.rand.nextFloat() - 0.5F);
                float ran2 = this.width * (this.rand.nextFloat() - 0.5F);
                this.world.spawnParticle(EnumParticleTypes.LAVA, this.posX + ran1, this.posY + this.height * 0.3, this.posZ + ran2, 0.0, 0.0, 0.0);
                if ((i & 3) == 0) {
                    this.world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, this.posX + ran2, this.posY + this.height * 0.5, this.posZ + ran1, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    private void explodeAndDie() {
        this.playSound(ModSounds.SHIP_EXPLODE, ConfigHandler.volumeFire, 0.7F / (this.rand.nextFloat() * 0.4F + 0.8F));
        for (int k = 0; k < 20; ++k) {
            double d2 = this.rand.nextGaussian() * 0.02;
            double d0 = this.rand.nextGaussian() * 0.02;
            double d1 = this.rand.nextGaussian() * 0.02;
            this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, this.posX + (this.rand.nextFloat() * this.width * 2.0F) - this.width, this.posY + (this.rand.nextFloat() * this.height), this.posZ + (this.rand.nextFloat() * this.width * 2.0F) - this.width, d2, d0, d1);
        }
        this.setDead();
    }

    @Override
    public boolean attackEntityWithAmmo(Entity target) {
        if (this.host == null) {
            this.setDead();
            return false;
        }
        if (this.numAmmoLight > 0) {
            --this.numAmmoLight;
        }
        float atk = this.getAttackBaseDamage(1, target);
        this.applySoundAtAttacker(1);
        this.applyParticleAtAttacker(1);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        atk = CombatHelper.applyCombatRateToDamage(this, true, (float) distVec.d, atk);
        atk = CombatHelper.applyDamageReduceOnPlayer(target, atk);
        if (!TeamHelper.doFriendlyFire(this, target)) {
            atk = 0.0F;
        }
        boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this).setProjectile(), atk);
        if (isTargetHurt) {
            if (!TeamHelper.checkSameOwner(this.getHostEntity(), target)) {
                BuffHelper.applyBuffOnTarget(target, this.getAttackEffectMap());
            }
            this.applySoundAtTarget(1);
            this.applyParticleAtTarget(0, target);
            if (ConfigHandler.canFlare && this.host instanceof BasicEntityShip) {
                ((BasicEntityShip) this.host).flareTarget(target);
            }
        }
        return isTargetHurt;
    }

    @Override
    public boolean attackEntityWithHeavyAmmo(Entity target) {
        if (this.host == null) {
            this.setDead();
            return false;
        }
        if (this.numAmmoHeavy > 0) {
            --this.numAmmoHeavy;
        }
        float atk = this.getAttackBaseDamage(2, target);
        float kbValue = 0.15F;
        int moveType = CombatHelper.calcMissileMoveTypeForAirplane(this, target, 4);
        Dist4d distVec = CalcHelper.getDistanceFromA2B(this, target);
        this.applySoundAtAttacker(2);
        this.applyParticleAtAttacker(2, distVec);
        float tarX = (float) target.posX;
        float tarY = (float) target.posY;
        float tarZ = (float) target.posZ;
        if (this.rand.nextFloat() <= CombatHelper.calcMissRate(this, (float) distVec.d)) {
            tarX = tarX - 5.0F + this.rand.nextFloat() * 10.0F;
            tarY += this.rand.nextFloat() * 5.0F;
            tarZ = tarZ - 5.0F + this.rand.nextFloat() * 10.0F;
            ParticleHelper.spawnAttackTextParticle(this, 0);
        }
        MissileData md = this.getMissileData(4);
        float[] data = {atk, kbValue, (float) (this.posY - 0.8), tarX, tarY + target.height * 0.2F, tarZ, 160.0F, 0.0F, md.vel0, md.accY1, md.accY2};
        EntityAbyssMissile missile = new EntityAbyssMissile(this.world, this, md.type, moveType, data);
        this.world.spawnEntity(missile);
        this.applySoundAtTarget(2);
        this.applyParticleAtTarget(2, target);
        if (ConfigHandler.canFlare && this.host instanceof BasicEntityShip) {
            ((BasicEntityShip) this.host).flareTarget(target);
        }
        return true;
    }

    @Override
    protected void returnSummonResource() {
        if (!(this.host instanceof BasicEntityShipCV)) {
            return;
        }
        BasicEntityShipCV ship = (BasicEntityShipCV) this.host;
        this.numAmmoLight = Math.max(0, this.numAmmoLight - AMMO_RETURN_PENALTY_LIGHT);
        this.numAmmoHeavy = Math.max(0, this.numAmmoHeavy - AMMO_RETURN_PENALTY_HEAVY);
        ship.setStateMinor(4, ship.getStateMinor(4) + this.numAmmoLight * ship.getAmmoConsumption());
        ship.setStateMinor(5, ship.getStateMinor(5) + this.numAmmoHeavy * ship.getAmmoConsumption());
        if (this instanceof EntityAirplane) {
            ship.setNumAircraftLight(ship.getNumAircraftLight() + 1);
        } else {
            ship.setNumAircraftHeavy(ship.getNumAircraftHeavy() + 1);
        }
    }

    @Override
    public float getAttackBaseDamage(int type, Entity target) {
        switch (type) {
            case 1:
            case 3:
                return CombatHelper.modDamageByAdditionAttrs(this.host, target, this.shipAttrs.getAttackDamageAir(), 0);
            case 2:
            case 4:
                return this.shipAttrs.getAttackDamageAirHeavy();
            default:
                return this.shipAttrs.getAttackDamageAir() * 0.125f;
        }
    }

    public void applySoundAtAttacker(int type) {
        if(type == 1){
            this.playSound(ModSounds.SHIP_MACHINEGUN, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
        } else if(type == 2){
            this.playSound(ModSounds.SHIP_FIREHEAVY, ConfigHandler.volumeFire, this.getSoundPitch() * 0.85f);
        }
    }

    public void applyParticleAtAttacker(int type) {
        if (type == 1) {
            NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 8, false), point);
        }
    }

    @Override
    public boolean getStateFlag(int flag) {
        return flag != 12;
    }

    @Override
    public int getPlayerUID() {
        return this.host != null ? this.host.getPlayerUID() : -1;
    }

    @Override
    public int getDamageType() {
        return 7;
    }

    @Override
    public float getJumpSpeed() {
        return JUMP_SPEED;
    }

    @Override
    public boolean isBurning() {
        return this.deathTime > DEATH_TIME_BURNING;
    }

    protected void collideWithEntity(Entity target) {}
    public void fall(float distance, float damageMultiplier) {}
    protected void updateFallState(double y, boolean onGround, IBlockState state, BlockPos pos) {}
    public boolean isOnLadder() { return false; }
    @Override public boolean canFindTarget() { return this.canEntityFindTarget; }
    @Override public boolean canFly() { return true; }
    @Override public void setStateFlag(int id, boolean flag) {}
    @Override public void setPlayerUID(int uid) {}
    public boolean canBePushed() { return false; }
    @Override protected void setSizeWithScaleLevel() {}
    @Override protected void setAttrsWithScaleLevel() {}
    @Override protected int getLifeLength() { return LIFETIME_TICKS; }
}