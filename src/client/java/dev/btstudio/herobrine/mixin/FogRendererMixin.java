package dev.btstudio.herobrine.mixin;

import dev.btstudio.herobrine.client.HerobrineFog;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The fog of <i>From The Fog</i>: the closer he is, the closer the fog gets.
 *
 * <p>26.3 builds the fog in {@code FogRenderer#setupFog}, which returns a plain {@link FogData}. This
 * only pulls the environmental fog distances in - nothing else in the renderer is touched, and with
 * no Herobrine nearby the value is left exactly as vanilla produced it.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
    /** Fog starts this far away when he is standing next to you... */
    private static final float NEAR_SPAN = 6.0F;
    /** ...and this far away at the edge of the 48 block range. */
    private static final float FAR_SPAN = 76.0F;

    @Inject(method = "setupFog", at = @At("RETURN"), cancellable = true)
    private void herobrine$closingFog(Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker,
                                      float darkenWorldAmount, ClientLevel level,
                                      CallbackInfoReturnable<FogData> cir) {
        float closeness = HerobrineFog.closeness(camera.entity());

        if (closeness <= 0.0F) {
            return;
        }

        FogData fog = cir.getReturnValue();
        float near = NEAR_SPAN + (1.0F - closeness) * (FAR_SPAN - NEAR_SPAN);
        float far = near + 8.0F + (1.0F - closeness) * 64.0F;

        fog.environmentalStart = Math.min(fog.environmentalStart, near);
        fog.environmentalEnd = Math.min(fog.environmentalEnd, far);
        HerobrineFog.noteApplied();
        cir.setReturnValue(fog);
    }
}
