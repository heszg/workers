package com.talhanation.workers.items;
import com.talhanation.workers.entities.AbstractWorkerEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

public class WorkersSpawnEgg extends ForgeSpawnEggItem {

    private final Supplier<? extends EntityType<? extends AbstractWorkerEntity>> entityType;
    // private Supplier<EntityType<?>> entityType;

    public WorkersSpawnEgg(Supplier<? extends EntityType<? extends AbstractWorkerEntity>> entityType, int primaryColor, int secondaryColor,
            Properties properties) {
        super(entityType, primaryColor, secondaryColor, properties);
        this.entityType = entityType;
    }

    @Override
    public @NotNull EntityType<?> getType(CompoundTag compound) {
        if (compound != null && compound.contains("EntityTag", 10)) {
            CompoundTag entityTag = compound.getCompound("EntityTag");

            if (entityTag.contains("id", 8)) {
                return EntityType.byString(entityTag.getString("id")).orElse(this.entityType.get());
            }
        }
        return this.entityType.get();
    }
}