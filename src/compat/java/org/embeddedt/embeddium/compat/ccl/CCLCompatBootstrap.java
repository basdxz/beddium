package org.embeddedt.embeddium.compat.ccl;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.embeddedt.embeddium.impl.Embeddium;

@Mod.EventBusSubscriber(modid = Embeddium.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCLCompatBootstrap {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("codechickenlib")) {
            CCLCompat.onClientSetup(event);
        }
    }
}
