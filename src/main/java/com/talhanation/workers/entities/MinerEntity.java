package com.talhanation.workers.entities;

import com.google.common.collect.ImmutableSet;
import com.talhanation.workers.Main;
import com.talhanation.workers.MinerInventoryContainer;
import com.talhanation.workers.entities.ai.*;
import com.talhanation.workers.network.MessageOpenGuiMiner;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;


public class MinerEntity extends AbstractWorkerEntity {

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) -> !item.hasPickUpDelay() && item.isAlive() && this.wantsToPickUp(item.getItem());

    private static final DataParameter<Direction> DIRECTION = EntityDataManager.defineId(MinerEntity.class, DataSerializers.DIRECTION);
    private static final DataParameter<Integer> MINE_TYPE = EntityDataManager.defineId(MinerEntity.class, DataSerializers.INT);
    private static final DataParameter<Integer> DEPTH = EntityDataManager.defineId(MinerEntity.class, DataSerializers.INT);
    /*
    MINE TYPES:
    0 = nothing
    1 = 1x1 Tunel
    2 = 3x3 Tunel
    3 = 8x8x8 Pit
    4 = 8x8x1 flat
     */

    private static final Set<Item> WANTED_ITEMS = ImmutableSet.of(
        Items.COAL,
        Items.IRON_ORE,
        Items.GOLD_ORE,
        Items.DIAMOND,
        Items.EMERALD,
        Items.STONE,
        Items.COBBLESTONE,
        Items.ANDESITE,
        Items.GRANITE,
        Items.GRAVEL,
        Items.SAND,
        Items.SANDSTONE,
        Items.RED_SAND,
        Items.REDSTONE,
        Items.DIRT,
        Items.DIORITE,
        Items.COARSE_DIRT
    );

    public static final Set<Block> IGNORING_BLOCKS = ImmutableSet.of(
            Blocks.CAVE_AIR,
            Blocks.AIR,
            Blocks.TORCH,
            Blocks.WALL_TORCH,
            Blocks.SOUL_WALL_TORCH,
            Blocks.REDSTONE_WIRE,
            Blocks.CAMPFIRE,
            Blocks.CAKE,
            Blocks.ACACIA_SIGN,
            Blocks.SPRUCE_SIGN,
            Blocks.BIRCH_SIGN,
            Blocks.DARK_OAK_SIGN,
            Blocks.JUNGLE_SIGN,
            Blocks.OAK_SIGN,
            Blocks.ACACIA_WALL_SIGN,
            Blocks.SPRUCE_WALL_SIGN,
            Blocks.BIRCH_WALL_SIGN,
            Blocks.DARK_OAK_WALL_SIGN,
            Blocks.JUNGLE_WALL_SIGN,
            Blocks.OAK_WALL_SIGN,
            Blocks.SOUL_LANTERN,
            Blocks.LANTERN,
            Blocks.DETECTOR_RAIL,
            Blocks.RAIL
    );

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DIRECTION, Direction.NORTH);
        this.entityData.define(MINE_TYPE, 0);
        this.entityData.define(DEPTH, 16);
    }

    public MinerEntity(EntityType<? extends AbstractWorkerEntity> entityType, World world) {
        super(entityType, world);

    }

    //ATTRIBUTES
    public static AttributeModifierMap.MutableAttribute setAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new WorkerPickupWantedItemGoal(this));
        this.goalSelector.addGoal(2, new MinerMineTunnelGoal(this, 0.5D, 10D));
        this.goalSelector.addGoal(2, new MinerMine3x3TunnelGoal(this, 0.5D, 10D));
        this.goalSelector.addGoal(2, new MinerMine8x8PitGoal(this, 0.5D, 15D));
        this.goalSelector.addGoal(2, new MinerMine8x8x1FlatGoal(this, 0.5D, 15D));
        this.goalSelector.addGoal(2, new WorkerFollowOwnerGoal(this, 1.2D, 6.0F, 3.0F));

        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));

        this.goalSelector.addGoal(9, new ReturnToVillageGoal(this, 0.6D, false));
        this.goalSelector.addGoal(10, new PatrolVillageGoal(this, 0.6D));
        this.goalSelector.addGoal(10, new WaterAvoidingRandomWalkingGoal(this, 1.0D, 0F));
        this.goalSelector.addGoal(11, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(12, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(10, new LookAtGoal(this, LivingEntity.class, 8.0F));
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld world, DifficultyInstance difficultyInstance, SpawnReason reason, @Nullable ILivingEntityData data, @Nullable CompoundNBT nbt) {
        ILivingEntityData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigator)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(difficultyInstance);
        this.setEquipment();
        this.setDropEquipment();
        this.setCanPickUpLoot(true);
        this.setCustomName(new StringTextComponent("Miner"));
        return ilivingentitydata;
    }

    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (this.wantsToPickUp(itemstack)) {
            Inventory inventory = this.getInventory();
            boolean flag = inventory.canAddItem(itemstack);
            if (!flag) {
                return;
            }

            this.onItemPickup(itemEntity);
            this.take(itemEntity, itemstack.getCount());
            ItemStack itemstack1 = inventory.addItem(itemstack);
            if (itemstack1.isEmpty()) {
                itemEntity.remove();
            } else {
                itemstack.setCount(itemstack1.getCount());
            }
        }

    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return (WANTED_ITEMS.contains(item));
    }

    public boolean shouldIgnorBlock(Block block) {
        return (IGNORING_BLOCKS.contains(block));
    }

    @Override
    public void setEquipment() {
        this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld world, AgeableEntity ageable) {
        return null;
    }

    @Override
    public int workerCosts() {
        return 10;
    }

    public int getMaxMineDepth(){
        return 16;
    }

    @Override
    public Predicate<ItemEntity> getAllowedItems(){
        return ALLOWED_ITEMS;
    }


    public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("MineType", this.getMineType());
        nbt.putInt("Depth", this.getMineDepth());
    }

    public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        this.setMineType(nbt.getInt("MineType"));
        this.setMineDepth(nbt.getInt("Depth"));
    }

    @Override
    protected boolean shouldLoadChunk() {
        return true;
    }

    public void setMineDirection(Direction dir) {
        entityData.set(DIRECTION, dir);
    }

    public Direction getMineDirection() {
        return entityData.get(DIRECTION);
    }

    public void setMineType(int x){
        this.setStartPos(Optional.empty());
        entityData.set(MINE_TYPE, x);
    }

    public int getMineType(){
       return entityData.get(MINE_TYPE);
    }

    public void setMineDepth(int x){
        entityData.set(DEPTH, x);
    }

    public int getMineDepth(){
        return entityData.get(DEPTH);
    }

    public void changeTool(BlockState blockState) {
        ToolType toolType = blockState.getHarvestTool();
        if (toolType != null){
            if (toolType == ToolType.SHOVEL){
                this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.STONE_SHOVEL));
            }
            else if (toolType == ToolType.PICKAXE){
                this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.STONE_PICKAXE));
            }
            else
                this.setItemSlot(EquipmentSlotType.MAINHAND, new ItemStack(ItemStack.EMPTY.getItem()));
        }
    }

    public void openGUI(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            NetworkHooks.openGui((ServerPlayerEntity) player, new INamedContainerProvider() {
                @Override
                public ITextComponent getDisplayName() {
                    return getName();
                }
                @Nullable
                @Override
                public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                    return new MinerInventoryContainer(i, MinerEntity.this, playerInventory);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenGuiMiner(player, this.getUUID()));
        }
    }

    @Override
    public void setIsWorking(boolean bool) {
        if (this.getMineType() == 0) bool = false;

        entityData.set(IS_WORKING, bool);

        LivingEntity owner = this.getOwner();
        if (owner != null)
            if (bool) {
                owner.sendMessage(new StringTextComponent("Im working now!"), owner.getUUID());
            }
            else
                owner.sendMessage(new StringTextComponent("I stopped working now!"), owner.getUUID());


    }
}
