package dev.btstudio.herobrine.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class HerobrineRenderState extends HumanoidRenderState {
    public boolean hostile;
    public boolean elite;
    public boolean inferno;
    /** Per-entity offset, in ticks, so nearby Herobrines do not pulse in unison. */
    public int eyeGlowPhase;
}
