package dev.btstudio.herobrine.world;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.btstudio.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The phantom presences saved with a dimension - invisible "second players" that keep mobs from
 * spawning around them, exactly like a real player does.
 *
 * <p>Stored per dimension (the world spawn point and every other {@link SavedData} work this way),
 * so a phantom survives a restart and never shows up anywhere a player could see it: it is not an
 * entity, has no hitbox, cannot be killed and does not appear in {@code /kill @e}.
 */
public class PhantomData extends SavedData {
    /** Not a codec for the data itself: the field only holds the coordinates. */
    private static final Codec<List<BlockPos>> POSITIONS = BlockPos.CODEC.listOf();

    public static final Codec<PhantomData> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(POSITIONS.fieldOf("phantoms").forGetter(PhantomData::positions))
            .apply(instance, PhantomData::new));

    /**
     * 26.3 stores saved data through a {@link SavedDataType}. No data fixers are registered for
     * the wandering-trader type, so it only carries the data version across a load - which is all
     * a mod-owned file needs.
     */
    public static final SavedDataType<PhantomData> TYPE = new SavedDataType<>(
            HerobrineMod.id("phantoms"),
            PhantomData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_WANDERING_TRADER);

    private final List<BlockPos> positions = new ArrayList<>();

    public PhantomData() {
    }

    public PhantomData(List<BlockPos> positions) {
        this.positions.addAll(positions);
    }

    public List<BlockPos> positions() {
        return List.copyOf(this.positions);
    }

    public boolean add(BlockPos pos) {
        BlockPos immutable = pos.immutable();

        if (this.positions.contains(immutable)) {
            return false;
        }

        this.positions.add(immutable);
        this.setDirty();
        return true;
    }

    public int clear() {
        int removed = this.positions.size();

        if (removed > 0) {
            this.positions.clear();
            this.setDirty();
        }

        return removed;
    }

    /** Allocation-free check used on the mob spawning hot path. */
    public boolean isNear(BlockPos pos, double radiusSqr) {
        for (BlockPos phantom : this.positions) {
            if (phantom.distSqr(pos) <= radiusSqr) {
                return true;
            }
        }

        return false;
    }

    /** Squared distance to the closest phantom, or {@link Double#MAX_VALUE} when there is none. */
    public double nearestDistanceSqr(BlockPos pos) {
        double nearest = Double.MAX_VALUE;

        for (BlockPos phantom : this.positions) {
            nearest = Math.min(nearest, phantom.distSqr(pos));
        }

        return nearest;
    }
}
