package ru.svoperation.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/**
 * Fictional Minecraft scenario entity. Its waypoint is a block position in the
 * current game world; movement is deliberately simple scripted gameplay.
 */
public final class GeranEntity extends Entity {
    private static final EntityDataAccessor<Integer> TARGET_X =
            SynchedEntityData.defineId(GeranEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_Y =
            SynchedEntityData.defineId(GeranEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_Z =
            SynchedEntityData.defineId(GeranEntity.class, EntityDataSerializers.INT);
    private static final int MAX_FLIGHT_TICKS = 20 * 30;
    private static final double GAME_SPEED = 0.28D;
    private boolean finished;

    public GeranEntity(EntityType<? extends GeranEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setGameWaypoint(int x, int y, int z) {
        entityData.set(TARGET_X, x);
        entityData.set(TARGET_Y, y);
        entityData.set(TARGET_Z, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TARGET_X, 0);
        builder.define(TARGET_Y, 0);
        builder.define(TARGET_Z, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || finished) return;

        if (tickCount > MAX_FLIGHT_TICKS) {
            finishAtCurrentPosition();
            return;
        }

        Vec3 destination = new Vec3(entityData.get(TARGET_X) + 0.5D,
                entityData.get(TARGET_Y) + 0.5D, entityData.get(TARGET_Z) + 0.5D);
        Vec3 offset = destination.subtract(position());
        double distance = offset.length();

        if (distance <= GAME_SPEED * 1.5D) {
            setPos(destination.x, destination.y, destination.z);
            finishAtCurrentPosition();
            return;
        }

        Vec3 step = offset.normalize().scale(GAME_SPEED);
        setDeltaMovement(step);
        setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
        float yaw = (float) (Math.atan2(step.z, step.x) * (180.0D / Math.PI)) - 90.0F;
        setYRot(yaw);
        setXRot((float) (-Math.atan2(step.y, Math.sqrt(step.x * step.x + step.z * step.z)) * (180.0D / Math.PI)));
    }

    private void finishAtCurrentPosition() {
        if (finished || level().isClientSide) return;
        finished = true;
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE, 2.0F, 0.82F + random.nextFloat() * 0.18F);
            // In-game blast damages nearby entities but never edits terrain.
            serverLevel.explode(this, getX(), getY(), getZ(), 2.5F, Level.ExplosionInteraction.NONE);
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TargetX", entityData.get(TARGET_X));
        tag.putInt("TargetY", entityData.get(TARGET_Y));
        tag.putInt("TargetZ", entityData.get(TARGET_Z));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setGameWaypoint(tag.getInt("TargetX"), tag.getInt("TargetY"), tag.getInt("TargetZ"));
    }
}
