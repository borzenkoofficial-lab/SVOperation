package ru.svoperation.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.svoperation.SVOperation;
import ru.svoperation.registry.SVEntities;

@Mod.EventBusSubscriber(modid = SVOperation.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SVClientModEvents {
    private SVClientModEvents() {}

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SVEntities.GERAN.get(), GeranRenderer::new);
    }
}
