package com.talhanation.workers.entities.ai.horse;

import com.talhanation.workers.entities.MerchantEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

public class HorseRiddenByMerchantGoal extends Goal {
    public final AbstractHorse horse;
    public boolean speedApplied;
    public int merchantSpeedState = -1;
    public MerchantEntity merchant;
    public HorseRiddenByMerchantGoal(AbstractHorse horse){
        this.horse = horse;
    }

    public boolean canUse() {
        if(horse.getControllingPassenger() instanceof MerchantEntity merchant){
            this.merchant = merchant;
            return true;
        }
        else return false;
    }

    @Override
    public void start() {
        super.start();
        speedApplied = false;
    }


    private void applyHorseSpeed(double merchantSpeed){
        AttributeInstance speedA = this.horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if ( speedA == null )
        {
            return;
        }
        double speed;
        if(this.horse.getPersistentData().contains("oldSpeed"))
        {
            speed = horse.getPersistentData().getDouble("oldSpeed");
        }
        else
        {
            speed = speedA.getValue();
            this.horse.getPersistentData().putDouble("oldSpeed", speed);
        }
        speedA.setBaseValue((0.225  + speed) * merchantSpeed);
        speedApplied = true;
    }

    @Override
    public void tick() {
        boolean merchantSpeedChanged = merchant.getTravelSpeedState() != merchantSpeedState;
        if(!speedApplied && merchantSpeedChanged){
            merchantSpeedState = merchant.getTravelSpeedState();
            double merchantSpeed;
            switch (merchantSpeedState){
                default -> merchantSpeed = 0.9F; 
                case 0 -> merchantSpeed = 0.65F; 
                case 2 -> merchantSpeed = 1.1F;
            }
            applyHorseSpeed(merchantSpeed);
        }
    }

    @Override
    public void stop() {
        super.stop();
        AttributeInstance speedA = this.horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if ( speedA != null )
        {
            double oldSpeed = horse.getPersistentData().getDouble("oldSpeed");
            speedA.setBaseValue(oldSpeed);
        }
    }
}
