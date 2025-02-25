package org.embeddedt.embeddium_integrity;

import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.Restriction;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class detects mods that apply mixins to Embeddium internals, and applies various levels of enforcement,
 * depending on configuration (see {@link EnforceLevel}).
 * <p>
 * Any attempts by mods to disable this detection will result in increasingly strengthened measures, up to and including
 * an explicit incompatible dependency on that mod being added. Please use supported means to interact with Embeddium, and/or
 * contribute your own. That benefits the whole community, while writing a bespoke mixin only benefits you.
 */
public class MixinTaintDetector implements IExtension {
    /**
     * The list of class packages that are considered "internal".
     */
    private static final List<String> TARGET_PREFIXES = List.of("org.embeddedt.embeddium");
    /**
     * A method handle used to read the package-private {@link ClassInfo#mixins} field. We need this to find out
     * about mixins before they are applied.
     */
    private static final MethodHandle GET_MIXINS_ON_CLASS_INFO;
    private static final Logger LOGGER = LoggerFactory.getLogger("Embeddium-MixinTaintDetector");
    private static final EnforceLevel DEFAULT_ENFORCE_LEVEL = EnforceLevel.WARN;

    /**
     * The enforcement level of taint detection.
     */
    public static final EnforceLevel ENFORCE_LEVEL;

    /**
     * Mods which are not subject to the new taint requirements.
     */
    private static final Collection<String> MOD_ID_WHITELIST = Set.of(
            "beddium", // obviously
            "flywheel" // mixin removed in next release, and only targets a class we are currently forced to protect anyway
    );

    /**
     * Mods which have injected into Embeddium with a mixin.
     */
    private static final Set<String> TAINTING_MODS = ConcurrentHashMap.newKeySet();

    public enum EnforceLevel {
        /**
         * Allow and do not report on any mixins being applied to Embeddium internals.
         */
        IGNORE,
        /**
         * Print a warning in the log when a mod applies mixins to Embeddium internals.
         */
        WARN,
        /**
         * Like {@link EnforceLevel#WARN}, but also throws an exception, which will usually result in the game crashing.
         * This is the strictest level of enforcement.
         */
        CRASH
    }

    static {
        MethodHandle mh = null;
        try {
            Field m = ClassInfo.class.getDeclaredField("mixins");
            m.setAccessible(true);
            mh = MethodHandles.publicLookup().unreflectGetter(m).asType(MethodType.methodType(Set.class, ClassInfo.class));
        } catch(ReflectiveOperationException | RuntimeException e) {
            e.printStackTrace();
        }
        GET_MIXINS_ON_CLASS_INFO = mh;
        EnforceLevel propertyLevel = EnforceLevel.valueOf(System.getProperty("embeddium.mixinTaintEnforceLevel", DEFAULT_ENFORCE_LEVEL.name()));
        if(propertyLevel.ordinal() < DEFAULT_ENFORCE_LEVEL.ordinal()) {
            propertyLevel = DEFAULT_ENFORCE_LEVEL;
        }
        ENFORCE_LEVEL = propertyLevel;
    }

    public static Collection<String> getTaintingMods() {
        return Collections.unmodifiableCollection(TAINTING_MODS);
    }

    /**
     * Bootstrap the taint detector. This should be called from a mixin plugin before any mixins are applied.
     */
    public static void initialize() {
        if(MixinEnvironment.getDefaultEnvironment().getActiveTransformer() instanceof IMixinTransformer transformer) {
            if(transformer.getExtensions() instanceof Extensions internalExtensions) {
                var instance = new MixinTaintDetector();
                try {
                    Field extensionsField = internalExtensions.getClass().getDeclaredField("extensions");
                    extensionsField.setAccessible(true);
                    ((List<IExtension>)extensionsField.get(internalExtensions)).add(instance);
                    Field activeExtensionsField = internalExtensions.getClass().getDeclaredField("activeExtensions");
                    activeExtensionsField.setAccessible(true);
                    List<IExtension> newActiveExtensions = new ArrayList<>((List<IExtension>)activeExtensionsField.get(internalExtensions));
                    newActiveExtensions.add(instance);
                    activeExtensionsField.set(internalExtensions, Collections.unmodifiableList(newActiveExtensions));
                } catch(ReflectiveOperationException | RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean checkActive(MixinEnvironment environment) {
        return true;
    }

    /**
     * {@return true if a class is an Embeddium internal class that mixins should not be allowed on}
     */
    private static boolean isEmbeddiumClass(String className) {
        for(String prefix : TARGET_PREFIXES) {
            if(className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static Set<IMixinInfo> getPotentialMixinsForClass(ClassInfo info) {
        if(GET_MIXINS_ON_CLASS_INFO != null) {
            try {
                return (Set<IMixinInfo>)GET_MIXINS_ON_CLASS_INFO.invokeExact(info);
            } catch(Throwable e) {
                LOGGER.error("Error encountered getting mixins for class info", e);
            }
        }
        return Collections.emptySet();
    }

    private static final Map<String, String> packagePrefixToModCache = new ConcurrentHashMap<>();

    /**
     * {@return the mod ID corresponding to a package name}
     */
    private static String getModIdByPackage(String pkg) {
        String id = packagePrefixToModCache.get(pkg);
        if(id != null) {
            return id;
        }

        id = "[unknown]";
        // Actually compute the ID
        String[] components = pkg.split("\\.");
        for(ModFileInfo mfi : LoadingModList.get().getModFiles()) {
            if(mfi.getMods().isEmpty()) {
                continue;
            }
            Path path = mfi.getFile().findResource(components);
            if(path != null && Files.exists(path)) {
                id = mfi.getMods().get(0).getModId();
                break;
            }
        }

        packagePrefixToModCache.put(pkg, id);
        return id;
    }

    /**
     * {@return true if the given mod version dependency allows at most one version}
     */
    private static boolean isDepSingleVersion(IModInfo.ModVersion version) {
        var restrictions = version.getVersionRange().getRestrictions();
        if(restrictions.isEmpty()) {
            return false;
        }
        for(Restriction restriction : restrictions) {
            if(restriction.getLowerBound() == null || restriction.getUpperBound() == null || !restriction.getLowerBound().equals(restriction.getUpperBound())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Filters the list of mixins targeting a class and returns ones from mods with ineligible dependency restrictions.
     * @param mixins the full list of mixins
     * @return a map pointing from ineligible mod ID to any mixins from that mod for the class
     */
    private static Map<String, List<IMixinInfo>> filterInvalidMixins(Collection<IMixinInfo> mixins) {
        Map<String, List<IMixinInfo>> map = new HashMap<>();
        for(IMixinInfo mixin : mixins) {
            var pkg = mixin.getConfig().getMixinPackage();
            String modId = getModIdByPackage(pkg);
            if(MOD_ID_WHITELIST.contains(modId)) {
                continue;
            }
            var file = LoadingModList.get().getModFileById(modId);
            if(file == null) {
                continue;
            }
            var deps = file.getMods().get(0).getDependencies();
            var embeddiumDeps = deps.stream().filter(dep -> dep.getModId().equals("beddium")).toList();
            // No dep or no single-version dep are both not allowed
            if(embeddiumDeps.isEmpty() || embeddiumDeps.stream().anyMatch(d -> !isDepSingleVersion(d))) {
                map.computeIfAbsent(modId, k -> new ArrayList<>()).add(mixin);
            }
        }
        return map;
    }

    @Override
    public void preApply(ITargetClassContext context) {
        if (ENFORCE_LEVEL == EnforceLevel.IGNORE) {
            return;
        }
        var classInfo = context.getClassInfo();
        var name = classInfo.getClassName();
        if(isEmbeddiumClass(name)) {
            var mixins = getPotentialMixinsForClass(classInfo);
            if(!mixins.isEmpty()) {
                var illegalMixinMap = filterInvalidMixins(mixins);
                if(!illegalMixinMap.isEmpty()) {
                    if(TAINTING_MODS.isEmpty()) {
                        LOGGER.error("Mod mixin into Embeddium internals detected. This instance is now tainted. The Embeddium team does not provide any guarantee of support for issues encountered while such mods are installed.");
                    }
                    TAINTING_MODS.addAll(illegalMixinMap.keySet());
                    var mixinList = "[" + String.join(", ", illegalMixinMap.keySet()) + "]";
                    LOGGER.warn("Mod(s) {} are modifying Embeddium class {}, which may cause instability.", mixinList, name);
                    if(ENFORCE_LEVEL == EnforceLevel.CRASH) {
                        throw new IllegalStateException("Mods " + mixinList + " are mixing into internal Embeddium class " + name + ". This has potential to destabilize the game, and the taint detector is currently configured to crash.");
                    }
                }
            }
        }
    }

    @Override
    public void postApply(ITargetClassContext context) {

    }

    @Override
    public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {

    }
}
