package com.talhanation.workers.init;

import com.google.common.collect.Lists;
import com.talhanation.workers.Main;
import com.talhanation.workers.util.RegistryUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MOD_ID);
    public static final List<RegistryObject<Item>> SPAWN_EGGS = Lists.newArrayList();
    public static final List<RegistryObject<BlockItem>> TOOLBOXES = Lists.newArrayList();

    public static final RegistryObject<Item> MINER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("miner", ModEntityTypes.MINER::get, 16755200, 16777045);
    public static final RegistryObject<Item> LUMBER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("lumberjack", ModEntityTypes.LUMBERJACK::get, 16755200, 16777045);
    public static final RegistryObject<Item> FISHERMAN_SPAWN_EGG = RegistryUtils.createSpawnEggItem("fisherman", ModEntityTypes.FISHERMAN::get, 16755201, 16777044);
    public static final RegistryObject<Item> SHEPHERD_SPAWN_EGG = RegistryUtils.createSpawnEggItem("shepherd", ModEntityTypes.SHEPHERD::get, 16755200, 16777045);
    public static final RegistryObject<Item> FARMER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("farmer", ModEntityTypes.FARMER::get, 16755200, 16777045);
    public static final RegistryObject<Item> MERCHANT_SPAWN_EGG = RegistryUtils.createSpawnEggItem("merchant", ModEntityTypes.MERCHANT::get, 16755200, 16777045);
    public static final RegistryObject<Item> CATTLE_FARMER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("cattle_farmer", ModEntityTypes.CATTLE_FARMER::get, 16755200, 16777045);
    public static final RegistryObject<Item> CHICKEN_FARMER_SPAWN_EGG = RegistryUtils.createSpawnEggItem("chicken_farmer", ModEntityTypes.CHICKEN_FARMER::get, 16755200, 16777045);
    public static final RegistryObject<Item> SWINEHERD_SPAWN_EGG = RegistryUtils.createSpawnEggItem("swineherd", ModEntityTypes.SWINEHERD::get, 16755200, 16777045);

    public static final RegistryObject<BlockItem> miner_block = RegistryUtils.createToolboxItem("miner_block", ModBlocks.MINER_BLOCK, new Item.Properties());
    public static final RegistryObject<BlockItem> lumberjack_block = RegistryUtils.createToolboxItem("lumberjack_block", ModBlocks.LUMBERJACK_BLOCK, new Item.Properties());
    public static final RegistryObject<BlockItem> fisherman_block = RegistryUtils.createToolboxItem("fisherman_block", ModBlocks.FISHER_BLOCK, new Item.Properties());
    public static final RegistryObject<BlockItem> merchant_block = RegistryUtils.createToolboxItem("merchant_block", ModBlocks.MERCHANT_BLOCK, new Item.Properties());
    public static final RegistryObject<BlockItem> farmer_block = RegistryUtils.createToolboxItem("farmer_block", ModBlocks.FARMER_BLOCK, new Item.Properties());
    public static final RegistryObject<BlockItem> shepherd_block = RegistryUtils.createToolboxItem("shepherd_block", ModBlocks.SHEPHERD_BLOCK, new Item.Properties());
    public static final RegistryObject<BlockItem> cattle_farmer_block = RegistryUtils.createToolboxItem("cattle_farmer_block", ModBlocks.CATTLE_FARMER_BLOCK, new Item.Properties());
    public static final RegistryObject<BlockItem> chicken_farmer_block = RegistryUtils.createToolboxItem("chicken_farmer_block", ModBlocks.CHICKEN_FARMER_BLOCK, new Item.Properties());
    public static final RegistryObject<BlockItem> swineherd_block = RegistryUtils.createToolboxItem("swineherd_block", ModBlocks.SWINEHERD_BLOCK, new Item.Properties());
}