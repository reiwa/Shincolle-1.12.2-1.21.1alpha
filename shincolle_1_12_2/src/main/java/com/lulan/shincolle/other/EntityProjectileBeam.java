package com.lulan.shincolle.entity.other;

import com.lulan.shincolle.client.render.IShipCustomTexture;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipAttrs;
import com.lulan.shincolle.entity.IShipOwner;
import com.lulan.shincolle.entity.IShipProjectile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModSounds;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.unitclass.Attrs;
import com.lulan.shincolle.utility.*;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.ArrayList;
import java.util.List;

public class EntityProjectileBeam
extends Entity
implements IShipOwner,
IShipAttrs,
IShipCustomTexture,
IShipProjectile {
    private IShipAttackBase host;
    private Entity host2;
    private int playerUID;
    private int type;
    private int lifeLength;
    private float atk;
    private float accX;
    private float accY;
    private float accZ;
    private final ArrayList<Entity> damagedTarget;

    public EntityProjectileBeam(World world) {
        super(world);
        this.setSize(1.0f, 1.0f);
        this.ignoreFrustumCheck = true;
        this.noClip = true;
        this.stepHeight = 0.0f;
        this.damagedTarget = new ArrayList<>();
    }

    public void initAttrs(IShipAttackBase host, int type, float ax, float ay, float az, float atk) {
        this.host = host;
        this.host2 = (Entity)host;
        this.playerUID = host.getPlayerUID();
        this.type = type;
        float acc;
        if(type == 1){
            this.setPosition(this.host2.posX, this.host2.posY + this.host2.height * 0.75, this.host2.posZ);
            this.lifeLength = 8;
            acc = 3.0f;
        } else {
            this.setPosition(this.host2.posX + ax, this.host2.posY + this.host2.height * 0.5, this.host2.posZ + az);
            this.lifeLength = 31;
            acc = 4.0f;
        }
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        accX = ax * acc;
        accY = ay * acc;
        accZ = az * acc;
        this.atk = atk;
    }

    @Override
    public int getPlayerUID() {
        return this.playerUID;
    }

    @Override
    public void setPlayerUID(int uid) {
    }

    @Override
    public Entity getHostEntity() {
        return this.host2;
    }

    protected void entityInit() {
    }

    protected void readEntityFromNBT(NBTTagCompound nbt) {
    }

    protected void writeEntityToNBT(NBTTagCompound nbt) {
    }

    public boolean isEntityInvulnerable(DamageSource attacker) {
        return true;
    }

    public boolean canBeCollidedWith() {
        return false;
    }

    public boolean canBePushed() {
        return false;
    }

    public void onUpdate() {
        this.motionX = accX;
        this.motionY = accY;
        this.motionZ = accZ;
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;
        this.setPosition(this.posX, this.posY, this.posZ);
        super.onUpdate();
        if (!this.world.isRemote) {
            if (this.ticksExisted > this.lifeLength || this.host == null) {
                this.setDead();
                return;
            }
            if (this.ticksExisted == 1) {
                NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
                CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, this.type, (byte)80), point);
            }
            List<Entity> hitList = this.world.getEntitiesWithinAABB(Entity.class, this.getEntityBoundingBox().expand(1.5, 1.5, 1.5));
            for (Entity ent : hitList) {
                if (!ent.canBeCollidedWith() || !EntityHelper.isNotHost(this, ent) || TargetHelper.isEntityInvulnerable(ent)) continue;
                boolean attacked = false;
                for (Entity ent2 : this.damagedTarget) {
                    if (!ent.equals(ent2)) continue;
                    attacked = true;
                    break;
                }
                if (attacked) continue;
                this.damagedTarget.add(ent);
                this.onImpact(ent);
            }
        } else {
            ParticleHelper.spawnAttackParticleAtEntity(this, 0.0, (double)32 - this.ticksExisted, 0.0, (byte)4);
        }
    }

    public boolean attackEntityFrom(DamageSource attacker, float atk) {
        return false;
    }

    protected void onImpact(Entity target) {
        if (this.host == null) {
            return;
        }
        this.playSound(ModSounds.SHIP_EXPLODE, ConfigHandler.volumeFire * 1.5f, 0.7f / (this.rand.nextFloat() * 0.4f + 0.8f));
        float beamAtk = this.atk;
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64.0);
        beamAtk = CombatHelper.modDamageByAdditionAttrs(this.host, target, beamAtk, 1);
        if (TeamHelper.checkSameOwner(this.host2, target)) {
            beamAtk = 0.0f;
        } else {
            beamAtk = CombatHelper.applyCombatRateToDamage(this.host, false, 1.0f, beamAtk);
            beamAtk = CombatHelper.applyDamageReduceOnPlayer(target, beamAtk);
            if (!TeamHelper.doFriendlyFire(this.host, target)) {
                beamAtk = 0.0f;
            }
        }
        if (target.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this.host2).setExplosion(), beamAtk)) {
            if (!TeamHelper.checkSameOwner(this.getHostEntity(), target)) {
                BuffHelper.applyBuffOnTarget(target, this.host.getAttackEffectMap());
            }
            CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 9, false), point);
        }
    }

    @Override
    public int getTextureID() {
        return -1;
    }

    @Override
    public Attrs getAttrs() {
        return this.host.getAttrs();
    }

    @Override
    public void setAttrs(Attrs data) {
    }

    @Override
    public void setProjectileType(int type) {
        this.type = type;
    }
}
