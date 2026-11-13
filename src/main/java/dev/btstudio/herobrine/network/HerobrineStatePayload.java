package dev.btstudio.herobrine.network;

import dev.btstudio.herobrine.HerobrineMod;
import dev.btstudio.herobrine.world.HerobrineName;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamMemberEncoder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The two per-dimension settings the client has to know about but cannot work out for itself.
 *
 * <ul>
 *   <li><b>fogClosesIn</b> - the fog only ever thickens around him, and the client is the side that
 *       draws fog. It needs to be told whether that is switched on in the dimension it is in.</li>
 *   <li><b>name</b> - the name he goes by, for the subtitle on the jumpscare. Everything else that
 *       names him is server-side chat, and his nametag arrives on its own as entity data.</li>
 * </ul>
 *
 * <p>Sent once on joining a dimension and again whenever either value changes, which in practice
 * means a handful of packets per world rather than one per tick.
 */
public record HerobrineStatePayload(boolean fogClosesIn, String name) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HerobrineStatePayload> TYPE =
            new CustomPacketPayload.Type<>(HerobrineMod.id("state"));

    public static final StreamCodec<FriendlyByteBuf, HerobrineStatePayload> CODEC =
            CustomPacketPayload.codec(HerobrineStatePayload::write, HerobrineStatePayload::read);

    private static final int MAX_NAME_BYTES = 64;

    private static void write(HerobrineStatePayload payload, FriendlyByteBuf buf) {
        buf.writeBoolean(payload.fogClosesIn());
        buf.writeUtf(payload.name(), MAX_NAME_BYTES);
    }

    private static HerobrineStatePayload read(FriendlyByteBuf buf) {
        boolean fog = buf.readBoolean();
        return new HerobrineStatePayload(fog, HerobrineName.sanitize(buf.readUtf(MAX_NAME_BYTES)));
    }

    /** The state a client starts with until the server tells it otherwise. */
    public static HerobrineStatePayload fallback() {
        return new HerobrineStatePayload(true, HerobrineName.DEFAULT);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
