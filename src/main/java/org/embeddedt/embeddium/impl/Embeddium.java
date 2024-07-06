package org.embeddedt.embeddium.impl;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.impl.data.fingerprint.FingerprintMeasure;
import org.embeddedt.embeddium.impl.data.fingerprint.HashedFingerprint;
import org.embeddedt.embeddium.impl.gui.EmbeddiumOptions;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.sodium.FlawlessFrames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.io.IOException;

@Mod(Embeddium.MODID)
public class Embeddium {
    public static final String MODID = EmbeddiumConstants.MODID;
    public static final String MODNAME = EmbeddiumConstants.MODNAME;

    private static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);
    private static EmbeddiumOptions CONFIG = loadConfig();

    private static String MOD_VERSION;

    public Embeddium() {
        MOD_VERSION = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString();

        try {
            updateFingerprint();
        } catch (Throwable t) {
            LOGGER.error("Failed to update fingerprint", t);
        }

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    }

    public void onClientSetup(final FMLClientSetupEvent event) {
        FlawlessFrames.onClientInitialization();
    }

    public static EmbeddiumOptions options() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }

        return CONFIG;
    }

    public static Logger logger() {
        if (LOGGER == null) {
            throw new IllegalStateException("Logger not yet available");
        }

        return LOGGER;
    }

    private static EmbeddiumOptions loadConfig() {
        try {
            return EmbeddiumOptions.load();
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration file", e);
            LOGGER.error("Using default configuration file in read-only mode");

            var config = new EmbeddiumOptions();
            config.setReadOnly();

            return config;
        }
    }

    public static void restoreDefaultOptions() {
        CONFIG = EmbeddiumOptions.defaults();

        try {
            EmbeddiumOptions.writeToDisk(CONFIG);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config file", e);
        }
    }

    public static String getVersion() {
        if (MOD_VERSION == null) {
            throw new NullPointerException("Mod version hasn't been populated yet");
        }

        return MOD_VERSION;
    }

    private static void updateFingerprint() {
        var current = FingerprintMeasure.create();

        if (current == null) {
            return;
        }

        HashedFingerprint saved = null;

        try {
            saved = HashedFingerprint.loadFromDisk();
        } catch (Throwable t) {
            LOGGER.error("Failed to load existing fingerprint",  t);
        }

        if (saved == null || !current.looselyMatches(saved)) {
            HashedFingerprint.writeToDisk(current.hashed());

            CONFIG.notifications.hasSeenDonationPrompt = false;
            CONFIG.notifications.hasClearedDonationButton = false;

            try {
                EmbeddiumOptions.writeToDisk(CONFIG);
            } catch (IOException e) {
                LOGGER.error("Failed to update config file", e);
            }
        }
    }

    public static boolean canUseVanillaVertices() {
        return !Embeddium.options().performance.useCompactVertexFormat && !ShaderModBridge.areShadersEnabled();
    }

    public static boolean canApplyTranslucencySorting() {
        return Embeddium.options().performance.useTranslucentFaceSorting && !ShaderModBridge.isNvidiumEnabled();
    }
}