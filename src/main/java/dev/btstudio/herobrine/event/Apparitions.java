package dev.btstudio.herobrine.event;

import dev.btstudio.herobrine.entity.Apparition;
import dev.btstudio.herobrine.entity.HerobrineEntity;
import dev.btstudio.herobrine.entity.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Spawns the four <i>Him</i> apparitions in the right place: behind you, in front of your face, or
 * out on the horizon.
 *
 * <p>Every helper here only ever picks a spot that already has two blocks of air and solid ground
 * underneath, so nothing is ever carved out of the world to make room for him.
 *
 * <p>The End is the awkward one: it is mostly void, so the surface heightmap that the Overworld
 * sightings lean on reports nothing useful out there. There the search looks for the island's own
 * ground - end stone and obsidian - by scanning the columns it picks, and if the horizon really has
 * nothing to stand on it falls back to whatever ground there is next to the player.
 */
public final class Apparitions {
    /** He stands this far behind you for a scare. */
    private static final int SCARE_DISTANCE = 3;
    /** ...this far in front of you for a jumpscare. */
    private static final int JUMPSCARE_DISTANCE = 4;
    /** ...and this far away for a spike. */
    private static final int SPIKE_DISTANCE = 9;

    /**
     * How far above and below the player to look for End ground. The island sits around Y 63 and
     * the outer islands a little higher, so this covers standing on one or falling past it.
     */
    private static final int END_SCAN_UP = 12;
    private static final int END_SCAN_DOWN = 24;

    private Apparitions() {
    }

    /**
     * Creates and places one apparition, already facing the player. Returns null when no valid
     * standing spot was found, in which case nothing is added to the world.
     */
    public static HerobrineEntity spawn(ServerLevel level, ServerPlayer player, Apparition apparition,
                                        RandomSource random) {
        BlockPos pos = switch (apparition) {
            case SCARE -> spotNear(level, player.blockPosition()
                    .relative(player.getDirection().getOpposite(), SCARE_DISTANCE), 4, random);
            case JUMPSCARE -> spotNear(level, player.blockPosition()
                    .relative(player.getDirection(), JUMPSCARE_DISTANCE), 4, random);
            case SPIKE -> spotNear(level, player.blockPosition()
                    .relative(player.getDirection(), SPIKE_DISTANCE), 5, random);
            case WATCH -> farSpot(level, player, random, 18, 32);
            case RUNNER -> farSpot(level, player, random, 14, 26);
            // Two to four times a normal sighting away - up to the full 128 blocks.
            case LURKING -> farSpot(level, player, random, 60, 120);
            case DWELLING -> spotNear(level, player.blockPosition()
                    .relative(player.getDirection().getOpposite(), 8), 4, random);
        };

        // The End is mostly void, so a horizon sighting can come back empty even when the player
        // is standing on the island. Rather than give up, take whatever ground is next to them.
        if (pos == null && isEnd(level)) {
            pos = spotNear(level, player.blockPosition(), 6, random);
        }

        if (pos == null) {
            return null;
        }

        HerobrineEntity herobrine = ModEntities.HEROBRINE.create(level, EntitySpawnReason.EVENT);

        if (herobrine == null) {
            return null;
        }

        herobrine.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        herobrine.setApparition(apparition);
        herobrine.setHostile(false);
        facePlayer(herobrine, player);

        if (!level.addFreshEntity(herobrine)) {
            herobrine.discard();
            return null;
        }

        return herobrine;
    }

    /**
     * Places an apparition on an exact block (used by the bed sighting, which knows where the bed
     * is). Nothing is added when the spot is not two blocks of air over solid ground.
     */
    public static HerobrineEntity spawnAt(ServerLevel level, BlockPos pos, Apparition apparition, Player player) {
        if (!isStandable(level, pos)) {
            return null;
        }

        HerobrineEntity herobrine = ModEntities.HEROBRINE.create(level, EntitySpawnReason.EVENT);

        if (herobrine == null) {
            return null;
        }

        herobrine.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        herobrine.setApparition(apparition);
        herobrine.setHostile(false);
        facePlayer(herobrine, player);

        if (!level.addFreshEntity(herobrine)) {
            herobrine.discard();
            return null;
        }

        return herobrine;
    }

    /** Points him at the player without touching the look controller. */
    private static void facePlayer(HerobrineEntity herobrine, Player player) {
        double dx = player.getX() - herobrine.getX();
        double dz = player.getZ() - herobrine.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;

        herobrine.setYRot(yaw);
        herobrine.setYHeadRot(yaw);
    }

    /**
     * A standing spot around the given column, searching straight up and down first and then a few
     * small horizontal offsets, so a wall right behind the player does not kill the event.
     */
    private static BlockPos spotNear(ServerLevel level, BlockPos around, int verticalRange, RandomSource random) {
        for (int attempt = 0; attempt < 6; attempt++) {
            BlockPos column = attempt == 0
                    ? around
                    : around.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);

            for (int dy = 0; dy <= verticalRange; dy++) {
                BlockPos up = column.above(dy);

                if (isStandable(level, up)) {
                    return up;
                }

                if (dy > 0) {
                    BlockPos down = column.below(dy);

                    if (isStandable(level, down)) {
                        return down;
                    }
                }
            }
        }

        return null;
    }

    /** Surface sighting somewhere on the horizon, or the same cave layer for underground players. */
    private static BlockPos farSpot(ServerLevel level, ServerPlayer player, RandomSource random,
                                    int minDistance, int maxDistance) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = minDistance + random.nextInt(maxDistance - minDistance + 1);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);

            // In the End the heightmap is mostly void, and what ground there is end stone or
            // obsidian, so scan the column for the island instead of asking for the surface.
            if (isEnd(level)) {
                BlockPos end = endGround(level, player, x, z, random);

                if (end != null) {
                    return end;
                }

                continue;
            }

            // Under the Nether's bedrock roof the heightmap is useless: stay on the player's level.
            boolean underground = player.getY() < 50.0 || level.dimension().equals(Level.NETHER);

            if (!underground) {
                BlockPos surface = new BlockPos(x,
                        level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);

                if (isStandable(level, surface)) {
                    return surface;
                }
            }

            if (underground) {
                // A single random Y rarely lands exactly on a cave floor. Search the nearby
                // vertical range (and a few adjacent columns) for a safe standing position.
                BlockPos cave = spotNear(level, new BlockPos(x, (int) Math.floor(player.getY()), z), 6, random);

                if (cave != null) {
                    return cave;
                }
            }
        }

        return null;
    }

    /** True in the End, where the ground has to be hunted for rather than looked up. */
    private static boolean isEnd(ServerLevel level) {
        return level.dimension().equals(Level.END);
    }

    /**
     * Ground out on the End island: end stone or obsidian with two blocks of air above it. The
     * heightmap is no help over the void, so the column is scanned around the player's height
     * instead, with a couple of neighbouring columns tried before giving up on this spot.
     */
    private static BlockPos endGround(ServerLevel level, ServerPlayer player, int x, int z,
                                      RandomSource random) {
        int centerY = (int) Math.floor(player.getY());
        int lowest = Math.max(level.getMinY(), centerY - END_SCAN_DOWN);
        int highest = Math.min(level.getMaxY() - 2, centerY + END_SCAN_UP);

        for (int attempt = 0; attempt < 3; attempt++) {
            int columnX = attempt == 0 ? x : x + random.nextInt(5) - 2;
            int columnZ = attempt == 0 ? z : z + random.nextInt(5) - 2;

            for (int y = highest; y >= lowest; y--) {
                BlockPos pos = new BlockPos(columnX, y, columnZ);

                if (isStandable(level, pos) && isEndGround(level, pos.below())) {
                    return pos;
                }
            }
        }

        return null;
    }

    /** What he will actually stand on out there: the island itself, or obsidian. */
    private static boolean isEndGround(ServerLevel level, BlockPos pos) {
        return isEndGround(level.getBlockState(pos));
    }

    private static boolean isEndGround(BlockState state) {
        return state.is(Blocks.END_STONE) || state.is(Blocks.OBSIDIAN);
    }

    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
}
