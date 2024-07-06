package org.embeddedt.embeddium.impl;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import org.embeddedt.embeddium.impl.compatibility.checks.EarlyDriverScanner;
import org.embeddedt.embeddium.impl.compatibility.environment.probe.GraphicsAdapterProbe;
import org.embeddedt.embeddium.impl.compatibility.workarounds.Workarounds;

public class EmbeddiumPreLaunch {
    public static void onPreLaunch() {
        if(FMLLoader.getDist() == Dist.CLIENT) {
            GraphicsAdapterProbe.findAdapters();
            EarlyDriverScanner.scanDrivers();
            Workarounds.init();
        }
    }
}