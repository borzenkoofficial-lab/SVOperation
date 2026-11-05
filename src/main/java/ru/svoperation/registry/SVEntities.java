package ru.svoperation.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.svoperation.SVOperation;
import ru.svoperation.entity.GeranEntity;

public final class SVEntities {
    private SVEntities() {}
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SVOperation.MOD_ID);

    public static final RegistryObject<EntityType<GeranEntity>> GERAN = ENTITY_TYPES.register("geran",
            () -> EntityType.Builder.<GeranEntity>of(GeranEntity::new, MobCategory.MISC)
                    .sized(1.65F, 0.42F)
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .build("geran"));

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
