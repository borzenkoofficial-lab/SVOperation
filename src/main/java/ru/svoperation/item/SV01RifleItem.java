package ru.svoperation.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.svoperation.client.SV01RifleRenderer;
import ru.svoperation.registry.SVItems;
import ru.svoperation.registry.SVSounds;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.Optional;
import java.util.function.Consumer;

public final class SV01RifleItem extends Item implements GeoItem {
 public static final int MAGAZINE_CAPACITY = 30;
 private static final int USE_DURATION = 72_000;
 private static final double RANGE = 48.0D;
 private static final float DAMAGE = 7.0F;
 private static final int COOLDOWN_TICKS = 10;
 private static final RawAnimation AIM_ANIMATION = RawAnimation.begin().thenLoop("animation.sv01.aim");
 private static final RawAnimation FIRE_ANIMATION = RawAnimation.begin().thenPlay("animation.sv01.fire");
 private static final RawAnimation RELOAD_ANIMATION = RawAnimation.begin().thenPlay("animation.sv01.reload");
 private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

 public SV01RifleItem(Properties properties) {
  super(properties.durability(MAGAZINE_CAPACITY + 1).stacksTo(1));
  SingletonGeoAnimatable.registerSyncedAnimatable(this);
 }

 @Override
 public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
  controllers.add(new AnimationController<>(this, "weapon", 0, state -> PlayState.STOP)
   .triggerableAnim("aim", AIM_ANIMATION)
   .triggerableAnim("fire", FIRE_ANIMATION)
   .triggerableAnim("reload", RELOAD_ANIMATION));
 }

 @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

 @Override
 public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
  consumer.accept(new GeoRenderProvider() {
   private SV01RifleRenderer renderer;
   @Override public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
    if (renderer == null) renderer = new SV01RifleRenderer();
    return renderer;
   }
  });
 }

 @Override
 public UseAnim getUseAnimation(ItemStack stack) {
  return UseAnim.CROSSBOW;
 }

 @Override
 public int getUseDuration(ItemStack stack, LivingEntity entity) {
  return USE_DURATION;
 }

 @Override
 public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
  ItemStack rifle = player.getItemInHand(hand);
  if (player.isShiftKeyDown()) {
   if (level.isClientSide) SV01RifleRenderer.triggerReload();
   else if (level instanceof ServerLevel serverLevel) reload(serverLevel, player, rifle);
   return InteractionResultHolder.sidedSuccess(rifle, level.isClientSide);
  }
  if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(rifle);
  if (!player.getAbilities().instabuild && getRounds(rifle) <= 0) return InteractionResultHolder.fail(rifle);

  // Hold right mouse to shoulder the rifle; releasing it fires one shot.
  player.startUsingItem(hand);
  if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
   triggerAnim(player, GeoItem.getOrAssignId(rifle, serverLevel), "weapon", "aim");
  }
  return InteractionResultHolder.consume(rifle);
 }

 @Override
 public void releaseUsing(ItemStack rifle, Level level, LivingEntity entity, int timeLeft) {
  if (level.isClientSide) {
   if (entity instanceof Player) SV01RifleRenderer.triggerFire();
   return;
  }
  if (!(entity instanceof Player player) || !(level instanceof ServerLevel serverLevel)) return;
  if (player.getCooldowns().isOnCooldown(this)) return;
  if (!player.getAbilities().instabuild && getRounds(rifle) <= 0) return;

  if (!player.getAbilities().instabuild) rifle.setDamageValue(rifle.getDamageValue() + 1);
  triggerAnim(player, GeoItem.getOrAssignId(rifle, serverLevel), "weapon", "fire");
  fire(serverLevel, player);
  player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
 }

 private static int getRounds(ItemStack rifle) {
  return Math.max(0, MAGAZINE_CAPACITY - rifle.getDamageValue());
 }

 private void reload(ServerLevel level, Player player, ItemStack rifle) {
  int missing = MAGAZINE_CAPACITY - getRounds(rifle);
  if (missing <= 0) return;
  int loaded = consumeAmmo(player, missing);
  if (loaded <= 0) {
   level.playSound(null, player.blockPosition(), SVSounds.SV01_BOLT.get(), SoundSource.PLAYERS, 0.35F, 0.72F);
   return;
  }
  rifle.setDamageValue(MAGAZINE_CAPACITY - (getRounds(rifle) + loaded));
  triggerAnim(player, GeoItem.getOrAssignId(rifle, level), "weapon", "reload");
  level.playSound(null, player.blockPosition(), SVSounds.SV01_RELOAD.get(), SoundSource.PLAYERS, 0.9F, 0.96F + level.random.nextFloat() * 0.08F);
  level.playSound(null, player.blockPosition(), SVSounds.SV01_BOLT.get(), SoundSource.PLAYERS, 0.65F, 0.96F + level.random.nextFloat() * 0.08F);
  player.getCooldowns().addCooldown(this, 25);
 }

 private static int consumeAmmo(Player player, int requested) {
  int remaining = requested;
  for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
   ItemStack stack = player.getInventory().getItem(slot);
   if (!stack.is(SVItems.RIFLE_AMMO.get())) continue;
   int take = Math.min(remaining, stack.getCount());
   stack.shrink(take);
   remaining -= take;
  }
  return requested - remaining;
 }

 private void fire(ServerLevel level, Player player) {
  Vec3 start = player.getEyePosition();
  Vec3 maxEnd = start.add(player.getLookAngle().scale(RANGE));
  BlockHitResult blockHit = level.clip(new ClipContext(start, maxEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
  Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
  AABB searchBox = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
  Entity closest = null;
  double closestDistance = Double.MAX_VALUE;
  for (Entity entity : level.getEntities(player, searchBox,
   candidate -> candidate instanceof LivingEntity && candidate.isAlive() && candidate.isPickable())) {
   Optional<Vec3> intersection = entity.getBoundingBox().inflate(0.25D).clip(start, end);
   if (intersection.isPresent()) {
    double distance = start.distanceToSqr(intersection.get());
    if (distance < closestDistance) { closestDistance = distance; closest = entity; }
   }
  }
  if (closest instanceof LivingEntity target) target.hurt(level.damageSources().playerAttack(player), DAMAGE);
  level.playSound(null, BlockPos.containing(start), SVSounds.SV01_FIRE.get(), SoundSource.PLAYERS,
   1.0F, 0.96F + level.random.nextFloat() * 0.08F);
 }
}
