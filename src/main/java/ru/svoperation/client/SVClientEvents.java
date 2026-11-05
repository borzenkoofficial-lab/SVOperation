package ru.svoperation.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import ru.svoperation.registry.SVEntities;
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
 public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
  event.registerEntityRenderer(SVEntities.GERAN.get(), GeranRenderer::new);
 }

 @SubscribeEvent
 public static void onPlayerRender(RenderPlayerEvent.Pre event) {
  Player player = event.getEntity();
  boolean rifleInMainHand = player.getMainHandItem().is(SVItems.SV01.get());
  boolean rifleInOffhand = player.getOffhandItem().is(SVItems.SV01.get());
  if (!rifleInMainHand && !rifleInOffhand) return;

  HumanoidArm weaponArm = rifleInMainHand
   ? player.getMainArm()
   : (player.getMainArm() == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT);

  PlayerModel<?> model = event.getRenderer().getModel();
  // The native crossbow hold pose positions the support arm against the weapon.
  // Set it only on the weapon arm: that pose already drives both arms together.
  if (weaponArm == HumanoidArm.RIGHT) {
   model.rightArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
   model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
  } else {
   model.leftArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
   model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
  }
 }
}
