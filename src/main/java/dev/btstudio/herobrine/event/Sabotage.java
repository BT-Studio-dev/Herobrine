package dev.btstudio.herobrine.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import dev.btstudio.herobrine.world.HerobrineName;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The things Herobrine does to your base while you are not looking: he takes your torches, swaps
 * the rest for redstone and soul light, blows out your candles, opens your doors, leaves signs with
 * one line on them and drops "gifts" into your chests.
 *
 * <p>Everything here works on blocks that already exist within a dozen or so blocks of the player
 * and only ever touches light fixtures, doors, candles, chests and empty air - never your walls,
 * your storage contents (donations fill an empty slot) or anything outside the search radius.
 */
public final class Sabotage {
    /** How far around the player we look for things to ruin. */
    private static final int SEARCH_RADIUS = 12;
    /** How many blocks one event may touch, so a big base does not lose every torch at once. */
    private static final int MAX_CHANGES = 4;

    /**
     * The lang keys of the one line he leaves behind. Inferno's signs are short - "Stop", warnings
     * in Swedish - and the three in the middle are the ones that name him, which is why every line
     * is translated with his name as an argument: the ones that do not use it simply drop it.
     */
    private static final String[] SIGN_KEYS = {
            "herobrine.sign.stop",
            "herobrine.sign.come_outside",
            "herobrine.sign.i_see_you",
            "herobrine.sign.leave_now",
            "herobrine.sign.was_here",
            "herobrine.sign.sees_you",
            "herobrine.sign.is_here",
            "herobrine.sign.look_behind_you",
            "herobrine.sign.dont_sleep",
            "herobrine.sign.wake_up",
            "herobrine.sign.run",
            // Swedish warnings, straight out of the same mod.
            "herobrine.sign.jag_ser_dig",
            "herobrine.sign.akta_dig",
            "herobrine.sign.spring",
    };

    /** The sign is not always oak: Inferno's mod ships every wood variant. */
    private static final Block[] SIGN_BLOCKS = {
            Blocks.OAK_SIGN,
            Blocks.SPRUCE_SIGN,
            Blocks.BIRCH_SIGN,
            Blocks.JUNGLE_SIGN,
            Blocks.ACACIA_SIGN,
            Blocks.DARK_OAK_SIGN,
            Blocks.MANGROVE_SIGN,
            Blocks.CHERRY_SIGN,
            Blocks.PALE_OAK_SIGN,
            Blocks.POPLAR_SIGN,
            Blocks.BAMBOO_SIGN,
            Blocks.CRIMSON_SIGN,
            Blocks.WARPED_SIGN,
    };

    private static final Item[] DONATIONS = {
            Items.BONE,
            Items.ROTTEN_FLESH,
            Items.SPIDER_EYE,
            Items.GUNPOWDER,
            Items.FLINT,
    };

    private Sabotage() {
    }

    /** "Disappearing torches": a few light sources around the player simply cease to exist. */
    public static int snuffLights(ServerLevel level, ServerPlayer player, RandomSource random) {
        List<BlockPos> lights = scan(level, player, SEARCH_RADIUS, Sabotage::isLightFixture);

        Collections.shuffle(lights, new java.util.Random(random.nextLong()));

        int removed = 0;

        for (BlockPos pos : lights) {
            if (removed >= MAX_CHANGES) {
                break;
            }

            level.removeBlock(pos, false);
            removed++;
        }

        return removed;
    }

    /** "Crimson curse": torches become redstone torches and lanterns become soul lanterns. */
    public static int curseLights(ServerLevel level, ServerPlayer player, RandomSource random) {
        List<BlockPos> lights = scan(level, player, SEARCH_RADIUS, Sabotage::isCurseable);

        Collections.shuffle(lights, new java.util.Random(random.nextLong()));

        int changed = 0;

        for (BlockPos pos : lights) {
            if (changed >= MAX_CHANGES) {
                break;
            }

            BlockState state = level.getBlockState(pos);

            if (state.is(Blocks.TORCH)) {
                level.setBlock(pos, Blocks.REDSTONE_TORCH.defaultBlockState(), 3);
                changed++;
            } else if (state.is(Blocks.LANTERN)) {
                level.setBlock(pos, Blocks.SOUL_LANTERN.defaultBlockState()
                        .setValue(LanternBlock.HANGING, state.getValue(LanternBlock.HANGING)), 3);
                changed++;
            }
        }

        return changed;
    }

    /** "Chilled candles": every candle and campfire in range goes out. */
    public static int douseFlames(ServerLevel level, ServerPlayer player, RandomSource random) {
        List<BlockPos> flames = scan(level, player, SEARCH_RADIUS, Sabotage::isBurning);

        Collections.shuffle(flames, new java.util.Random(random.nextLong()));

        int doused = 0;

        for (BlockPos pos : flames) {
            if (doused >= MAX_CHANGES) {
                break;
            }

            BlockState state = level.getBlockState(pos);
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
            level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            doused++;
        }

        return doused;
    }

    /**
     * "Sinister signs": a sign appears nearby with a single line on it, and the lines that name him
     * use the name this world has given him.
     */
    public static boolean creepySign(ServerLevel level, ServerPlayer player, RandomSource random,
                                     String name) {
        BlockPos pos = findSignSpot(level, player, random);

        if (pos == null) {
            return false;
        }

        BlockState signState = SIGN_BLOCKS[random.nextInt(SIGN_BLOCKS.length)].defaultBlockState();
        level.setBlock(pos, signState, 3);

        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            return true;
        }

        List<Component> messages = new ArrayList<>(List.of(Component.empty(), Component.empty(),
                Component.empty(), Component.empty()));
        String key = SIGN_KEYS[random.nextInt(SIGN_KEYS.length)];
        messages.set(random.nextInt(messages.size()),
                Component.translatable(key, HerobrineName.component(name)));

        sign.setText(new SignText(messages, messages, DyeColor.RED, false), SignTextSlot.FRONT);
        return true;
    }

    /** "Dreadful donations": something unpleasant is waiting in an empty chest slot. */
    public static boolean donation(ServerLevel level, ServerPlayer player, RandomSource random) {
        List<BlockPos> containers = scan(level, player, SEARCH_RADIUS, state -> state.hasBlockEntity());

        Collections.shuffle(containers, new java.util.Random(random.nextLong()));

        for (BlockPos pos : containers) {
            if (!(level.getBlockEntity(pos) instanceof Container container)) {
                continue;
            }

            int size = container.getContainerSize();
            int start = size > 0 ? random.nextInt(size) : 0;

            for (int i = 0; i < size; i++) {
                int slot = (start + i) % size;

                if (container.getItem(slot).isEmpty()) {
                    container.setItem(slot, new ItemStack(DONATIONS[random.nextInt(DONATIONS.length)]));
                    container.setChanged();
                    return true;
                }
            }
        }

        return false;
    }

    /** "Ghost doors": a door nearby swings open or shut on its own. */
    public static boolean ghostDoor(ServerLevel level, ServerPlayer player, RandomSource random) {
        List<BlockPos> doors = scan(level, player, SEARCH_RADIUS, state -> state.getBlock() instanceof DoorBlock);

        if (doors.isEmpty()) {
            return false;
        }

        BlockPos pos = doors.get(random.nextInt(doors.size()));
        BlockState state = level.getBlockState(pos);
        boolean open = !state.getValue(DoorBlock.OPEN);

        level.setBlock(pos, state.setValue(DoorBlock.OPEN, open), 3);

        BlockPos other = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        BlockState otherState = level.getBlockState(other);

        if (otherState.getBlock() instanceof DoorBlock) {
            level.setBlock(other, otherState.setValue(DoorBlock.OPEN, open), 3);
        }

        level.playSound(null, pos, open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    /** "Burning base": a single block of fire, next to you, where nothing was burning before. */
    public static boolean arson(ServerLevel level, ServerPlayer player, RandomSource random) {
        for (int attempt = 0; attempt < 32; attempt++) {
            BlockPos pos = randomNear(level, player, random, 4, 11);

            if (!level.getBlockState(pos).isAir()) {
                continue;
            }

            BlockPos below = pos.below();

            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                    || !level.getFluidState(pos).isEmpty()) {
                continue;
            }

            BlockState fire = BaseFireBlock.getState(level, pos);

            if (!fire.canSurvive(level, pos)) {
                continue;
            }

            level.setBlock(pos, fire, 3);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 1.4F);
            return true;
        }

        return false;
    }

    private static boolean isLightFixture(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH)
                || state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN);
    }

    private static boolean isCurseable(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.LANTERN);
    }

    private static boolean isBurning(BlockState state) {
        return (state.getBlock() instanceof AbstractCandleBlock || state.getBlock() instanceof CampfireBlock)
                && state.hasProperty(BlockStateProperties.LIT)
                && state.getValue(BlockStateProperties.LIT);
    }

    private static List<BlockPos> scan(ServerLevel level, ServerPlayer player, int radius, Predicate<BlockState> filter) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos centre = player.blockPosition();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = centre.offset(dx, dy, dz);

                    if (filter.test(level.getBlockState(pos))) {
                        found.add(pos);
                    }
                }
            }
        }

        return found;
    }

    private static BlockPos findSignSpot(ServerLevel level, ServerPlayer player, RandomSource random) {
        for (int attempt = 0; attempt < 48; attempt++) {
            BlockPos pos = randomNear(level, player, random, 6, 14);
            BlockState state = Blocks.OAK_SIGN.defaultBlockState();

            if (level.getBlockState(pos).isAir() && state.canSurvive(level, pos)) {
                return pos;
            }
        }

        return null;
    }

    private static BlockPos randomNear(ServerLevel level, ServerPlayer player, RandomSource random,
                                       int minDistance, int maxDistance) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = minDistance + random.nextInt(maxDistance - minDistance + 1);
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
        int y = (int) Math.floor(player.getY()) + random.nextInt(7) - 3;

        return new BlockPos(x, y, z);
    }
}
