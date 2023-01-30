package com.talhanation.workers.entities.ai;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.entities.LumberjackEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.EnumSet;
import java.util.Random;

public class LumberjackAI extends Goal {
    private final LumberjackEntity lumber;
    private BlockPos workPos;

    public LumberjackAI(LumberjackEntity lumber) {
        this.lumber = lumber;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        // Stop AI while following the player.
        if (this.lumber.getFollow()) {
            return false;
        }
        // Stop AI to at night, so SleepGoal can start.
        if (!this.lumber.level.isDay()) {
            return false;
        }
        // Stop AI if inventory is full, so TransferItemsInChestGoal can start.
        if (this.lumber.hasFullInventory()) {
            return false;
        }
        // Stop AI if work position is not set.
        BlockPos initialPosition = this.lumber.getStartPos();
        if (initialPosition == null) {
            return false;
        }
        // Start AI if there are trees near the work place.
        return this.getWoodPos() != null;
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.workPos = lumber.getStartPos();
        lumber.resetWorkerParameters();
    }

    public void tick() {
        breakLeaves();

        // TODO: Add memories of initial saplings/trees around the work position. 
        // TODO: Replant if the blocks are AIR.

        // Go back to assigned work position.
        if (workPos != null && !workPos.closerThan(lumber.getOnPos(), 10D) && !lumber.getFollow()) {
            this.lumber.getNavigation().moveTo(workPos.getX(), workPos.getY(), workPos.getZ(), 1);
            return;
        }

        // If near work position, start chopping.
        BlockPos chopPos = getWoodPos();
        if (chopPos == null) return;

        BlockPos lumberPos = lumber.getOnPos();
        this.lumber.getNavigation().moveTo(chopPos.getX(), chopPos.getY(), chopPos.getZ(), 1);
        this.lumber.getLookControl().setLookAt(chopPos.getX(), chopPos.getY() + 1, chopPos.getZ(), 10.0F, (float) this.lumber.getMaxHeadXRot());

        boolean standingBelowChopPos = (
            lumberPos.getX() == chopPos.getX() && 
            lumberPos.getZ() == chopPos.getZ() && 
            lumberPos.getY() < chopPos.getY()
        );

        if (
            chopPos.closerThan(lumber.getOnPos(), 9) ||
            standingBelowChopPos
        ) {
            this.mineBlock(chopPos);

            if (
                lumber.level.getBlockState(chopPos.below()).is(Blocks.DIRT) && 
                this.lumber.level.isEmptyBlock(chopPos)
            ) {
                plantSaplingFromInv(chopPos);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void breakLeaves() {
        AABB boundingBox = this.lumber.getBoundingBox();
        double offset = 0.25D;
        BlockPos start = new BlockPos(boundingBox.minX - offset, boundingBox.minY - offset, boundingBox.minZ - offset);
        BlockPos end = new BlockPos(boundingBox.maxX + offset, (boundingBox.maxY + offset) * 2, boundingBox.maxZ + offset);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean hasBroken = false;
        if (this.lumber.level.hasChunksAt(start, end)) {
            for (int i = start.getX(); i <= end.getX(); ++i) {
                for (int j = start.getY(); j <= end.getY(); ++j) {
                    for (int k = start.getZ(); k <= end.getZ(); ++k) {
                        pos.set(i, j, k);
                        BlockState blockstate = this.lumber.level.getBlockState(pos);
                        if (blockstate.getBlock() instanceof LeavesBlock) {
                            this.lumber.level.destroyBlock(pos, true, this.lumber);
                            hasBroken = true;
                        }
                    }
                }
            }
        }

        if (hasBroken) {
            this.lumber.level.playSound(null, this.lumber.getX(), this.lumber.getY(), this.lumber.getZ(), SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1F, 0.9F + 0.2F);
            this.lumber.workerSwingArm();
        }
    }

    public BlockPos getWoodPos() {
        int range = 16;

        for (int j = 0; j < range; j++) {
            for (int i = 0; i < range; i++) {
                for(int k = 0; k < range * 2; k++){
                    BlockPos blockPos = this.lumber.getStartPos().offset(j - range / 2F, k, i - range / 2F);

                    BlockState blockState = this.lumber.level.getBlockState(blockPos);
                    if (this.lumber.wantsToBreak(blockState.getBlock())) {
                        return blockPos;
                    }
                }
            }
        }
        return null;
    }

    public BlockPos getPlantPos() {
        int range = 16;
        Random random = new Random();
        for (int j = 0; j < range; j++) {
            BlockPos blockPos = this.lumber.getStartPos().offset(random.nextInt(10) - range / 2F, 1, random.nextInt(10) - range / 2F);


            if (this.lumber.level.isEmptyBlock(blockPos) && lumber.level.getBlockState(blockPos.below()).is(Blocks.GRASS_BLOCK)) {
                return blockPos;
            }
        }
        return null;
    }


    private void plantSaplingFromInv(BlockPos blockPos) {
        SimpleContainer inventory = lumber.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack itemstack = inventory.getItem(i);
            if (!itemstack.isEmpty() && itemstack.is(ItemTags.SAPLINGS)) {
                BlockState placedSaplingBlock = Block.byItem(itemstack.getItem()).defaultBlockState();
                this.lumber.level.setBlock(blockPos, placedSaplingBlock, 3);
                lumber.level.playSound(null, (double) blockPos.getX(), (double) blockPos.getY(), (double) blockPos.getZ(), SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                itemstack.shrink(1);
                if (itemstack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                break;
            }
        }
    }

    private boolean mineBlock(BlockPos blockPos){
        if (this.lumber.isAlive() && ForgeEventFactory.getMobGriefingEvent(this.lumber.level, this.lumber) && !lumber.getFollow()) {

            BlockState blockstate = this.lumber.level.getBlockState(blockPos);
            Block block = blockstate.getBlock();
            ItemStack heldItem = this.lumber.getItemInHand(InteractionHand.MAIN_HAND);

            if (lumber.wantsToBreak(block)){
                if (lumber.getCurrentTimeBreak() % 5 == 4) {
                    lumber.level.playLocalSound(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockstate.getSoundType().getHitSound(), SoundSource.BLOCKS, 1F, 0.75F, false);
                }

                //set max destroy speed
                int bp = (int) (blockstate.getDestroySpeed(this.lumber.level, blockPos) * 30);
                this.lumber.setBreakingTime(bp);

                //increase current
                this.lumber.setCurrentTimeBreak(this.lumber.getCurrentTimeBreak() + (int) (1 * (heldItem.getDestroySpeed(blockstate))));
                float f = (float) this.lumber.getCurrentTimeBreak() / (float) this.lumber.getBreakingTime();

                int i = (int) (f * 10);

                if (i != this.lumber.getPreviousTimeBreak()) {
                    this.lumber.level.destroyBlockProgress(1, blockPos, i);
                    this.lumber.setPreviousTimeBreak(i);
                }

                if (this.lumber.getCurrentTimeBreak() >= this.lumber.getBreakingTime()) {
                    // Break the target block
                    this.lumber.level.destroyBlock(blockPos, true, this.lumber);
                    this.lumber.setCurrentTimeBreak(-1);
                    this.lumber.setBreakingTime(0);
                    // Damage the tool
                    heldItem.setDamageValue(heldItem.getDamageValue() + 1);
                    // Break the tool when it reaches max durability
                    if (!heldItem.is(Items.AIR) && heldItem.getDamageValue() >= heldItem.getMaxDamage()) {
                        this.lumber.broadcastBreakEvent(InteractionHand.MAIN_HAND);
                        this.lumber.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        this.lumber.stopUsingItem();
                        // Attempt to use the next available tool in inventory
                        this.lumber.upgradeTool();
                        if (this.lumber.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                            // If there are no more tools remaining, alert the owner
                            LivingEntity owner = this.lumber.getOwner();
                            if (owner != null) {
                                this.lumber.tellPlayer(owner, AbstractWorkerEntity.TEXT_OUT_OF_TOOLS(heldItem));
                            }
                        }
                     }
                    return true;
                }
                this.lumber.workerSwingArm();
            }
        }
        return false;
    }
}

