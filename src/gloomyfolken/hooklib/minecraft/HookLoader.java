package gloomyfolken.hooklib.minecraft;

import cpw.mods.fml.common.asm.transformers.DeobfuscationTransformer;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import gloomyfolken.hooklib.asm.AsmHook;
import gloomyfolken.hooklib.asm.HookClassTransformer;
import gloomyfolken.hooklib.asm.ReadClassHelper;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Удобная базовая реализация IFMLLoadingPlugin для использования HookLib.
 * Регистрировать хуки и контейнеры нужно в registerHooks().
 */
public abstract class HookLoader implements IFMLLoadingPlugin {

    private static DeobfuscationTransformer deobfuscationTransformer;

    static {
        if (HookLibPlugin.getObfuscated()) {
            deobfuscationTransformer = new DeobfuscationTransformer();
        }
    }

    private static HookClassTransformer getTransformer() {
        return PrimaryClassTransformer.instance.registeredSecondTransformer ?
                MinecraftClassTransformer.instance : PrimaryClassTransformer.instance;
    }

    /**
     * Регистрирует вручную созданный хук
     */
    public static void registerHook(AsmHook hook) {
        getTransformer().registerHook(hook);
    }

    /**
     * Деобфусцирует класс с хуками и регистрирует хуки из него
     */
    public static void registerHookContainer(String className) {
        try {
            InputStream classData = ReadClassHelper.getClassData(className);
            byte[] bytes = IOUtils.toByteArray(classData);
            classData.close();
            if (deobfuscationTransformer != null) {
                bytes = deobfuscationTransformer.transform(className, className, bytes);
            }
            ByteArrayInputStream newData = new ByteArrayInputStream(bytes);
            getTransformer().registerHookContainer(newData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 1.6.x only
    public String[] getLibraryRequestClass() {
        return null;
    }

    // 1.7.x only
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        registerHooks();
    }

    protected abstract void registerHooks();
}
