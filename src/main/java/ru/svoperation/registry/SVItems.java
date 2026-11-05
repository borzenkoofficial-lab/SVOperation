package ru.svoperation.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.svoperation.SVOperation;
import ru.svoperation.item.SV01RifleItem;

public final class SVItems {
    private SVItems() {}
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SVOperation.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SVOperation.MOD_ID);
    public static final RegistryObject<Item> SV01 = ITEMS.register("sv01", () -> new SV01RifleItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> RIFLE_AMMO = ITEMS.register("rifle_ammo", () -> new Item(new Item.Properties()));
    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.svoperation"))
            .icon(() -> new ItemStack(SV01.get()))
            .displayItems((parameters, output) -> {
                output.accept(SV01.get());
                output.accept(RIFLE_AMMO.get());
            }).build());
    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
