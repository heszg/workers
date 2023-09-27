package com.talhanation.workers.util;

import com.talhanation.workers.entities.AbstractWorkerEntity;
import com.talhanation.workers.init.ModItems;
import com.talhanation.workers.items.WorkersSpawnEgg;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class RegistryUtils {

    public static RegistryObject<Item> createSpawnEggItem(String entityName, Supplier<EntityType<? extends AbstractWorkerEntity>> supplier, int primaryColor, int secondaryColor) {
        RegistryObject<Item> spawnEgg = ModItems.ITEMS.register(entityName + "_spawn_egg", () -> new WorkersSpawnEgg(supplier, primaryColor, secondaryColor, new Item.Properties()));
        ModItems.SPAWN_EGGS.add(spawnEgg);
        return spawnEgg;
    }

    public static RegistryObject<BlockItem> createToolboxItem(String entityName, RegistryObject<Block> blockRegistry, Properties properties ) {
        RegistryObject<BlockItem> box = ModItems.ITEMS.register(entityName, () -> new BlockItem(blockRegistry.get(), properties) );
        ModItems.TOOLBOXES.add(box);
        return box;
    }

}