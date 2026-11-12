package com.herobrinemod.herobrine.component;

import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.dynamic.Codecs;

public class DataComponentTypeList {
    public static final ComponentType<Integer> KILLS = Registry.register(Registries.DATA_COMPONENT_TYPE, "kills", new ComponentType.Builder<Integer>().codec(Codecs.NONNEGATIVE_INT).packetCodec(PacketCodecs.VAR_INT).build());
}
