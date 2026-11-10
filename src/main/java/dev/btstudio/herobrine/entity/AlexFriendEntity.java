package dev.btstudio.herobrine.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Alex, the friendly one's counterpart.
 *
 * <p>She is Steve in everything that matters - the same following, the same defence of you, the
 * same refusal to despawn - and different in three things only: the skin she wears (slim arms,
 * auburn hair, moss and brown where he is cyan and blue), the name she answers to, and the two
 * and a half minutes that play while she is walking with you.
 *
 * <p>She is a subclass rather than a copy because there is one set of companion behaviour in this
 * mod and it should not be possible for the two of them to drift apart. Nothing in the haunting
 * treats either of them as a threat: neither is a monster.
 */
public class AlexFriendEntity extends SteveFriendEntity {
    public AlexFriendEntity(EntityType<? extends AlexFriendEntity> type, Level level) {
        super(type, level);
        // Steve's constructor has just named him; she is not him.
        this.setCustomName(Component.translatable("herobrine.alex.name"));
        this.setCustomNameVisible(true);
    }
}
