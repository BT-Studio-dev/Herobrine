package com.herobrinemod.herobrine.mixin;

import com.herobrinemod.herobrine.savedata.SaveDataHandler;
import net.minecraft.util.crash.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrashReport.class)
public class CrashReportMixin {
    @Inject(method = "create", at = @At("HEAD"))
    private static void saveDataOnCrash(Throwable cause, String title, CallbackInfoReturnable<CrashReport> cir) {
        if(SaveDataHandler.getHerobrineSaveData() != null) {
            if(SaveDataHandler.getHerobrineSaveData().isDirty()) {
                SaveDataHandler.getHerobrineSaveData().saveFile();
            }
        }
    }
}
