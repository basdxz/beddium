package me.jellysquid.mods.sodium.client.compatibility.checks;

import me.jellysquid.mods.sodium.client.gui.console.Console;
import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ResourcePackScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-InGameChecks");
    private static final List<String> VSH_FSH_BLACKLIST = Arrays.asList(
            "rendertype_solid.vsh", "rendertype_solid.fsh",
            "rendertype_cutout_mipped.vsh", "rendertype_cutout_mipped.fsh",
            "rendertype_cutout.vsh", "rendertype_cutout.fsh",
            "rendertype_translucent.vsh", "rendertype_translucent.fsh",
            "rendertype_tripwire.vsh", "rendertype_tripwire.fsh"
    );
    private static final List<String> GLSL_BLACKLIST = Arrays.asList(
            "light.glsl",
            "fog.glsl"
    );

    /**
     * <a href="https://github.com/CaffeineMC/sodium-fabric/issues/1569">#1569</a>
     * Iterate through all active resource packs, and detect resource packs which contain files matching the blacklist.
     * An error message is shown for resource packs which replace terrain core shaders.
     * A warning is shown for resource packs which replace the default light.glsl and fog.glsl shaders.
     * Detailed information on shader files replaced by resource packs is printed in the client log.
     */
    public static void checkIfCoreShaderLoaded(ResourceManager manager) {
        HashMap<String, MessageLevel> detectedResourcePacks = new HashMap<>();
        var customResourcePacks = manager.listPacks();

        customResourcePacks.forEach(resourcePack -> {
            // Omit 'vanilla', 'fabric', and 'mod_resources' resource packs
            if (!resourcePack.packId().equals("vanilla") && !resourcePack.packId().equals("fabric") && !resourcePack.packId().equals("mod_resources")) {
                var resourcePackName = resourcePack.packId();
                var ignoredShaders = determineIgnoredShaders(resourcePack);

                resourcePack.listResources(PackType.CLIENT_RESOURCES, ResourceLocation.DEFAULT_NAMESPACE, "shaders", (path, ignored) -> {
                    // Trim full shader file path to only contain the filename
                    var shaderName = path.getPath().substring(path.getPath().lastIndexOf('/') + 1);

                    // Check if the pack has already acknowledged the warnings in this file,
                    // in this case we report a different info log about the situation
                    if (ignoredShaders.contains(shaderName)) {
                        if (VSH_FSH_BLACKLIST.contains(shaderName)) {
                            LOGGER.info("Resource pack '{}' replaces core shader '{}' but indicates it can be ignored", resourcePackName, shaderName);
                        }

                        if (GLSL_BLACKLIST.contains(shaderName)) {
                            LOGGER.info("Resource pack '{}' replaces shader '{}' but indicates it can be ignored", resourcePackName, shaderName);
                        }
                        return;
                    }

                    if (VSH_FSH_BLACKLIST.contains(shaderName)) {

                        if (!detectedResourcePacks.containsKey(resourcePackName)) {
                            detectedResourcePacks.put(resourcePackName, MessageLevel.SEVERE);
                        } else if (detectedResourcePacks.get(resourcePackName) == MessageLevel.WARN) {
                            detectedResourcePacks.replace(resourcePackName, MessageLevel.SEVERE);
                        }

                        LOGGER.error("Resource pack '{}' replaces core shader '{}'", resourcePackName, shaderName);
                    }

                    if (GLSL_BLACKLIST.contains(shaderName)) {

                        if (!detectedResourcePacks.containsKey(resourcePackName)) {
                            detectedResourcePacks.put(resourcePackName, MessageLevel.WARN);
                        }

                        LOGGER.error("Resource pack '{}' replaces shader '{}'", resourcePackName, shaderName);

                    }
                });
            }
        });

        if (detectedResourcePacks.containsValue(MessageLevel.SEVERE)) {
            showConsoleMessage(Component.translatable("sodium.console.core_shaders_error"), MessageLevel.SEVERE);

            for (Map.Entry<String, MessageLevel> entry : detectedResourcePacks.entrySet()) {

                if (entry.getValue() == MessageLevel.SEVERE) {
                    // Omit 'file/' prefix for the in-game message
                    var message = entry.getKey().startsWith("file/") ? entry.getKey().substring(5) : entry.getKey();
                    showConsoleMessage(Component.literal(message), MessageLevel.SEVERE);
                }
            }
        }

        if (detectedResourcePacks.containsValue(MessageLevel.WARN)) {
            showConsoleMessage(Component.translatable("sodium.console.core_shaders_warn"), MessageLevel.WARN);

            for (Map.Entry<String, MessageLevel> entry : detectedResourcePacks.entrySet()) {

                if (entry.getValue() == MessageLevel.WARN) {
                    // Omit 'file/' prefix for the in-game message
                    var message = entry.getKey().startsWith("file/") ? entry.getKey().substring(5) : entry.getKey();
                    showConsoleMessage(Component.literal(message), MessageLevel.WARN);
                }
            }
        }

        if (!detectedResourcePacks.isEmpty()) {
            showConsoleMessage(Component.translatable("sodium.console.core_shaders_info"), MessageLevel.INFO);
        }
    }

    /**
     * Looks at a resource pack's metadata to find a list of shaders that can be gracefully
     * ignored. This offers resource packs the ability to acknowledge they are shipping shaders
     * which will not work with Sodium, but that Sodium can ignore.
     *
     * @param resourcePack The resource pack to fetch the ignored shaders of
     * @return A list of shaders to ignore, this is the filename only without the path
     */
    private static List<String> determineIgnoredShaders(PackResources resourcePack) {
        var ignoredShaders = new ArrayList<String>();
        try {
            var meta = resourcePack.getMetadataSection(SodiumResourcePackMetadata.SERIALIZER);
            if (meta != null) {
                ignoredShaders.addAll(meta.ignoredShaders());
            }
        } catch (IOException x) {
            LOGGER.error("Failed to load pack.mcmeta file for resource pack '{}'", resourcePack.packId());
        }
        return ignoredShaders;
    }

    private static void showConsoleMessage(MutableComponent message, MessageLevel messageLevel) {
        Console.instance().logMessage(messageLevel, message, 20.0);
    }

}
