package dev.btstudio.herobrine.item;

import dev.btstudio.herobrine.entity.HerobrineEntity;
import dev.btstudio.herobrine.event.Crucifix;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The Cross. It does nothing to animals, villagers or other players - only to him.
 *
 * <p>Right-click Herobrine with it and the crucifixion goes one of two ways, which is the point of
 * the original mod: either he cannot stand it and is gone, or it was never going to work and he
 * stops pretending to be shy.
 */
public class CrossItem extends Item {
    public CrossItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (!(target instanceof HerobrineEntity herobrine) || !(player.level() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        return Crucifix.attempt(level, player, herobrine, stack);
    }
}
