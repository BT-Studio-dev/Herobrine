package dev.btstudio.herobrine.entity;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Steve, the friendly one.
 *
 * <p>A player-shaped companion for singleplayer survival: he follows the person who summoned him,
 * fights hostile mobs (Herobrine included - he knows what his own face means), picks up the drops
 * he walks over, and never despawns. He is not a {@link Monster}, so nothing in the haunting code
 * treats him as a threat, and he keeps working in survival mode - no commands needed once he exists
 * beyond the one that summoned him.</p>
 */
public class SteveFriendEntity extends PathfinderMob {
    /** Past this many blocks he starts walking after you... */
    private static final double FOLLOW_START = 8.0;
    /** ...and he is close enough once he is inside this. */
    private static final double FOLLOW_STOP = 3.0;
    /** Left this far behind, he gives up on walking and appears behind you. */
    private static final double TELEPORT_DISTANCE = 24.0;
    private static final int TELEPORT_COOLDOWN_TICKS = 100;
    /** Hostiles inside this radius get his attention. */
    private static final double DEFEND_RADIUS = 16.0;
    private static final double MAX_HEALTH = 40.0;
    private static final double ATTACK_DAMAGE = 5.0;

    private Optional<UUID> owner = Optional.empty();
    private boolean staying;
    private int teleportCooldown;

    public SteveFriendEntity(EntityType<? extends SteveFriendEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCanPickUpLoot(true);
        this.setCustomName(Component.translatable("herobrine.steve.name"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FollowOwnerGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Anything hostile counts - including a hostile Herobrine, who is a Monster.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.teleportCooldown > 0) {
            this.teleportCooldown--;
        }
    }

    /**
     * Binds him to a player and gives him that player's own username, so the name floating over him
     * reads like a second person in the world rather than a mob called "Steve".
     */
    public void setOwner(Player player) {
        this.owner = Optional.of(player.getUUID());

        if (player.getName() != null) {
            this.setCustomName(player.getName());
            this.setCustomNameVisible(true);
        }
    }

    public boolean isOwnedBy(Player player) {
        return this.owner.map(uuid -> uuid.equals(player.getUUID())).orElse(false);
    }

    public boolean isStaying() {
        return this.staying;
    }

    public void setStaying(boolean staying) {
        this.staying = staying;

        if (staying) {
            this.getNavigation().stop();
        }
    }

    /** The player this Steve belongs to, or null when they are offline or out of the world. */
    public Player owner() {
        if (this.owner.isEmpty() || !(this.level() instanceof ServerLevel server)) {
            return null;
        }

        return server.getServer().getPlayerList().getPlayer(this.owner.get());
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Owner", this.owner.map(UUID::toString).orElse(""));
        output.putBoolean("Staying", this.staying);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String saved = input.getStringOr("Owner", "");

        if (saved.isEmpty()) {
            this.owner = Optional.empty();
        } else {
            try {
                this.owner = Optional.of(UUID.fromString(saved));
            } catch (IllegalArgumentException exception) {
                this.owner = Optional.empty();
            }
        }
        this.staying = input.getBooleanOr("Staying", false);
    }

    /** Walks to the owner, or catches up by teleport when walking will not do. */
    private class FollowOwnerGoal extends Goal {
        @Override
        public boolean canUse() {
            return !SteveFriendEntity.this.staying && SteveFriendEntity.this.getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            Player owner = SteveFriendEntity.this.owner();

            if (owner == null || !owner.isAlive() || owner.level() != SteveFriendEntity.this.level()) {
                return;
            }

            double distance = SteveFriendEntity.this.distanceTo(owner);

            if (distance > TELEPORT_DISTANCE && SteveFriendEntity.this.teleportCooldown == 0
                    && !SteveFriendEntity.this.isPassenger()) {
                SteveFriendEntity.this.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                SteveFriendEntity.this.teleportCooldown = TELEPORT_COOLDOWN_TICKS;
                SteveFriendEntity.this.getNavigation().stop();
                return;
            }

            if (distance > FOLLOW_START) {
                SteveFriendEntity.this.getNavigation().moveTo(owner, 1.1);
            } else if (distance < FOLLOW_STOP) {
                SteveFriendEntity.this.getNavigation().stop();
            }

            LivingEntity target = SteveFriendEntity.this.getTarget();

            if (target == null) {
                LivingEntity nearest = SteveFriendEntity.this.level()
                        .getEntitiesOfClass(Monster.class,
                                SteveFriendEntity.this.getBoundingBox().inflate(DEFEND_RADIUS),
                                Monster::isAlive)
                        .stream()
                        .min(java.util.Comparator.comparingDouble(
                                monster -> SteveFriendEntity.this.distanceTo(monster)))
                        .orElse(null);

                if (nearest != null) {
                    SteveFriendEntity.this.setTarget(nearest);
                }
            }
        }
    }
}
