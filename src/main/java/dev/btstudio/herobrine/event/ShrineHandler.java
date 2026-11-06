package dev.btstudio.herobrine.event;

import dev.btstudio.herobrine.entity.HerobrineEntity;
import dev.btstudio.herobrine.entity.ModEntities;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * The summoning shrine: netherrack in the centre, a full ring of <b>eight</b> gold blocks around it,
 * and a redstone torch on top of each of the four cardinal gold blocks.
 *
 * <pre>
 *   G G G          G = gold block (8 of them)
 *   G N G          N = netherrack (centre)
 *   G G G          the four cardinals carry a redstone torch on top
 * </pre>
 *
 * Light the top of the netherrack with flint and steel. The lightning is visual only; what arrives
 * is the red-eyed hostile Herobrine. The old four-block iron rite is gone - the elite and inferno
 * forms are summoned with {@code /herobrine summon elite|inferno} instead.
 */
public final class ShrineHandler {
    private ShrineHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(level instanceof ServerLevel serverLevel)
                    || !player.getItemInHand(hand).is(Items.FLINT_AND_STEEL)
                    || hit.getDirection() != Direction.UP) {
                return InteractionResult.PASS;
            }

            BlockPos center = hit.getBlockPos();

            if (serverLevel.getBlockState(center).is(Blocks.NETHERRACK) && isShrine(serverLevel, center)) {
                summon(serverLevel, center);
            }

            // PASS lets vanilla light the fire as normal.
            return InteractionResult.PASS;
        });
    }

    /** All eight horizontal neighbours are gold, and the four cardinals carry a redstone torch. */
    private static boolean isShrine(ServerLevel level, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                if (!level.getBlockState(center.offset(dx, 0, dz)).is(Blocks.GOLD_BLOCK)) {
                    return false;
                }
            }
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(center.relative(direction).above()).is(Blocks.REDSTONE_TORCH)) {
                return false;
            }
        }

        return true;
    }

    private static void summon(ServerLevel level, BlockPos center) {
        double x = center.getX() + 0.5;
        double y = center.getY() + 1.0;
        double z = center.getZ() + 0.5;

        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);

        if (bolt != null) {
            bolt.setPos(x, y, z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        HerobrineEntity herobrine = ModEntities.HEROBRINE.create(level, EntitySpawnReason.TRIGGERED);

        if (herobrine != null) {
            herobrine.setPos(x, y + 0.1, z);
            herobrine.setHostile(true);
            level.addFreshEntity(herobrine);
        }
    }
}
