package gloomyfolken.hooklib.asm;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class HookClassTransformer implements IClassTransformer {

    public static HookClassTransformer instance;

    private static HashMap<String, List<AsmHook>> hooksMap = new HashMap<String, List<AsmHook>>();

    public HookClassTransformer(){
        instance = this;
    }

    public static void registerHook(AsmHook hook){
        if (hooksMap.containsKey(hook.getTargetClassName())){
            hooksMap.get(hook.getTargetClassName()).add(hook);
        } else {
            List<AsmHook> list = new ArrayList<AsmHook>(2);
            list.add(hook);
            hooksMap.put(hook.getTargetClassName(), list);
        }
    }

    @Override
    public byte[] transform(String name, String newName, byte[] bytecode) {
        List<AsmHook> hooks = hooksMap.get(newName);
        if (hooks != null){
            try {
                info("Injecting hooks into class " + newName);
                int numHooks = hooks.size();
                int majorVersion =  ((bytecode[6]&0xFF)<<8) | (bytecode[7]&0xFF);
                int minorVersion =  ((bytecode[4]&0xFF)<<8) | (bytecode[5]&0xFF);
                boolean java7 = majorVersion > 50;
                if (java7){
                    warning("Bytecode version of class " + newName + " is " + majorVersion + "." + minorVersion + ".");
                    warning("This is Java 1.7+, whereas Minecraft works on Java 1.6 (bytecode version 50).");
                    warning("Enabling COMPUTE_FRAMES, but it probably will crash minecraft on the server side.");
                    warning("If you are in an MCP environment, you should better set javac version to 1.6.");
                    warning("If you are not, something is going completly wrong.");
                }

                ClassReader cr = new ClassReader(bytecode);
                ClassWriter cw = new ClassWriter(java7 ? ClassWriter.COMPUTE_FRAMES : 0);
                HookInjectorClassWriter hooksWriter = new HookInjectorClassWriter(cw, hooks);
                cr.accept(hooksWriter, java7 ? ClassReader.SKIP_FRAMES : ClassReader.EXPAND_FRAMES);

                int numInjectedHooks = numHooks - hooksWriter.hooks.size();
                info("Successfully injected " + numInjectedHooks + " hook" + (numInjectedHooks == 1 ? "" : "s"));
                for (AsmHook notInjected : hooksWriter.hooks){
                    warning("Can not found target method of hook " + notInjected);
                }

                hooksMap.remove(newName);
                return cw.toByteArray();
            } catch (Exception e){
                severe("A problem has occured during transformation of class " + newName + ".");
                severe("No hook will be injected into this class.");
                severe("Attached hooks:");
                for (AsmHook hook : hooks) {
                    severe(hook.toString());
                }
                severe("Stack trace:");
                e.printStackTrace();
            }
        }
        return bytecode;
    }

    private static final String LOG_PREFIX = "[HOOKLIB] ";

    private static void severe(String msg){
        FMLRelaunchLog.severe(LOG_PREFIX + msg);
    }

    private static void info(String msg){
        FMLRelaunchLog.info(LOG_PREFIX + msg);
    }
    private static void finest(String msg){
        FMLRelaunchLog.finest(LOG_PREFIX + msg);
    }

    private void warning(String msg){
        FMLRelaunchLog.warning(LOG_PREFIX + msg);
    }

    private static class HookInjectorClassWriter extends ClassVisitor {

        List<AsmHook> hooks;

        public HookInjectorClassWriter(ClassWriter cv, List<AsmHook> hooks) {
            super(Opcodes.ASM4, cv);
            this.hooks = hooks;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            Iterator<AsmHook> it = hooks.iterator();
            while (it.hasNext()) {
                AsmHook hook = it.next();
                if (name.equals(hook.getTargetMethodName(HookLibPlugin.getObfuscated()))
                        && desc.equals(hook.getTargetMethodDescription())){
                    mv = hook.getInjectorFactory().createHookInjector(mv, access, name, desc, hook);
                    it.remove();
                }
            }
            return mv;
        }
    }

}
