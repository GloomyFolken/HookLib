package gloomyfolken.hooklib.minecraft;

import cpw.mods.fml.common.Loader;
import gloomyfolken.hooklib.asm.Hook;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class SecondaryTransformerHook {

    /**
     * Регистрирует хук-трансформер последним.
     */
    @Hook
    public static void injectData(Loader loader, Object... data) {
        LaunchClassLoader classLoader = (LaunchClassLoader) SecondaryTransformerHook.class.getClassLoader();
        classLoader.registerTransformer(MinecraftClassTransformer.class.getName());
    }

}
