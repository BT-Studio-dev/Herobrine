package dev.btstudio.herobrine.compat;

import dev.btstudio.herobrine.network.HerobrineStatePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;

import java.util.List;

/**
 * The 26.3 side of the handful of places where 26.3 and 1.21.11 disagree.
 *
 * <p>There are two files with this exact class and package, one under {@code src/mc26} and one
 * under {@code src/mc121}, and each build only compiles the one it needs. That is deliberate: the
 * differences below are all in the names and shapes of Minecraft's own classes, so there is no way
 * to write one call that satisfies both, and the alternative is two copies of every file that
 * touches these APIs.
 *
 * <p>The cost is that the two files can drift. Every method here must exist, with the same
 * signature, in the other one. There is no compile-time check for that, so change them together.
 */
public final class Mc {
    private Mc() {
    }

    /** Show a player a message in chat, not on the action bar. */
    public static void sendMessage(Player player, MutableComponent message) {
        player.sendSystemMessage(message);
    }

    /**
     * As above, with the message on the action bar instead of in chat when overlay is true. Both
     * versions spell this displayClientMessage, which is the only form Player itself carries in
     * both; sendSystemMessage's second argument lives on ServerPlayer and is not on Player here.
     */
    public static void sendMessage(Player player, MutableComponent message, boolean overlay) {
        player.displayClientMessage(message, overlay);
    }

    /** The data fixer our saved data is filed under. */
    public static DataFixTypes savedDataFixType() {
        return DataFixTypes.SAVED_DATA_WANDERING_TRADER;
    }

    /** Register every payload this mod sends from the server to the client. */
    public static void registerClientboundPayloads() {
        PayloadTypeRegistry.clientboundPlay()
                .register(HerobrineStatePayload.TYPE, HerobrineStatePayload.CODEC);
    }

    /** A lightning bolt for the shrine ritual, or null if one could not be made. */
    public static LightningBolt createLightning(Level level) {
        return EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
    }

    /** Every sign block in the game, so a planted sign is never a missing block. */
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
            Blocks.POPLAR_SIGN,
            Blocks.BAMBOO_SIGN,
            Blocks.CRIMSON_SIGN,
            Blocks.WARPED_SIGN,
        };
    }

    /** Write the front face of a sign. */
    public static void setSignText(SignBlockEntity sign, List<Component> messages, DyeColor colour) {
        sign.setText(new SignText(messages, messages, colour, false), SignTextSlot.FRONT);
    }
}
