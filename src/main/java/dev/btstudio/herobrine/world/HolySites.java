package dev.btstudio.herobrine.world;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.btstudio.herobrine.compat.Mc;
import dev.btstudio.herobrine.HerobrineMod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The cauldrons that hold <b>holy water</b> - one per church, saved with the dimension.
 *
 * <p>A water cauldron is just a water cauldron, so the only way to know which ones are holy is to
 * remember where the churches put them. Right-clicking one of these with an empty bottle fills it
 * with holy water (see {@code event/HolyWater}); any other cauldron behaves exactly as vanilla.
 */
public class HolySites extends SavedData {
    private static final Codec<List<BlockPos>> POSITIONS = BlockPos.CODEC.listOf();

    public static final Codec<HolySites> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(POSITIONS.fieldOf("sites").forGetter(HolySites::positions))
            .apply(instance, HolySites::new));

    public static final SavedDataType<HolySites> TYPE = new SavedDataType<>(
            HerobrineMod.id("holy_sites"),
            HolySites::new,
            CODEC,
            Mc.savedDataFixType());

    private final List<BlockPos> positions = new ArrayList<>();

    public HolySites() {
    }

    public HolySites(List<BlockPos> positions) {
        this.positions.addAll(positions);
    }

    public List<BlockPos> positions() {
        return List.copyOf(this.positions);
    }

    public boolean mark(BlockPos pos) {
        BlockPos immutable = pos.immutable();

        if (this.positions.contains(immutable)) {
            return false;
        }

        this.positions.add(immutable);
        this.setDirty();
        return true;
    }

    public boolean unmark(BlockPos pos) {
        boolean removed = this.positions.remove(pos);

        if (removed) {
            this.setDirty();
        }

        return removed;
    }

    public boolean contains(BlockPos pos) {
        return this.positions.contains(pos);
    }

    public static HolySites of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static boolean isHoly(ServerLevel level, BlockPos pos) {
        return of(level).contains(pos);
    }

    public static void add(ServerLevel level, BlockPos pos) {
        of(level).mark(pos);
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        of(level).unmark(pos);
    }

    public static int count(ServerLevel level) {
        return of(level).positions().size();
    }
}
