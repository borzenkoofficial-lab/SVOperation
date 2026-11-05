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
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoRenderProvider;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.Optional;
import java.util.function.Consumer;

public final class SV01RifleItem extends Item implements GeoItem {
    private static final double RANGE = 48.0D;
    private static final float DAMAGE = 7.0F;
    private static final int COOLDOWN_TICKS = 10;
    private static final RawAnimation FIRE_ANIMATION = RawAnimation.begin().thenPlay("animation.sv01.fire");
    private static final RawAnimation RELOAD_ANIMATION = RawAnimation.begin().thenPlay("animation.sv01.reload");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public SV01RifleItem(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "weapon", 0, state -> PlayState.STOP)
                .triggerableAnim("fire", FIRE_ANIMATION).triggerableAnim("reload", RELOAD_ANIMATION));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SV01RifleRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer == null) renderer = new SV01RifleRenderer();
                return renderer;
            }
        });
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack rifle = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild && !hasAmmo(player)) return InteractionResultHolder.fail(rifle);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            if (!player.getAbilities().instabuild && !consumeAmmo(player)) return InteractionResultHolder.fail(rifle);
            triggerAnim(player, GeoItem.getOrAssignId(rifle, serverLevel), "weapon", "fire");
            fire(serverLevel, player);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(rifle, level.isClientSide);
    }
    private static boolean hasAmmo(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            if (player.getInventory().getItem(slot).is(SVItems.RIFLE_AMMO.get())) return true;
        return false;
    }
    private static boolean consumeAmmo(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(SVItems.RIFLE_AMMO.get())) { stack.shrink(1); return true; }
        }
        return false;
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
