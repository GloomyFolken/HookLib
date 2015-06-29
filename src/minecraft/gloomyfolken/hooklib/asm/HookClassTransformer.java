package gloomyfolken.hooklib.asm;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class HookClassTransformer implements IClassTransformer {

    public static HookClassTransformer instance;

    private HashMap<String, List<AsmHook>> hooksMap = new HashMap<String, List<AsmHook>>();

    public HookClassTransformer(){
        instance = this;
    }

    public void registerHook(AsmHook hook){
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
    private void printNode(AbstractInsnNode node) {
        if (node.getType() == 5) {
            MethodInsnNode mnode = (MethodInsnNode) node;
            System.out.println("MethodInsnNode: opcode=" + mnode.getOpcode() + ", owner=" + mnode.owner + ", name=" + mnode.name + ", desc="
                    + mnode.desc);
        } else if (node.getType() == 7) {
            JumpInsnNode jnode = (JumpInsnNode) node;
            System.out.println("JumpInsnNode: opcode=" + jnode.getOpcode() + ", label=" + jnode.label.getLabel());
        } else if (node.getType() == 0) {
            InsnNode inode = (InsnNode) node;
            System.out.println("InsnNode: opcode=" + inode.getOpcode());
        } else if (node.getType() == 8) {
            LabelNode lnode = (LabelNode) node;
            System.out.println("LabelNode: opcode= " + lnode.getOpcode() + ", label=" + lnode.getLabel().toString());
        } else if (node.getType() == 15) {
            System.out.println("LineNumberNode, opcode=" + node.getOpcode());
        } else if (node instanceof FrameNode) {
            FrameNode fnode = (FrameNode) node;
            String out = "FrameNode: opcode=" + fnode.getOpcode() + ", type=" + fnode.type + ", nLocal="
                    + (fnode.local == null ? -1 : fnode.local.size()) + ", local=";
            if (fnode.local != null) {
                for (Object obj : fnode.local) {
                    out += obj == null ? "null" : obj.toString() + ";";
                }
            } else {
                out += null;
            }
            out += ", nstack=" + (fnode.stack == null ? -1 : fnode.stack.size()) + ", stack=";
            if (fnode.stack != null) {
                for (Object obj : fnode.stack) {
                    out += obj == null ? "null" : obj.toString() + ";";
                }
            } else {
                out += null;
            }
            System.out.println(out);
        } else if (node.getType() == 2) {
            VarInsnNode vnode = (VarInsnNode) node;
            System.out.println("VarInsnNode: opcode=" + vnode.getOpcode() + ", var=" + vnode.var);
        } else if (node.getType() == 9) {
            LdcInsnNode lnode = (LdcInsnNode) node;
            System.out.println("LdcInsnNode: opcode=" + lnode.getOpcode() + ", cst=" + lnode.cst);
        } else if (node.getType() == 4) {
            FieldInsnNode fnode = (FieldInsnNode) node;
            System.out.println("FieldInsnNode: opcode=" + fnode.getOpcode() + ", owner=" + fnode.owner + ", name=" + fnode.name + ", desc="
                    + fnode.desc);
        } else if (node.getType() == 3) {
            TypeInsnNode tnode = (TypeInsnNode) node;
            System.out.println("TypeInsnNode: opcode=" + tnode.getOpcode() + ", desc=" + tnode.desc);
        } else {
            printUnexpectedNode(node);
        }
    }

    private void printUnexpectedNode(AbstractInsnNode node) {
        Logger.getLogger(LOG_PREFIX).log(Level.FINE, "class=" + node.getClass().getCanonicalName() + ", type=" + node.getType() + ", opcode=" + node.getOpcode());
    }

    private static final String LOG_PREFIX = "[HOOKLIB] ";

    private void severe(String msg){
        FMLRelaunchLog.severe(LOG_PREFIX + msg);
    }

    private void info(String msg){
        FMLRelaunchLog.info(LOG_PREFIX + msg);
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
                if (name.equals(hook.getTargetMethodName(HookLibPlugin.obf))
                        && desc.equals(hook.getTargetMethodDescription())){
                    mv = hook.getInjectorFactory().createHookInjector(mv, access, name, desc, hook);
                    it.remove();
                }
            }
            return mv;
        }
    }

}
