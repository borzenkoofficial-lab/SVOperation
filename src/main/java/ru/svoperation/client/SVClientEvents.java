package ru.svoperation.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.svoperation.SVOperation;
import ru.svoperation.registry.SVItems;

@Mod.EventBusSubscriber(
 modid = SVOperation.MOD_ID,
 bus = Mod.EventBusSubscriber.Bus.FORGE,
 value = Dist.CLIENT
)
public final class SVClientEvents {
 private SVClientEvents() {}

 @SubscribeEvent
 public static void onPlayerRender(RenderPlayerEvent.Pre event) {
  Player player = event.getEntity();
  boolean rifleInHands = player.getMainHandItem().is(SVItems.RIFLE.get())
   || player.getOffhandItem().is(SVItems.RIFLE.get());
  if (!rifleInHands) return;

  PlayerModel<?> model = event.getRenderer().getModel();
  // CROSSBOW_HOLD is Minecraft's native two-arm pose; applying it to both arms
  // keeps the support hand on the fore-end instead of leaving it hanging.
  model.rightArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
  model.leftArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
 }
}
