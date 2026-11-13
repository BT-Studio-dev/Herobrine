package dev.btstudio.herobrine.event;

import java.util.ArrayList;
import java.util.List;

import dev.btstudio.herobrine.item.ModItems;
import dev.btstudio.herobrine.world.Feature;
import dev.btstudio.herobrine.world.HerobrineSettings;
import dev.btstudio.herobrine.world.HolySites;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The churches of <i>Herobrine: Forgotten</i>: a small cobblestone chapel that stands in the plains,
 * half of it already forgotten, with a cauldron of <b>holy water</b> on the altar and a chest the
 * mod leaves behind.
 *
 * <p>In the original these are a world-gen structure. They are built here on demand instead - from
 * the haunting roll ("things spawn that you don't remember building") or from
 * {@code /herobrine church} - which keeps them out of the world generator and lets them react to
 * where the player is. Every block of the footprint has to be air with solid ground underneath, so
 * a church can never be dropped on top of a base.
 */
public final class Churches {
    /** Half width and half depth of the chapel, in blocks. */
    private static final int HALF_WIDTH = 3;
    private static final int HALF_DEPTH = 4;
    private static final int WALL_HEIGHT = 3;
    private static final int ROOF_HEIGHT = 4;
    private static final int RIDGE_HEIGHT = 5;

    /** Only plains, and never this close to the player who triggered it. */
    private static final int MIN_DISTANCE = 40;
    private static final int MAX_DISTANCE = 64;

    private Churches() {
    }

    /**
     * Tries to raise a chapel somewhere in the plains around the player.
     *
     * @return true when one was built
     */
    public static boolean spawnNearPlayer(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (!HerobrineSettings.enabled(level, Feature.CHURCHES)) {
            return false;
        }

        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            BlockPos origin = new BlockPos(x,
                    level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);

            if (!level.getBiome(origin).is(Biomes.PLAINS)) {
                continue;
            }

            if (!isFlat(level, origin)) {
                continue;
            }

            if (build(level, origin)) {
                return true;
            }
        }

        return false;
    }

    /** Raises the chapel with {@code origin} as the block the player would stand on inside it. */
    public static boolean build(ServerLevel level, BlockPos origin) {
        if (!HerobrineSettings.enabled(level, Feature.CHURCHES)) {
            return false;
        }

        if (!level.getBlockState(origin.below()).isFaceSturdy(level, origin.below(), Direction.UP)) {
            return false;
        }

        List<BlockPos> footprint = new ArrayList<>();

        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx++) {
            for (int dz = -HALF_DEPTH; dz <= HALF_DEPTH; dz++) {
                for (int dy = 0; dy <= RIDGE_HEIGHT; dy++) {
                    footprint.add(origin.offset(dx, dy, dz));
                }
            }
        }

        // Never drop a church through a wall, a tree or a build: the whole volume has to be air.
        for (BlockPos pos : footprint) {
            if (!level.getBlockState(pos).isAir()) {
                return false;
            }
        }

        // Floor.
        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx++) {
            for (int dz = -HALF_DEPTH; dz <= HALF_DEPTH; dz++) {
                level.setBlock(origin.offset(dx, 0, dz), Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        }

        // Walls, with glass in the long sides and a doorway at the front.
        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx++) {
            for (int dz = -HALF_DEPTH; dz <= HALF_DEPTH; dz++) {
                boolean wall = Math.abs(dx) == HALF_WIDTH || Math.abs(dz) == HALF_DEPTH;

                if (!wall) {
                    continue;
                }

                for (int dy = 1; dy <= WALL_HEIGHT; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);

                    if (dz == HALF_DEPTH && dx == 0 && dy <= 2) {
                        continue; // the doorway
                    }

                    boolean window = dy == 2 && (Math.abs(dx) == HALF_WIDTH ? dz % 2 == 0 : dx % 2 == 0);
                    level.setBlock(pos, window ? Blocks.GLASS_PANE.defaultBlockState()
                            : Blocks.COBBLESTONE.defaultBlockState(), 3);
                }
            }
        }

        // Roof and a low ridge along the middle.
        for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx++) {
            for (int dz = -HALF_DEPTH; dz <= HALF_DEPTH; dz++) {
                level.setBlock(origin.offset(dx, ROOF_HEIGHT, dz), Blocks.OAK_PLANKS.defaultBlockState(), 3);

                if (dx == 0) {
                    level.setBlock(origin.offset(dx, RIDGE_HEIGHT, dz), Blocks.OAK_PLANKS.defaultBlockState(), 3);
                }
            }
        }

        // The altar: stone bricks across the back, holy water on top.
        for (int dx = -1; dx <= 1; dx++) {
            level.setBlock(origin.offset(dx, 1, -HALF_DEPTH + 1), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }

        BlockPos altar = origin.offset(0, 2, -HALF_DEPTH + 1);
        level.setBlock(altar, Blocks.WATER_CAULDRON.defaultBlockState(), 3);
        HolySites.add(level, altar);

        fillChest(level, origin.offset(HALF_WIDTH - 1, 1, -HALF_DEPTH + 1));

        return true;
    }

    /** Four iron sticks and two bottles: the rest of the cross, waiting where he left it. */
    private static void fillChest(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);

        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(ModItems.IRON_STICK, 4));
            chest.setItem(1, new ItemStack(Items.GLASS_BOTTLE, 2));
        }
    }

    /** A church needs level ground: the four corners of the footprint within one block of each other. */
    private static boolean isFlat(ServerLevel level, BlockPos origin) {
        int height = origin.getY();

        for (int dx : new int[]{-HALF_WIDTH, HALF_WIDTH}) {
            for (int dz : new int[]{-HALF_DEPTH, HALF_DEPTH}) {
                int corner = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        origin.getX() + dx, origin.getZ() + dz);

                if (Math.abs(corner - height) > 1) {
                    return false;
                }
            }
        }

        return true;
    }
}
