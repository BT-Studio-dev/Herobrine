package dev.btstudio.herobrine.compat;

import dev.btstudio.herobrine.network.HerobrineStatePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

import java.util.List;

/**
 * The 1.21.11 side of the handful of places where 26.3 and 1.21.11 disagree.
 *
 * <p>There are two files with this exact class and package, one under {@code src/mc26} and one
 * under {@code src/mc121}, and each build only compiles the one it needs. That is deliberate: the
 * differences below are all in the names and shapes of Minecraft's own classes, so there is no way
 * to write one call that satisfies both, and the alternative is two copies of every file that
 * touches these APIs.
 *
 * <p>The cost is that the two files can drift. Every method here must exist, with the same
 * signature, in the other one. There is no compile-time check for that, so change them together.
 *
 * <p>Where this version is simply missing something, the feature is reduced rather than removed:
 * there is no poplar wood in 1.21.11, so planted signs pick from the woods that do exist.
 */
public final class Mc {
    private Mc() {
    }

    /**
     * Show a player a message in chat. 1.21.11's {@code Player} has no one argument
     * {@code sendSystemMessage}; it has {@code displayClientMessage}, and passing false for the
     * second argument is what puts the message in chat rather than above the hotbar.
     */
    public static void sendMessage(Player player, MutableComponent message) {
        player.displayClientMessage(message, false);
    }

    /**
     * As above, but on the action bar when overlay is true. This takes a ServerPlayer rather than a
     * Player because that is what the one call site has, and because the two argument form lives on
     * ServerPlayer in both versions.
     */
    public static void sendMessage(ServerPlayer player, MutableComponent message, boolean overlay) {
        player.sendSystemMessage(message, overlay);
    }

    /**
     * The data fixer our saved data is filed under. 1.21.11 has no SAVED_DATA_WANDERING_TRADER, and
     * this mod's data is not a vanilla type whichever constant is used, so any of the SAVED_DATA_*
     * entries would do. SCOREBOARD is the closest in spirit: plain world state.
     */
    public static DataFixTypes savedDataFixType() {
        return DataFixTypes.SAVED_DATA_SCOREBOARD;
    }

    /** Register every payload this mod sends from the server to the client. */
    public static void registerClientboundPayloads() {
        PayloadTypeRegistry.playS2C()
                .register(HerobrineStatePayload.TYPE, HerobrineStatePayload.CODEC);
    }

    /** A lightning bolt for the shrine ritual, or null if one could not be made. */
    public static LightningBolt createLightning(Level level) {
        return (LightningBolt) EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
    }

    /** Every sign block this version has. 1.21.11 predates poplar wood. */
    public static Block[] signBlocks() {
        return new Block[] {
            Blocks.OAK_SIGN,
            Blocks.SPRUCE_SIGN,
            Blocks.BIRCH_SIGN,
            Blocks.JUNGLE_SIGN,
            Blocks.ACACIA_SIGN,
            Blocks.DARK_OAK_SIGN,
            Blocks.MANGROVE_SIGN,
            Blocks.CHERRY_SIGN,
            Blocks.PALE_OAK_SIGN,
            Blocks.BAMBOO_SIGN,
            Blocks.CRIMSON_SIGN,
            Blocks.WARPED_SIGN,
        };
    }

    /**
     * Write the front face of a sign. 1.21.11 has no SignTextSlot: front and back are chosen with a
     * boolean, and SignText takes arrays rather than lists.
     */
    public static void setSignText(SignBlockEntity sign, List<Component> messages, DyeColor colour) {
        Component[] lines = messages.toArray(new Component[0]);
        sign.setText(new SignText(lines, lines, colour, false), true);
    }
}
