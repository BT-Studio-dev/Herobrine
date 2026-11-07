package dev.btstudio.herobrine.entity;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.Monster;

import dev.btstudio.herobrine.event.IronWard;
import dev.btstudio.herobrine.sound.ModSounds;
import dev.btstudio.herobrine.world.HerobrineName;
import dev.btstudio.herobrine.world.HerobrineSettings;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Herobrine has four forms:
 * <ul>
 *   <li><b>Stalker</b> (default, spawned by haunting events): stands motionless, stares at the
 *       nearest player and vanishes if looked at for too long, approached, or after ~20s.</li>
 *   <li><b>Hostile</b> (gold shrine): red eyes, hunts and attacks players.</li>
 *   <li><b>Elite</b> ({@code /herobrine summon elite}): black and red, tougher than the
 *       gold shrine's Herobrine.</li>
 *   <li><b>Watcher</b>: the <i>True Herobrine</i> form. He does nothing but stand still and look
 *       at you for two full minutes, and vanishes the moment you come within 25 blocks.</li>
 * </ul>
 */
public class HerobrineEntity extends Monster {
    private static final EntityDataAccessor<Boolean> HOSTILE =
            SynchedEntityData.defineId(HerobrineEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ELITE =
            SynchedEntityData.defineId(HerobrineEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WATCHER =
            SynchedEntityData.defineId(HerobrineEntity.class, EntityDataSerializers.BOOLEAN);

    /** ~20 seconds at 20 ticks per second. */
    /** How often the name setting is re-read, in ticks. He ticks twenty times a second and the
     *  name changes perhaps once in a world's lifetime. */
    private static final int NAME_CHECK_INTERVAL = 20;

    private static final int MAX_STALK_TICKS = 400;
    /** ~1.5 seconds of being stared at. */
    private static final int MAX_STARE_TICKS = 30;
    private static final double VANISH_DISTANCE = 7.0;
    private static final double SEARCH_RANGE = 64.0;

    // Attributes of the elite version.
    private static final double ELITE_MAX_HEALTH = 120.0;
    private static final double ELITE_ATTACK_DAMAGE = 14.0;
    private static final double ELITE_ARMOR = 12.0;
    private static final double ELITE_KNOCKBACK_RESISTANCE = 0.8;
    private static final double ELITE_MOVEMENT_SPEED = 0.34;

    /** True Herobrine rules: he watches for two full minutes. */
    private static final int WATCHER_MAX_TICKS = 2400;
    /** ...and is gone the moment you come within 25 blocks of him. */
    private static final double WATCHER_VANISH_DISTANCE = 25.0;

    /** A scare just stands there, breathing, for ten seconds. */
    private static final int SCARE_MAX_TICKS = 200;
    /** The jumpscare is two seconds of noise and darkness. */
    private static final int JUMPSCARE_MAX_TICKS = 40;
    private static final int JUMPSCARE_DARKNESS_TICKS = 200;
    /** A spike waits this long before it lunges into your face... */
    private static final int SPIKE_LUNGE_TICKS = 50;
    /** ...and is gone a few seconds later. */
    private static final int SPIKE_MAX_TICKS = 110;
    private static final int SPIKE_NAUSEA_TICKS = 180;

    /** The runner stands around for at most twenty seconds... */
    private static final int RUNNER_MAX_TICKS = 400;
    /** ...and bolts in hops, this often... */
    private static final int RUNNER_FLEE_INTERVAL = 12;
    /** ...this far. */
    private static final double RUNNER_FLEE_HOP = 5.0;
    /** He is spooked by a glance, or by you closing to this distance. */
    private static final double RUNNER_SPOOK_DISTANCE = 26.0;
    /** Past this he is out of sight and gone. */
    private static final double RUNNER_VANISH_DISTANCE = 48.0;
    private static final float RUNNER_STEP_PITCH = 1.4F;

    /** A lurking sighting just stands there, two to four times further out. */
    private static final int LURKING_MAX_TICKS = 1200;
    /** A dwelling one refuses to leave while the light is below this... */
    private static final int DWELLING_MIN_LIGHT = 8;
    /** ...for up to two minutes. */
    private static final int DWELLING_MAX_TICKS = 2400;

    /** Walk into him this close and he screams. */
    private static final double SCARE_DISTANCE = 2.5;
    /** "Sneaky strike": ignore him long enough and he takes a swing before vanishing. */
    private static final int STRIKE_AFTER_TICKS = 60;
    private static final float STRIKE_CHANCE = 0.4F;
    private static final float STRIKE_DAMAGE = 3.0F;

    private int lifeTicks;
    private int stareTicks;
    private Apparition apparition = Apparition.WATCH;

    /** The name last written to the nametag, so the tick compares strings instead of components. */
    private String appliedName;
    private int nameCheckTicks;

    public HerobrineEntity(EntityType<? extends HerobrineEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOSTILE, false);
        builder.define(ELITE, false);
        builder.define(WATCHER, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isHostile() {
        return this.entityData.get(HOSTILE);
    }

    /** Hostile Herobrine hunts players; stalker Herobrine only watches, then vanishes. */
    public void setHostile(boolean hostile) {
        this.entityData.set(HOSTILE, hostile);
        // Stalkers stand perfectly still.
        this.setNoAi(!hostile);
        if (hostile) {
            this.setPersistenceRequired();
        }
    }

    /** True for the elite version, which is tougher and uses the black skin. */
    public boolean isElite() {
        return this.entityData.get(ELITE);
    }

    /**
     * Turns this Herobrine into the elite version.
     *
     * <p>Current health is deliberately left alone so a damaged elite keeps its health when a
     * level is reloaded; a freshly spawned one should be topped up by the caller with
     * {@code setHealth(getMaxHealth())}.
     */
    public void setElite(boolean elite) {
        this.entityData.set(ELITE, elite);

        if (!elite) {
            return;
        }

        this.setAttributeBase(Attributes.MAX_HEALTH, ELITE_MAX_HEALTH);
        this.setAttributeBase(Attributes.ATTACK_DAMAGE, ELITE_ATTACK_DAMAGE);
        this.setAttributeBase(Attributes.ARMOR, ELITE_ARMOR);
        this.setAttributeBase(Attributes.KNOCKBACK_RESISTANCE, ELITE_KNOCKBACK_RESISTANCE);
        this.setAttributeBase(Attributes.MOVEMENT_SPEED, ELITE_MOVEMENT_SPEED);
        this.setPersistenceRequired();
    }

    /** True for the watcher: non-destructive, motionless, and gone if you get close. */
    public boolean isWatcher() {
        return this.entityData.get(WATCHER);
    }

    /**
     * Turns this Herobrine into the {@code True Herobrine} watcher. Nothing else is touched: he has
     * no AI, never attacks, and his only exit is his own timer or the 25 block distance rule.
     */
    public void setWatcher(boolean watcher) {
        this.entityData.set(WATCHER, watcher);

        if (watcher) {
            this.entityData.set(HOSTILE, false);
            this.setNoAi(true);
        }
    }

    /** Which of the four <i>Him</i> apparitions this one is. Defaults to a plain sighting. */
    public Apparition getApparition() {
        return this.apparition;
    }

    /** Chooses the behaviour; it only takes effect while he is not hostile. */
    public void setApparition(Apparition apparition) {
        this.apparition = apparition == null ? Apparition.WATCH : apparition;
    }

    /**
     * "Don't hit him." Any hit lands on an apparition, so the apparition drops its act: the scare
     * that was only standing behind you turns into a hunter.
     *
     * <p>Unless the hit came from iron. Iron-stick gear repels him: an apparition struck with it
     * recoils and is gone, and the hunting form is driven back a few blocks
     * ({@link dev.btstudio.herobrine.event.IronWard}).
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (IronWard.repel(level, this, source)) {
            // He is already gone; there is nothing left to hurt.
            return true;
        }

        if (!this.isHostile()) {
            this.setHostile(true);
        }

        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);

        if (hit && target != null && this.isElite()) {
            target.igniteForSeconds(4.0F);
        }

        return hit;
    }

    private void setAttributeBase(Holder<Attribute> attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);

        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!(this.level() instanceof ServerLevel serverLevel) || this.isHostile()) {
            return;
        }

        this.refreshName(serverLevel);

        this.lifeTicks++;

        Player player = serverLevel.getNearestPlayer(this, SEARCH_RANGE);

        if (player == null) {
            this.vanish(serverLevel);
            return;
        }

        switch (this.apparition) {
            case SCARE -> this.tickScare(serverLevel, player);
            case JUMPSCARE -> this.tickJumpscare(serverLevel, player);
            case SPIKE -> this.tickSpike(serverLevel, player);
            case RUNNER -> this.tickRunner(serverLevel, player);
            case LURKING, DWELLING -> this.tickWatch(serverLevel, player);
            default -> this.tickWatch(serverLevel, player);
        }
    }

    /**
     * Keeps his name in step with the setting: the one the world has given him is the one on his
     * nametag, on his death messages and in the chat lines he sends. It is stored as a custom name
     * rather than looked up on demand, because that is the part Minecraft syncs to every client -
     * and it is deliberately not made visible, so he still does not walk around with a label over
     * his head. Vanilla shows the nametag of a named entity you are looking straight at, which is
     * exactly when you want to see it.
     */
    private void refreshName(ServerLevel level) {
        // Nothing has been applied yet, so look immediately; after that, once a second is plenty.
        if (this.appliedName != null && --this.nameCheckTicks > 0) {
            return;
        }

        this.nameCheckTicks = NAME_CHECK_INTERVAL;

        String wanted = HerobrineSettings.name(level);

        if (wanted.equals(this.appliedName)) {
            return;
        }

        this.appliedName = wanted;
        this.setCustomName(HerobrineName.component(wanted));
    }

    /** The classic sighting: stand still, stare, and be gone once you get close. */
    private void tickWatch(ServerLevel serverLevel, Player player) {
        boolean watcher = this.isWatcher();
        boolean lurking = this.apparition == Apparition.LURKING;
        // "Dwelling shadow": he will not leave while the room is dark.
        boolean dwellingInTheDark = this.apparition == Apparition.DWELLING
                && serverLevel.getMaxLocalRawBrightness(this.blockPosition()) < DWELLING_MIN_LIGHT;
        int maxTicks = watcher ? WATCHER_MAX_TICKS
                : lurking ? LURKING_MAX_TICKS
                : this.apparition == Apparition.DWELLING ? DWELLING_MAX_TICKS
                : MAX_STALK_TICKS;
        double vanishDistance = watcher ? WATCHER_VANISH_DISTANCE : VANISH_DISTANCE;

        if (this.lifeTicks > maxTicks && !dwellingInTheDark) {
            this.vanish(serverLevel);
            return;
        }

        this.lockEyesOn(player);

        double distance = this.distanceTo(player);

        // "Sudden scare": colliding with him screams in your face and he is gone.
        if (!watcher && distance < SCARE_DISTANCE) {
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.0F, 0.7F);
            this.vanish(serverLevel);
            return;
        }

        if (distance < vanishDistance) {
            if (!watcher) {
                strikeIfIgnored(serverLevel, player);
            }

            this.vanish(serverLevel);
            return;
        }

        // The watcher does not care that you looked back - only that you came closer. Nor does a
        // lurking one, and a dwelling one will not leave just because you found him in the dark.
        if (!watcher && !lurking && !dwellingInTheDark && this.isPlayerLookingAtMe(player)) {
            if (++this.stareTicks > MAX_STARE_TICKS) {
                this.vanish(serverLevel);
            }
        } else {
            this.stareTicks = 0;
        }
    }

    /** He is already standing behind you, and he simply waits. Hit him and he stops waiting. */
    private void tickScare(ServerLevel level, Player player) {
        this.lockEyesOn(player);

        double distance = this.distanceTo(player);

        if (this.lifeTicks > SCARE_MAX_TICKS || distance > 40.0) {
            this.vanish(level);
            return;
        }

        if (distance < SCARE_DISTANCE) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.GLITCH, SoundSource.HOSTILE, 1.0F, 0.8F);
            this.vanish(level);
        }
    }

    /** Two seconds in your face: a scream from the depths and the darkness closing in. */
    private void tickJumpscare(ServerLevel level, Player player) {
        this.lockEyesOn(player);

        if (this.lifeTicks == 1) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.JUMPSCARE, SoundSource.HOSTILE, 1.5F, 0.9F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.WHISPER, SoundSource.HOSTILE, 1.2F, 0.6F);
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, JUMPSCARE_DARKNESS_TICKS, 0));
        }

        if (this.lifeTicks > JUMPSCARE_MAX_TICKS) {
            this.vanish(level);
        }
    }

    /** A silent watcher that lunges into your face and leaves your stomach turning. */
    private void tickSpike(ServerLevel level, Player player) {
        this.lockEyesOn(player);

        if (this.lifeTicks == SPIKE_LUNGE_TICKS) {
            Vec3 front = player.getEyePosition().add(player.getViewVector(1.0F).scale(1.6));
            this.teleportTo(front.x, player.getY(), front.z);
            this.lockEyesOn(player);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.FOG, SoundSource.HOSTILE, 1.2F, 1.1F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.WHISPER, SoundSource.HOSTILE, 1.0F, 0.7F);
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, SPIKE_NAUSEA_TICKS, 0));
        }

        if (this.lifeTicks > SPIKE_MAX_TICKS) {
            this.vanish(level);
        }
    }

    /**
     * The runner, straight out of <i>Inferno's Herobrine</i>: he lets you see him and the moment you
     * look his way or start closing in he bolts - hop after hop, away from you - until he is out of
     * sight or his nerve runs out.
     */
    private void tickRunner(ServerLevel level, Player player) {
        this.lockEyesOn(player);

        double distance = this.distanceTo(player);

        if (this.lifeTicks > RUNNER_MAX_TICKS || distance > RUNNER_VANISH_DISTANCE) {
            this.vanish(level);
            return;
        }

        boolean spotted = this.isPlayerLookingAtMe(player) || distance < RUNNER_SPOOK_DISTANCE;

        if (!spotted || this.lifeTicks % RUNNER_FLEE_INTERVAL != 0) {
            return;
        }

        Vec3 away = new Vec3(this.getX() - player.getX(), 0.0, this.getZ() - player.getZ());

        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(1.0, 0.0, 0.0);
        }

        away = away.normalize().scale(RUNNER_FLEE_HOP);

        BlockPos spot = this.fleeSpot((int) Math.floor(this.getX() + away.x),
                (int) Math.floor(this.getZ() + away.z));

        if (spot != null) {
            this.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
            this.lockEyesOn(player);
        }

        level.playSound(null, this.getX(), this.getY() + 0.2, this.getZ(),
                SoundEvents.STONE_STEP, SoundSource.HOSTILE, 1.0F, RUNNER_STEP_PITCH);
    }

    /** An air pocket with something under it, near the height he is standing at. */
    private BlockPos fleeSpot(int x, int z) {
        int feet = this.blockPosition().getY();

        for (int dy = 3; dy >= -5; dy--) {
            BlockPos pos = new BlockPos(x, feet + dy, z);

            if (this.level().getBlockState(pos).isAir()
                    && this.level().getBlockState(pos.above()).isAir()
                    && !this.level().getBlockState(pos.below()).isAir()) {
                return pos;
            }
        }

        return null;
    }

    /** Stare at the player's eyes without running the look controller. */
    private void lockEyesOn(Player player) {
        this.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
        this.setYHeadRot(this.getYRot());
    }

    private boolean isPlayerLookingAtMe(Player player) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 toMe = new Vec3(
                this.getX() - player.getX(),
                this.getEyeY() - player.getEyeY(),
                this.getZ() - player.getZ());
        double distance = toMe.length();

        if (distance < 1.0E-4) {
            return true;
        }

        double dot = view.dot(toMe.normalize());
        // Roughly a 10 degree cone, widened enough to stay forgiving at long range.
        return dot > 1.0 - 0.025 / distance && player.hasLineOfSight(this);
    }

    /** "Sneaky strike": stare at nothing for too long and he hits you on the way out. */
    private void strikeIfIgnored(ServerLevel level, Player player) {
        if (this.lifeTicks < STRIKE_AFTER_TICKS || this.stareTicks > 0 || this.getRandom().nextFloat() >= STRIKE_CHANCE) {
            return;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HEARTBEAT,
                SoundSource.HOSTILE, 1.0F, 1.0F);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.hurtServer(level, level.damageSources().magic(), STRIKE_DAMAGE);
        }
    }

    /** True when any Herobrine stands within {@code radius} blocks - used to keep players awake. */
    public static boolean isNear(ServerPlayer player, double radius) {
        return !player.level().getEntitiesOfClass(HerobrineEntity.class,
                player.getBoundingBox().inflate(radius), herobrine -> true).isEmpty();
    }

    private void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.0, this.getZ(),
                25, 0.3, 0.6, 0.3, 0.02);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                ModSounds.FOG, SoundSource.HOSTILE, 1.0F, 0.6F);
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Hostile", this.isHostile());
        output.putBoolean("Elite", this.isElite());
        output.putBoolean("Watcher", this.isWatcher());
        output.putString("Apparition", this.apparition.name());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // Attributes and health are restored by super, so elite only re-applies the base values.
        this.setHostile(input.getBooleanOr("Hostile", false));
        this.setElite(input.getBooleanOr("Elite", false));
        // "Inferno" is gone: an old save that still carries the key simply has it ignored.
        this.setWatcher(input.getBooleanOr("Watcher", false));
        Apparition saved = Apparition.byName(input.getStringOr("Apparition", Apparition.WATCH.name()));
        this.apparition = saved == null ? Apparition.WATCH : saved;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        // Stalkers must survive until their own timer runs out; hostile ones are persistent.
        return !this.isHostile();
    }
}
