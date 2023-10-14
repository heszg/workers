package com.talhanation.workers.entities;

import com.talhanation.workers.Main;
import com.talhanation.workers.config.WorkersModConfig;
import com.talhanation.workers.entities.ai.*;
import com.talhanation.workers.entities.ai.navigation.SailorPathNavigation;
import com.talhanation.workers.entities.ai.navigation.WorkersPathNavigation;
import com.talhanation.workers.inventory.WorkerHireContainer;
import com.talhanation.workers.network.MessageHireGui;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.talhanation.workers.Translatable.*;

public abstract class AbstractWorkerEntity extends AbstractChunkLoaderEntity {
    private static final EntityDataAccessor<Optional<BlockPos>> START_POS = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> DEST_POS = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> HOME = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> CHEST = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Optional<BlockPos>> BED = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> FOLLOW = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> IS_WORKING = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_PICKING_UP = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> breakingTime = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> currentTimeBreak = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> previousTimeBreak = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HUNGER = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> NEEDS_HOME = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NEEDS_CHEST = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NEEDS_BED = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> PROFESSION_NAME = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> FARMED_ITEMS = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> NEEDS_TOOL = SynchedEntityData.defineId(AbstractWorkerEntity.class, EntityDataSerializers.BOOLEAN);

    int hurtTimeStamp = 0;
    public boolean startPosChanged;

    public AbstractWorkerEntity(EntityType<? extends AbstractWorkerEntity> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 2;
        this.setMaxUpStep( 1.25F );
    }

    @Override
    @NotNull
    protected PathNavigation createNavigation(@NotNull Level level) {
        return new WorkersPathNavigation(this, level);
    }

    @Override
    @NotNull
    public PathNavigation getNavigation() {
        if (this instanceof IBoatController sailor && this.getVehicle() instanceof Boat) {
            return new SailorPathNavigation(sailor, this.getCommandSenderWorld());
        }
        else
            return super.getNavigation();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new WorkerUpkeepPosGoal(this));
        this.goalSelector.addGoal(0, new EatGoal(this));
        this.goalSelector.addGoal(0, new SleepGoal(this));
        this.goalSelector.addGoal(0, new WorkerFloatGoal(this));
        this.goalSelector.addGoal(0, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(1, new DepositItemsInChestGoal(this));
        this.goalSelector.addGoal(10, new WorkerMoveToHomeGoal<>(this));
        this.goalSelector.addGoal(0, new WorkerFollowOwnerGoal(this, 1.0F, 20.0F));

        //this.goalSelector.addGoal(14, new MoveBackToVillageGoal(this, 0.6D, false));
        //this.goalSelector.addGoal(10, new GolemRandomStrollInVillageGoal(this, 0.6D));
        //this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0F));
        this.goalSelector.addGoal(11, new WorkerLookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(12, new WorkerRandomLookAroundGoal(this));
        this.goalSelector.addGoal(10, new WorkerLookAtPlayerGoal(this, LivingEntity.class, 8.0F));
    }

    /////////////////////////////////// TICK/////////////////////////////////////////

    public double getMyRidingOffset() {
        return -0.35D;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        //Main.LOGGER.debug("Running goals are: {}", this.goalSelector.getRunningGoals().map(WrappedGoal::getGoal).toArray());
        Level level = this.getCommandSenderWorld();
        level.getProfiler().push("looting");
        if (
            !level.isClientSide && 
            this.canPickUpLoot() && 
            this.isAlive() && 
            !this.dead && 
            ForgeEventFactory.getMobGriefingEvent(level, this)
        ) {
            List<ItemEntity> nearbyItems = level.getEntitiesOfClass(
                ItemEntity.class,
                this.getBoundingBox().inflate(2.5D, 0.5D, 2.5D)
            );
            for (ItemEntity itementity : nearbyItems) {
                if (
                    !itementity.isRemoved() && 
                    !itementity.getItem().isEmpty() && 
                    !itementity.hasPickUpDelay() && 
                    this.wantsToPickUp(itementity.getItem())
                ) {
                    this.pickUpItem(itementity);
                }
            }
        }
    }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (this.wantsToPickUp(itemstack)) {
            SimpleContainer inventory = this.getInventory();
            if (!inventory.canAddItem(itemstack)) return;

            this.onItemPickup(itemEntity);
            this.take(itemEntity, itemstack.getCount());
            ItemStack itemstack1 = inventory.addItem(itemstack);
            if (itemstack1.isEmpty()) {
                itemEntity.remove(RemovalReason.DISCARDED);
            } else {
                itemstack.setCount(itemstack1.getCount());
                this.increaseFarmedItems();
            }
        }
    }

    // TODO: Boolean for worker "can work without tools" like lumberjacks punching trees, or farmers.
    // TODO: GoalAI#canUse() should check for this boolean.
    public void consumeToolDurability() {
        ItemStack heldItem = this.getItemInHand(InteractionHand.MAIN_HAND);
        // Damage the tool
        heldItem.hurtAndBreak(1, this,(worker) -> {
            worker.broadcastBreakEvent(EquipmentSlot.MAINHAND);
            worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            worker.stopUsingItem();

            if (worker.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                LivingEntity owner = worker.getOwner();
                if (owner != null) worker.tellPlayer(owner, TEXT_OUT_OF_TOOLS(heldItem));
                worker.setNeedsTool(true);
            }
        });
        this.upgradeTool();
    }

    public boolean canWorkWithoutTool(){
        return true;
    }

    public void tick() {
        super.tick();
        updateSwingTime();
        updateSwimming();

        //Main.LOGGER.info("Hunger: " + this.getHunger());

        if (hurtTimeStamp > 0)
            hurtTimeStamp--;
    }

    public void rideTick() {
        super.rideTick();
        if (this.getVehicle() instanceof PathfinderMob creatureentity) {
            this.yBodyRot = creatureentity.yBodyRot;
        }
    }

    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor world, @NotNull DifficultyInstance diff, @NotNull MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag nbt) {
        return spawnData;
    }
    public void setDropEquipment() {
        this.dropEquipment();
    }


    //////////////////////////////////// REGISTER////////////////////////////////////

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HOME, Optional.empty());
        this.entityData.define(START_POS, Optional.empty());
        this.entityData.define(DEST_POS, Optional.empty());
        this.entityData.define(CHEST, Optional.empty());
        this.entityData.define(BED, Optional.empty());
        this.entityData.define(IS_WORKING, false);
        this.entityData.define(IS_PICKING_UP, false);
        this.entityData.define(FOLLOW, false);
        this.entityData.define(NEEDS_HOME, false);
        this.entityData.define(NEEDS_CHEST, false);
        this.entityData.define(NEEDS_BED, false);
        this.entityData.define(NEEDS_TOOL, false);
        this.entityData.define(breakingTime, 0);
        this.entityData.define(currentTimeBreak, -1);
        this.entityData.define(previousTimeBreak, -1);
        this.entityData.define(HUNGER, 50F);
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(PROFESSION_NAME, "");
        this.entityData.define(FARMED_ITEMS, 0);
    }

    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("Follow", this.getFollow());
        nbt.putBoolean("isWorking", this.getIsWorking());
        nbt.putBoolean("isPickingUp", this.getIsPickingUp());
        nbt.putBoolean("needsHome", this.needsHome());
        nbt.putBoolean("needsChest", this.needsChest());
        nbt.putBoolean("needsTool", this.needsTool());
        nbt.putInt("breakTime", this.getBreakingTime());
        nbt.putInt("currentTimeBreak", this.getCurrentTimeBreak());
        nbt.putInt("previousTimeBreak", this.getPreviousTimeBreak());
        nbt.putString("OwnerName", this.getOwnerName());
        nbt.putFloat("Hunger", this.getHunger());
        nbt.putString("ProfessionName", this.getProfessionName());
        nbt.putInt("FarmedItems", this.getFarmedItems());

        BlockPos startPos = this.getStartPos();
        if (startPos != null) this.setNbtPosition(nbt, "Start", startPos);
        BlockPos destPos = this.getDestPos();
        if (destPos != null) this.setNbtPosition(nbt, "Dest", destPos);
        BlockPos homePos = this.getHomePos();
        if (homePos != null) this.setNbtPosition(nbt, "Home", homePos);
        BlockPos chestPos = this.getChestPos();
        if (chestPos != null) this.setNbtPosition(nbt, "Chest", chestPos);
        BlockPos bedPos = this.getBedPos();
        if (bedPos != null) this.setNbtPosition(nbt, "Bed", bedPos);
    }

    public void setNbtPosition(CompoundTag nbt, String blockName, @Nullable BlockPos pos) {
        if (pos == null) return;
        nbt.putInt(String.format("%sPosX", blockName), pos.getX());
        nbt.putInt(String.format("%sPosY", blockName), pos.getY());
        nbt.putInt(String.format("%sPosZ", blockName), pos.getZ());
    }

    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setFollow(nbt.getBoolean("Follow"));
        this.setBreakingTime(nbt.getInt("breakTime"));
        this.setIsPickingUp(nbt.getBoolean("isPickingUp"));
        this.setNeedsTool(nbt.getBoolean("needsTool"));
        this.setCurrentTimeBreak(nbt.getInt("currentTimeBreak"));
        this.setPreviousTimeBreak(nbt.getInt("previousTimeBreak"));
        this.setIsWorking(nbt.getBoolean("isWorking"), true);
        this.setHunger(nbt.getFloat("Hunger"));
        this.setOwnerName(nbt.getString("OwnerName"));
        this.setProfessionName(nbt.getString("ProfessionName"));
        this.setFarmedItems(nbt.getInt("FarmedItems"));

        BlockPos startPos = this.getNbtPosition(nbt, "Start");
        if (startPos != null) this.setStartPos(startPos);
        BlockPos destPos = this.getNbtPosition(nbt, "Dest");
        if (destPos != null) this.setDestPos(destPos);
        BlockPos homePos = this.getNbtPosition(nbt, "Home");
        if (homePos != null) this.setHomePos(homePos);
        BlockPos chestPos = this.getNbtPosition(nbt, "Chest");
        if (chestPos != null) this.setChestPos(chestPos);
        BlockPos bedPos = this.getNbtPosition(nbt, "Bed");
        if (bedPos != null) this.setBedPos(bedPos);
    }

    public BlockPos getNbtPosition(CompoundTag nbt, String blockName) {
        if (
            nbt.contains(String.format("%sPosX", blockName)) &&
            nbt.contains(String.format("%sPosY", blockName)) &&
            nbt.contains(String.format("%sPosZ", blockName))
        ) {
            return new BlockPos(
                nbt.getInt(String.format("%sPosX", blockName)),
                nbt.getInt(String.format("%sPosY", blockName)),
                nbt.getInt(String.format("%sPosZ", blockName))
            );
        }
        return null;
    }

    //////////////////////////////////// GET////////////////////////////////////

    public int getFarmedItems() {
        return this.entityData.get(FARMED_ITEMS);
    }

    public String getProfessionName() {
        return entityData.get(PROFESSION_NAME);
    }

    public String getOwnerName() {
        return entityData.get(OWNER_NAME);
    }

    @Nullable
    public BlockPos getHomePos() {
        return this.entityData.get(HOME).orElse(null);
    }

    public boolean needsHome() {
        return this.entityData.get(NEEDS_HOME);
    }

    public boolean needsTool() {
        return this.entityData.get(NEEDS_TOOL);
    }
    @Nullable
    public BlockPos getChestPos() {
        return this.entityData.get(CHEST).orElse(null);
    }

    public boolean needsChest() {
        return this.entityData.get(NEEDS_CHEST);
    }

    @Nullable
    public BlockPos getBedPos() {
        return this.entityData.get(BED).orElse(null);
    }

    public boolean needsBed() {
        return this.entityData.get(NEEDS_BED);
    }

    public int getCurrentTimeBreak() {
        return this.entityData.get(currentTimeBreak);
    }

    public int getPreviousTimeBreak() {
        return this.entityData.get(previousTimeBreak);
    }

    public int getBreakingTime() {
        return this.entityData.get(breakingTime);
    }

    public float getHunger() {
        return this.entityData.get(HUNGER);
    }

    public BlockPos getWorkerOnPos() {
        return this.getOnPos();
    }

    public BlockPos getDestPos() {
        return this.entityData.get(DEST_POS).orElse(null);
    }

    public BlockPos getStartPos() {
        return this.entityData.get(START_POS).orElse(null);
    }

    public boolean getFollow() {
        return this.entityData.get(FOLLOW);
    }

    public boolean getIsWorking() {
        return this.entityData.get(IS_WORKING);
    }

    public boolean getIsPickingUp() {
        return this.entityData.get(IS_PICKING_UP);
    }

    public SoundEvent getHurtSound(DamageSource ds) {
        if(WorkersModConfig.WorkersLookLikeVillagers.get()){
            return SoundEvents.VILLAGER_HURT;
        }
        else
            return SoundEvents.PLAYER_HURT;
    }

    protected SoundEvent getDeathSound() {
        if(WorkersModConfig.WorkersLookLikeVillagers.get()){
            return SoundEvents.VILLAGER_DEATH;
        }
        else
            return SoundEvents.PLAYER_DEATH;
    }

    protected float getSoundVolume() {
        return 0.4F;
    }

    protected float getStandingEyeHeight(Pose pos, EntityDimensions size) {
        return size.height * 0.9F;
    }

    public int getMaxHeadXRot() {
        return this.isInSittingPose() ? 20 : super.getMaxHeadXRot();
    }

    /**
     * This is used to determine whether the worker should store an ItemStack 
     * in a chest or keep it in its inventory.
     * 
     * For example, lumberjacks need saplings to replant trees, farmers need seeds, etc.
     * 
     * @param itemStack The ItemStack to compare against
     * @return true if the ItemStack will be kept in inventory, false if it will be stored in a chest.
     */
    public boolean wantsToKeep(ItemStack itemStack) {
        return (itemStack.isEdible() && itemStack.getFoodProperties(this).getNutrition() > 4);
    }

    //////////////////////////////////// SET////////////////////////////////////

    public void setFarmedItems(int x){
        this.entityData.set(FARMED_ITEMS, x);
    }
    public void setProfessionName(String string) {
        this.entityData.set(PROFESSION_NAME, string);
    }

    public void setOwnerName(String string) {
        this.entityData.set(OWNER_NAME, string);
    }

    public void setHomePos(BlockPos pos) {
        this.entityData.set(HOME, Optional.of(pos));            
    }

    public void setChestPos(BlockPos pos) {
        this.entityData.set(CHEST, Optional.of(pos));
    }
    
    public void setBedPos(BlockPos pos) {
        this.entityData.set(BED, Optional.of(pos));
    }

    public void setPreviousTimeBreak(int value) {
        this.entityData.set(previousTimeBreak, value);
    }

    public void setCurrentTimeBreak(int value) {
        this.entityData.set(currentTimeBreak, value);
    }

    public void setBreakingTime(int value) {
        this.entityData.set(breakingTime, value);
    }

    public void setHunger(float value) {
        this.entityData.set(HUNGER, value);
    }

    public void setDestPos(BlockPos pos) {
        this.entityData.set(DEST_POS, Optional.of(pos));
    }

    public void setStartPos(BlockPos pos) {
        if(this.getStartPos() != pos){
            this.startPosChanged = true;
        }
        this.entityData.set(START_POS, Optional.ofNullable(pos));
    }

    public void clearStartPos() {
        this.entityData.set(START_POS, Optional.empty());
    }

    public void setNeedsHome(boolean bool) {
        this.entityData.set(NEEDS_HOME, bool);
    }

    public void setNeedsTool(boolean bool) {
        if(bool) setFarmedItems(64);
        this.entityData.set(NEEDS_TOOL, bool);
    }

    public void setNeedsChest(boolean bool) {
        this.entityData.set(NEEDS_CHEST, bool);
    }
    
    public void setNeedsBed(boolean bool) {
        this.entityData.set(NEEDS_BED, bool);
    }

    public void setFollow(boolean bool) {
        if (getFollow() == bool) return;
        this.entityData.set(FOLLOW, bool);

        LivingEntity owner = this.getOwner();
        if (owner == null) return;

        if (bool) {
            this.tellPlayer(owner, TEXT_FOLLOW);

        } else if (this.getIsWorking()) {
            this.tellPlayer(owner, TEXT_CONTINUE);
        } else {
            this.tellPlayer(owner, TEXT_WANDER);
        }
    }

	public void setIsWorking(boolean bool) {
        LivingEntity owner = this.getOwner();

        if (getIsWorking() != bool) {
            if (owner != null) {
                if (!this.isStarving()) {
                    if (bool) {
                        this.tellPlayer(owner, TEXT_WORKING);
                    } else {
                        this.tellPlayer(owner, TEXT_DONE);
                    }
                } else if (isStarving()) {
                    this.tellPlayer(owner, TEXT_STARVING);
                    entityData.set(IS_WORKING, false);
                }
            }
            entityData.set(IS_WORKING, bool);
        }
    }

	public void setIsWorking(boolean bool, boolean withoutFeedback) {
        if (withoutFeedback) {
            entityData.set(IS_WORKING, bool);
        } else {
            this.setIsWorking(bool);
        }
    }

    public void setIsPickingUp(boolean bool) {
        entityData.set(IS_PICKING_UP, bool);
    }

    public void setOwned(boolean owned) {
        super.setTame(owned);
    }

    public void setEquipment() {
    }

    //////////////////////////////////// ATTACK
    //////////////////////////////////// FUNCTIONS////////////////////////////////////

    public boolean hurt(DamageSource dmg, float amt) {
        String name = this.getDisplayName().getString();
        String attacker_name;

        if (this.isInvulnerableTo(dmg)) {
            return false;
        } else {
            Entity entity = dmg.getEntity();
            this.setOrderedToSit(false);
            if (entity != null && !(entity instanceof Player) && !(entity instanceof AbstractArrow)) {
                amt = (amt + 1.0F) / 2.0F;
            }

            LivingEntity attacker = this.getLastHurtByMob();

            if (this.isTame() && attacker != null && hurtTimeStamp <= 0) {
                attacker_name = attacker.getDisplayName().getString();

                LivingEntity owner = this.getOwner();
                if (owner != null && owner != attacker) {
                    this.tellPlayer(owner, TEXT_ATTACKED(name, attacker_name));
                    hurtTimeStamp = 80;
                }
            }

            return super.hurt(dmg, amt);
        }
    }

    public boolean doHurtTarget(Entity entity) {
        boolean flag = entity.hurt(this.damageSources().mobAttack(this), (float)((int)this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        if (flag) {
            this.doEnchantDamageEffects(this, entity);
        }

        return flag;
    }

    public void die(@NotNull DamageSource dmg) {

        // TODO: Liberate POI on death.
        super.die(dmg);

    }

    //////////////////////////////////// OTHER     ////////////////////////////////////
    //////////////////////////////////// FUNCTIONS ////////////////////////////////////

    public boolean needsToSleep() {
        return !this.getCommandSenderWorld().isDay();
    }

     public void updateHunger() {
         if(getHunger() > 0) {
             setHunger((getHunger() - 0.0001F));

             if (getIsWorking()) setHunger((getHunger() - 0.0002F));

             if(getBedPos() == null) setHunger((getHunger() - 0.0002F));
         }

        if (isStarving() && this.getIsWorking()) {
            this.setIsWorking(false);
        }
    }

    public void tellPlayer(LivingEntity player, Component message) {
        Component dialogue = Component.literal(this.getName().getString())
            .append(": ")
            .append(message);
        player.sendSystemMessage(dialogue);
    }

    public void walkTowards(BlockPos pos, double speed) {
        this.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), speed);
        this.getLookControl().setLookAt(
            pos.getX(), 
            pos.getY() + 1, 
            pos.getZ(), 
            10.0F,
            this.getMaxHeadXRot()
        );
    }
    public boolean needsToGetFood(){
        boolean isChest = this.getChestPos() != null;
        return this.needsToEat() && (isChest);
    }
    public boolean needsToEat() {
        return (getHunger() <= 50F || getHealth() < getMaxHealth() * 0.2) || isStarving();
    }

    public boolean isStarving() {
        return (getHunger() <= 1F);
    }

    public boolean isSaturated() {
        return (getHunger() >= 90F);
    }

    public void resetWorkerParameters() {
        this.resetFarmedItems();
        this.setBreakingTime(0);
        this.setCurrentTimeBreak(-1);
        this.setPreviousTimeBreak(-1);
    }

    public boolean canWork(){
         boolean canNotWorkWithTool = needsTool() && !canWorkWithoutTool();
        // Stop AI while following the player.
        // Stop AI to at night, so SleepGoal can start.
        // Stop AI if work position is not set.
        // Stop AI if inventory is full, so TransferItemsInChestGoal can start.
        // Stop AI if can not work wotihout tool;
        if(canNotWorkWithTool || this.getStartPos() == null || this.needsToSleep() || this.getFollow() || this.needsToDeposit() || this.needsToGetFood() || startPosChanged) {
            startPosChanged = false;
            return false;
        }
        // Start AI if should working
        return this.getIsWorking();
    }

    public boolean needsToDeposit(){
        return (this.needsTool() || this.getFarmedItems() >= 64) 
                && getChestPos() != null
                && !this.getFollow() 
                && !this.needsChest() 
                && !this.needsToSleep(); //TODO: configurable amount
    }

    public void increaseFarmedItems(){
        this.setFarmedItems(getFarmedItems() + 1);
    }

    public void resetFarmedItems(){
        this.setFarmedItems(0);
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    public abstract int workerCosts();

    @Override
    public boolean canBreed() {
        return false;
    }

    @Override
    protected void spawnTamingParticles(boolean smoke) {

    }

    public void workerSwingArm() {
        if (!this.swinging) {
            this.swing(InteractionHand.MAIN_HAND);
        }
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.getCommandSenderWorld().isClientSide) {
            return InteractionResult.CONSUME;
        } else {
            if (this.isTame() && player.getUUID().equals(this.getOwnerUUID())) {
                if (player.isCrouching()) {
                    openGUI(player);
                }
                if (!player.isCrouching()) {
                    setFollow(!getFollow());
                    return InteractionResult.SUCCESS;
                }
            } else if (this.isTame() && !player.getUUID().equals(this.getOwnerUUID())) {
                this.tellPlayer(player, TEXT_HELLO_OWNED(this.getProfessionName(), this.getOwnerName()));
            } else if (!this.isTame()) {
                this.tellPlayer(player, TEXT_HELLO(this.getProfessionName()));
                this.openHireGUI(player);
                this.navigation.stop();
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
    }

    public boolean hire(Player player) {
        this.makeHireSound();

        this.tame(player);
        this.setOwnerName(player.getDisplayName().getString());
        this.setOrderedToSit(false);
        this.setOwnerUUID(player.getUUID());
        this.setOwned(true);
        this.setFollow(true);
        this.navigation.stop();

        int i = this.random.nextInt(4);
        switch (i) {
            case 1 -> this.tellPlayer(player, TEXT_RECRUITED1);
            case 2 -> this.tellPlayer(player, TEXT_RECRUITED2);
            case 3 -> this.tellPlayer(player, TEXT_RECRUITED3);
        }

        this.tellOtherVillagersIWasHired(player);
        return true;
    }

    public void makeHireSound() {
        this.getCommandSenderWorld().playSound(
            null, 
            this.getX(), 
            this.getY() + 4, 
            this.getZ(),
            WorkersModConfig.WorkersLookLikeVillagers.get() ? SoundEvents.VILLAGER_AMBIENT : SoundEvents.PLAYER_BREATH,
            this.getSoundSource(), 
            15.0F, 
            0.8F + 0.4F * this.random.nextFloat()
        );
    }

    private void tellOtherVillagersIWasHired(Player player) {
        if (this.getCommandSenderWorld() instanceof ServerLevel server) {
            server.getNearbyEntities(Villager.class, null, this, getBoundingBox()).forEach((villager) -> {
                server.onReputationEvent(ReputationEventType.ZOMBIE_VILLAGER_CURED, player, villager);
            });
        }
    }

    public abstract Predicate<ItemEntity> getAllowedItems();

    public abstract void openGUI(Player player);

    public void initSpawn(){
        this.setEquipment();
        this.setDropEquipment();
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
    }

    public void openHireGUI(Player player) {
        this.navigation.stop();
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider containerSupplier = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return AbstractWorkerEntity.this.getCustomName();
                }

                @Nullable
                @Override
                public AbstractContainerMenu createMenu(
                    int i, 
                    @NotNull Inventory playerInventory,
                    @NotNull Player playerEntity
                ) {
                    return new WorkerHireContainer(
                        i, 
                        playerInventory.player, 
                        AbstractWorkerEntity.this,
                        playerInventory
                    );
                }
            };

            Consumer<FriendlyByteBuf> extraDataWriter = packetBuffer -> {
                packetBuffer.writeUUID(getUUID());
            };

            NetworkHooks.openScreen((ServerPlayer) player, containerSupplier, extraDataWriter);
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageHireGui(player, this.getUUID()));
        }
    }

    public double getDistanceToOwner(){
        return this.getOwner() != null ? this.distanceToSqr(this.getOwner()) : 1D;
    }

    public abstract boolean isRequiredMainTool(ItemStack tool);
    public abstract boolean isRequiredSecondTool(ItemStack tool);

    public boolean hasMainToolInInv() {
        SimpleContainer inventory = this.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (this.isRequiredMainTool(itemStack)) return true;
        }
        return false;
    }
}
