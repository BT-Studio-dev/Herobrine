package dev.btstudio.herobrine.entity;

import dev.btstudio.herobrine.HerobrineMod;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    private static final ResourceKey<EntityType<?>> HEROBRINE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, HerobrineMod.id("herobrine"));

    /**
     * Created through {@link FabricEntityType.Builder} so the default attributes are registered
     * together with the type (no separate {@code FabricDefaultAttributeRegistry} call needed).
     */
    public static final EntityType<HerobrineEntity> HEROBRINE = FabricEntityType.Builder
            .createMob(HerobrineEntity::new, MobCategory.MONSTER,
                    builder -> builder.defaultAttributes(HerobrineEntity::createAttributes))
            .sized(0.6F, 1.95F)
            .eyeHeight(1.62F)
            .clientTrackingRange(10)
            .build(HEROBRINE_KEY);

    private static final ResourceKey<EntityType<?>> STEVE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, HerobrineMod.id("steve_friend"));

    /** The friendly companion. Not a monster, so no monster spawn rules apply to him. */
    public static final EntityType<SteveFriendEntity> STEVE_FRIEND = FabricEntityType.Builder
            .createMob(SteveFriendEntity::new, MobCategory.MISC,
                    builder -> builder.defaultAttributes(SteveFriendEntity::createAttributes))
            .sized(0.6F, 1.8F)
            .eyeHeight(1.62F)
            .clientTrackingRange(10)
            .build(STEVE_KEY);

    private ModEntities() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, HEROBRINE_KEY, HEROBRINE);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, STEVE_KEY, STEVE_FRIEND);
    }
}
