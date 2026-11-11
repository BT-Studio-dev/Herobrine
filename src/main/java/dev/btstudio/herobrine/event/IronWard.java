package dev.btstudio.herobrine.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.btstudio.herobrine.compat.Mc;
import dev.btstudio.herobrine.entity.HerobrineEntity;
import dev.btstudio.herobrine.item.ModItems;
import dev.btstudio.herobrine.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Iron repels him. Everything hammered out of iron sticks - the stick itself, the sword and the
 * four pieces of armour - makes him keep his distance:
 *
 * <ul>
 *   <li><b>Carried iron</b> lowers the chance of an apparition. Armour counts per piece, an iron
 *       sword or stick in either hand counts, and iron lying on the ground within
 *       {@value #GROUND_RADIUS} blocks counts a little too. Nothing ever turns him away more often
 *       than {@value #MAX_WARD} times in ten, so a full set of iron is a strong ward, not a wall.</li>
 *   <li><b>Struck with iron</b> he cannot stay. Hit an apparition with the sword (or with a bare
 *       iron stick) and he recoils and is gone instead of dropping the act and hunting you. Hit the
 *       hostile form and the blow drives him back a few blocks - the shrine's Herobrine is not
 *       banished by a piece of metal, but he will not stand in it either.</li>
 * </ul>
 *
 * <p>None of this touches a single block, and purist mode is left exactly as it was: he only ever
 * watches there, and iron does not make him watch less.
 */
public final class IronWard {
    /** Iron on the ground wards him off from this far away. */
    private static final double GROUND_RADIUS = 6.0;

    /** How much of the ward each source carries. */
    private static final float ARMOUR_WARD = 0.10F;
    private static final float HELD_WARD = 0.15F;
    private static final float GROUND_WARD = 0.05F;

    /** A heap of dropped iron is not twice the ward of two sticks. */
    private static final int MAX_GROUND_STACKS = 2;
    /** Full armour and a sword in hand: six apparitions in ten are turned away, never more. */
    private static final float MAX_WARD = 0.60F;

    /** How far a hostile Herobrine is driven back when the iron meets him. */
    private static final double RECOIL_DISTANCE = 7.0;

    /** "The iron holds" is worth saying, but not every time it happens. */
    private static final int WARD_MESSAGE_COOLDOWN_TICKS = 2_400;
    private static final Map<UUID, Long> LAST_WARD_MESSAGE = new HashMap<>();

    /** The four armour slots, in the order a player puts the set on. */
    private static final List<EquipmentSlot> ARMOUR_SLOTS =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    /** Everything hammered out of iron sticks. */
    private static final List<Item> IRON_GEAR = List.of(ModItems.IRON_STICK, ModItems.IRON_SWORD,
            ModItems.IRON_HELMET, ModItems.IRON_CHESTPLATE, ModItems.IRON_LEGGINGS, ModItems.IRON_BOOTS);

    private IronWard() {
    }

    /** True for the mod's iron-stick gear: the stick, the sword, and the four armour pieces. */
    public static boolean isIronGear(ItemStack stack) {
        return !stack.isEmpty() && IRON_GEAR.contains(stack.getItem());
    }

    /**
     * How strongly iron keeps him away from this player: {@code 0.0} with none of it on them, up to
     * {@value #MAX_WARD} in a full iron-stick set with the sword drawn and iron at their feet.
     */
    public static float wardOf(ServerPlayer player) {
        float ward = 0.0F;

        for (EquipmentSlot slot : ARMOUR_SLOTS) {
            if (isIronGear(player.getItemBySlot(slot))) {
                ward += ARMOUR_WARD;
            }
        }

        if (isIronGear(player.getMainHandItem())) {
            ward += HELD_WARD;
        }

        if (isIronGear(player.getOffhandItem())) {
            ward += HELD_WARD;
        }

        ward += groundWard(player);
        return Math.min(MAX_WARD, ward);
    }

    /**
     * Rolls one apparition against the iron the player is carrying. True means he thought better of
     * it and the sighting does not happen.
     */
    public static boolean turnsAway(ServerPlayer player, RandomSource random) {
        float ward = wardOf(player);

        if (ward <= 0.0F || random.nextFloat() >= ward) {
            return false;
        }

        tellWarded(player);
        return true;
    }

    /**
     * "An iron-tipped weapon makes him retreat." Called from
     * {@link HerobrineEntity#hurtServer(ServerLevel, DamageSource, float)} before he drops the act:
     * an apparition struck with iron is gone, a hunting one is driven back and still takes the hit.
     *
     * @return true when he was driven off and no damage should be applied
     */
    public static boolean repel(ServerLevel level, HerobrineEntity herobrine, DamageSource source) {
        if (!(source.getEntity() instanceof Player player)
                || (!isIronGear(player.getMainHandItem()) && !isIronGear(player.getOffhandItem()))) {
            return false;
        }

        if (herobrine.isHostile()) {
            recoil(level, herobrine, player);
            return false;
        }

        retreat(level, herobrine, player);
        return true;
    }

    /** Iron lying on the ground nearby counts too, up to two stacks of it. */
    private static float groundWard(ServerPlayer player) {
        List<ItemEntity> dropped = player.level().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(GROUND_RADIUS),
                item -> item.isAlive() && isIronGear(item.getItem()));

        return Math.min(MAX_GROUND_STACKS, dropped.size()) * GROUND_WARD;
    }

    /** He cannot stay: a puff of smoke where he was standing, and the sound of him leaving. */
    private static void retreat(ServerLevel level, HerobrineEntity herobrine, Player player) {
        BlockPos pos = herobrine.blockPosition();

        level.sendParticles(ParticleTypes.SMOKE, herobrine.getX(), herobrine.getY() + 1.0, herobrine.getZ(),
                30, 0.3, 0.6, 0.3, 0.02);
        level.playSound(null, pos, ModSounds.FOG, SoundSource.HOSTILE, 1.0F, 0.6F);

        herobrine.discard();
        Mc.sendMessage(player, Component.translatable("herobrine.iron.retreat").withStyle(ChatFormatting.GRAY));
    }

    /**
     * The hunting form is not banished by a piece of metal, but it will not stand in one: he is put
     * down {@value #RECOIL_DISTANCE} blocks further away, on ground that is already clear.
     */
    private static void recoil(ServerLevel level, HerobrineEntity herobrine, Player player) {
        Vec3 away = new Vec3(herobrine.getX() - player.getX(), 0.0, herobrine.getZ() - player.getZ());

        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(1.0, 0.0, 0.0);
        }

        away = away.normalize().scale(RECOIL_DISTANCE);
        BlockPos spot = herobrine.blockPosition().offset((int) Math.round(away.x), 0, (int) Math.round(away.z));

        if (level.getBlockState(spot).isAir() && level.getBlockState(spot.above()).isAir()) {
            herobrine.teleportTo(spot.getX() + 0.5, herobrine.getY(), spot.getZ() + 0.5);
        }

        level.playSound(null, herobrine.blockPosition(), ModSounds.GLITCH, SoundSource.HOSTILE, 1.0F, 1.1F);
        Mc.sendMessage(player, Component.translatable("herobrine.iron.recoil").withStyle(ChatFormatting.GRAY));
    }

    /** One line on the action bar, at most once every two minutes, so the ward is never a mystery. */
    private static void tellWarded(ServerPlayer player) {
        ServerLevel level = player.level();
        long gameTime = level.getGameTime();
        Long last = LAST_WARD_MESSAGE.get(player.getUUID());

        if (last != null && gameTime - last < WARD_MESSAGE_COOLDOWN_TICKS) {
            return;
        }

        LAST_WARD_MESSAGE.put(player.getUUID(), gameTime);
        Mc.sendMessage(player, Component.translatable("herobrine.iron.ward").withStyle(ChatFormatting.GRAY), true);
    }

    /** Drops what we were holding about a player who has left. */
    public static void forget(UUID id) {
        LAST_WARD_MESSAGE.remove(id);
    }
}
