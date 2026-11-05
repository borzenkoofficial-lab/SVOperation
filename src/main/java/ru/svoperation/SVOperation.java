package ru.svoperation;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import ru.svoperation.registry.SVItems;
import ru.svoperation.registry.SVSounds;

@Mod(SVOperation.MOD_ID)
public final class SVOperation {
    public static final String MOD_ID = "svoperation";
    public static final Logger LOGGER = LogUtils.getLogger();
    public SVOperation() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        SVItems.register(modBus);
        SVSounds.register(modBus);
        LOGGER.info("SVOperation initialized");
    }
}
