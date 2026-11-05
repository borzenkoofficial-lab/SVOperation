package ru.svoperation.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.svoperation.SVOperation;

public final class SVSounds {
    private SVSounds() {}
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SVOperation.MOD_ID);
    public static final RegistryObject<SoundEvent> SV01_FIRE = register("sv01_fire");
    public static final RegistryObject<SoundEvent> SV01_RELOAD = register("sv01_reload");
    public static final RegistryObject<SoundEvent> SV01_BOLT = register("sv01_bolt");
    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SVOperation.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
    public static void register(IEventBus modBus) { SOUNDS.register(modBus); }
}
