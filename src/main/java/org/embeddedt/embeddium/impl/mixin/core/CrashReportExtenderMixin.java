package org.embeddedt.embeddium.impl.mixin.core;

import net.minecraftforge.logging.CrashReportExtender;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CrashReportExtender.class)
public class CrashReportExtenderMixin {

    // TODO: [VEN] Find where to append the crash report header
//    @Inject(method = "addCrashReportHeader", at = @At("HEAD"), remap = false)
//    private static void injectEmbeddiumTaintHeader(StringBuilder builder, CrashReport crashReport, CallbackInfo ci) {
//        try {
//            var mods = MixinTaintDetector.getTaintingMods();
//            if(!mods.isEmpty()) {
//                builder.append("// Embeddium instance tainted by mods: [").append(String.join(", ", mods)).append("]\n");
//                builder.append("// Please do not reach out for Embeddium support without removing these mods first.\n");
//                builder.append("// -------\n");
//            }
//        } catch(Throwable ignored) {
//            // fail-safe, we absolutely do not want to crash during crash report generation
//        }
//    }
}
