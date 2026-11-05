package ru.svoperation.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ru.svoperation.entity.GeranEntity;
import ru.svoperation.registry.SVEntities;

/**
 * Two-step in-game controller: right-click a block to mark a Minecraft
 * waypoint, then right-click air to launch. Sneak-use cancels the selection.
 */
public final class GeranControllerItem extends Item {
    private static final String KEY_X = "svoperation_geran_x";
    private static final String KEY_Y = "svoperation_geran_y";
    private static final String KEY_Z = "svoperation_geran_z";
    private static final String KEY_SET = "svoperation_geran_waypoint_set";
    private static final int MAX_GAME_RANGE = 128;

    public GeranControllerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            clearWaypoint(player);
            player.displayClientMessage(Component.translatable("message.svoperation.geran_cancelled"), true);
            return InteractionResult.CONSUME;
        }

        BlockPos selected = context.getClickedPos().relative(context.getClickedFace());
        if (player.blockPosition().distSqr(selected) > (double) MAX_GAME_RANGE * MAX_GAME_RANGE) {
            player.displayClientMessage(Component.translatable("message.svoperation.geran_too_far"), true);
            return InteractionResult.FAIL;
        }

        player.getPersistentData().putInt(KEY_X, selected.getX());
        player.getPersistentData().putInt(KEY_Y, selected.getY());
        player.getPersistentData().putInt(KEY_Z, selected.getZ());
        player.getPersistentData().putBoolean(KEY_SET, true);
        player.displayClientMessage(Component.translatable("message.svoperation.geran_waypoint_set",
                selected.getX(), selected.getY(), selected.getZ()), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<net.minecraft.world.item.ItemStack> use(
            Level level, Player player, net.minecraft.world.InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);

        if (player.isShiftKeyDown()) {
            clearWaypoint(player);
            player.displayClientMessage(Component.translatable("message.svoperation.geran_cancelled"), true);
            return InteractionResultHolder.consume(stack);
        }

        var data = player.getPersistentData();
        if (!data.getBoolean(KEY_SET)) {
            player.displayClientMessage(Component.translatable("message.svoperation.geran_select_point"), true);
            return InteractionResultHolder.fail(stack);
        }

        int x = data.getInt(KEY_X);
        int y = data.getInt(KEY_Y);
        int z = data.getInt(KEY_Z);
        BlockPos target = new BlockPos(x, y, z);
        if (player.blockPosition().distSqr(target) > (double) MAX_GAME_RANGE * MAX_GAME_RANGE
                || !level.hasChunkAt(target)) {
            clearWaypoint(player);
            player.displayClientMessage(Component.translatable("message.svoperation.geran_invalid_point"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (level instanceof ServerLevel serverLevel) {
            GeranEntity entity = SVEntities.GERAN.get().create(serverLevel);
            if (entity == null) return InteractionResultHolder.fail(stack);
            Vec3 start = player.position().add(0.0D, 1.4D, 0.0D);
            entity.setPos(start.x, start.y, start.z);
            entity.setGameWaypoint(x, y, z);
            serverLevel.addFreshEntity(entity);
            player.displayClientMessage(Component.translatable("message.svoperation.geran_launched"), true);
        }
        clearWaypoint(player);
        return InteractionResultHolder.consume(stack);
    }

    private static void clearWaypoint(Player player) {
        var data = player.getPersistentData();
        data.remove(KEY_X);
        data.remove(KEY_Y);
        data.remove(KEY_Z);
        data.remove(KEY_SET);
    }
}
