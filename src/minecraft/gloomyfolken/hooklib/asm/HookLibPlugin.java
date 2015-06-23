package gloomyfolken.hooklib.asm;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

public class HookLibPlugin implements IFMLLoadingPlugin {

    static boolean obf;

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
        return new String[]{HookClassTransformer.class.getName()};
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
        obf = ((Boolean)data.get("runtimeDeobfuscationEnabled"));
    }
}
