package dev.btstudio.herobrine.event;

import dev.btstudio.herobrine.compat.Mc;
import dev.btstudio.herobrine.entity.HerobrineEntity;
import dev.btstudio.herobrine.entity.ModEntities;
import dev.btstudio.herobrine.world.Feature;
import dev.btstudio.herobrine.world.HerobrineSettings;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * The summoning shrine, sunk one block into the ground: netherrack in the centre, a full ring of
 * <b>eight</b> gold blocks around it at the same level, and a redstone torch standing on each of the
 * four cardinal gold blocks. The plate is recessed, so the torches sit at the pit's rim and only
 * they and the netherrack break the surface.
 *
 * <pre>
 *   from above:       side on:
 *     . T .             ~ T ~          G = gold block (8 of them), below the surface
 *     T N T             G N G          N = netherrack (centre), below the surface
 *     . T .             ~ ~ ~          T = redstone torch (4 of them), at the rim
 *                                      ~ = the surface the pit is cut into
 * </pre>
 *
 * Light the top of the netherrack with flint and steel. The lightning is visual only; what arrives
 * is the red-eyed hostile Herobrine. The old four-block iron rite is gone - the elite form is
 * summoned with {@code /herobrine summon elite} instead.
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

            if (HerobrineSettings.enabled(serverLevel, Feature.SHRINE)
                    && serverLevel.getBlockState(center).is(Blocks.NETHERRACK)
                    && isShrine(serverLevel, center)) {
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

        LightningBolt bolt = Mc.createLightning(level);

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
