package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.IShipAttackBase;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;

public class EntityAIShipSkillAttack
extends EntityAIBase {
    private final IShipAttackBase host;
    private final EntityLiving host2;

    public EntityAIShipSkillAttack(IShipAttackBase host) {
        if (host == null) {
            throw new IllegalArgumentException("RangeAttack AI requires interface IShipCannonAttack");
        }
        this.host = host;
        this.host2 = (EntityLiving)host;
        this.setMutexBits(15);
    }

    public boolean shouldExecute() {
        if (this.host != null) {
            if (this.host.getIsSitting() || this.host.getStateMinor(43) > 0) {
                if (this.host.getStateEmotion(5) > 0) {
                    this.host.setStateEmotion(5, 0, true);
                }
                return false;
            }
            if (this.host.getIsRiding() && this.host2.getRidingEntity() instanceof BasicEntityMount) {
                return false;
            }
            return this.host.getStateEmotion(5) > 0;
        }
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.shouldExecute();
    }

    @Override
    public void updateTask() {
        if (this.host != null) {
            this.host.updateSkillAttack(this.host.getEntityTarget());
        }
    }
}
