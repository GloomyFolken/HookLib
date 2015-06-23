package gloomyfolken.hooklib.asm;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

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
        if (name.equals("net.minecraft.client.Minecraft")) {
            /*ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytecode);
            classReader.accept(classNode, 0);

            for (int i = 0; i < classNode.methods.size(); i++){
                MethodNode method = classNode.methods.get(i);
                if (method.name.equals("clickMiddleMouseButton")){
                    Iterator<AbstractInsnNode> it = method.instructions.iterator();
                    while (it.hasNext()){
                        printNode(it.next());
                    }
                }
            }

            ClassWriter writer = new ClassWriter(0);
            classNode.accept(writer);
            return writer.toByteArray();*/
        }
        if (hooks != null){
            try {

                finest("Injecting hooks into class " + newName);
                int numHooks = hooks.size();
                int majorVersion =  ((bytecode[6]&0xFF)<<8) | (bytecode[7]&0xFF);
                int minorVersion =  ((bytecode[4]&0xFF)<<8) | (bytecode[5]&0xFF);
                if (majorVersion > 50){
                    warning("Bytecode version of class " + newName + " is " + majorVersion + "." + minorVersion + ".");
                    warning("This is Java 1.7+, whereas Minecraft works on Java 1.6 (bytecode version 50).");
                    warning("Enabling COMPUTE_FRAMES, but it probably will crash minecraft on the server side.");
                    warning("If you are in an MCP environment, you should better set javac version to 1.6.");
                    warning("If you are not, something is going completly wrong.");
                }

                ClassReader cr = new ClassReader(bytecode);
                ClassWriter cw = new ClassWriter(majorVersion > 50 ? ClassWriter.COMPUTE_FRAMES : 0);

                HookInjectorClassWriter hooksWriter = new HookInjectorClassWriter(cw, hooks);
                cr.accept(hooksWriter, majorVersion > 50 ? ClassReader.SKIP_FRAMES : ClassReader.EXPAND_FRAMES);

                int numInjectedHooks = numHooks - hooksWriter.hooks.size();
                finest("Successfully injected " + numInjectedHooks + " hook" + (numInjectedHooks == 1 ? "" : "s"));
                for (AsmHook notInjected : hooksWriter.hooks){
                    warning("Can not found target method of hook " + notInjected);
                }

                hooksMap.remove(newName);

                if (newName.equals("net.minecraft.entity.EntityLivingBase")){
                    ClassNode classNode = new ClassNode();
                    ClassReader classReader = new ClassReader(cw.toByteArray());
                    classReader.accept(classNode, 0);
                    for (MethodNode method : classNode.methods){
                        if (method.name.equals("getTotalArmorValue")){
                            Iterator<AbstractInsnNode> it = method.instructions.iterator();
                            while (it.hasNext()){
                                printNode(it.next());
                            }
                        }
                    }
                }

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

    private void severe(String msg){
        FMLRelaunchLog.severe(LOG_PREFIX + msg);
    }

    private void finest(String msg){
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
        System.out.println("class=" + node.getClass().getCanonicalName() + ", type=" + node.getType() + ", opcode=" + node.getOpcode());
    }

    /**
     * Кривой и запутанный, но полезный метод. Позволяет встроить в начало или
     * конец любого метода обращение к любому статическому методу.
     * Работает только при компиляторе 1.6
     *
     * @param classNode
     *            - в котором находится метод
     * @param methodName
     *            - название метода
     * @param methodDescription
     *            - описание метода
     * @param callMethodOwner
     *            - класс, в котором содержатся нужный статический метод
     * @param callMethodName
     *            - название нужного статического метода
     * @param callMethodDescription
     *            - описание нужного статического метода
     * @param after
     *            - нужно ли вставлять обращение в конце метода (если стоит
     *            true, то вызов идет последней командой. Иначе - в начале)
     * @param returnIfTrue
     *            - возвращать ли что-либо, если указанный метод вернул true
     * @param returnObject
     *            - Объект, который нужно возвращать. Константа
     * @param argIds
     *            - номера аргументов, которые нужно передать статическому
     *            методу. Как правило, 0 - объект this, от 1 и дальше -
     *            аргументы к исполняемому методу. Однако, исключений дофига, и
     *            лучше номера аргументов проверять, предварительно добавив в
     *            метод нужный вызов и распечатав его при помощи printNode.
     * @param argOpcodes
     *            - типы аргументов. int,short,boolean,byte: Opcodes.ILOAD (21)
     *            long: Opcodes.LLOAD (22) float: Opcodes.FLOAD (23) double:
     *            Opcodes.DLOAD (24) другое: Opcodes.ALOAD (25)
     * @return получилось ли вставить вызов метода
     */

    private boolean insertHook(ClassNode classNode, String methodName, String methodDescription, String callMethodOwner, String callMethodName,
                               String callMethodDescription, boolean after, boolean returnIfTrue, Object returnObject, int[] argIds, int[] argOpcodes) {
        Iterator<MethodNode> methods = classNode.methods.iterator();
        while (methods.hasNext()) {
            MethodNode m = methods.next();
            if (m.name.equals(methodName) && m.desc.equals(methodDescription)) {
                InsnList toInject = new InsnList();

                for (int i = 0; i < argIds.length; i++) {
                    toInject.add(new VarInsnNode(argOpcodes[i], argIds[i]));
                }
                toInject.add(new MethodInsnNode(184, callMethodOwner, callMethodName, callMethodDescription));
                if (returnIfTrue) {
                    LabelNode label = new LabelNode();
                    toInject.add(new JumpInsnNode(153, label));
                    if (methodDescription.endsWith("V")) {
                        toInject.add(new InsnNode(Opcodes.RETURN));
                    } else if (methodDescription.endsWith("I") || methodDescription.endsWith("Z") || methodDescription.endsWith("B")
                            || methodDescription.endsWith("S")) {
                        toInject.add(new LdcInsnNode(returnObject));
                        toInject.add(new InsnNode(Opcodes.IRETURN));
                    } else if (methodDescription.endsWith("L")) {
                        toInject.add(new LdcInsnNode(returnObject));
                        toInject.add(new InsnNode(Opcodes.LRETURN));
                    } else if (methodDescription.endsWith("F")) {
                        toInject.add(new LdcInsnNode(returnObject));
                        toInject.add(new InsnNode(Opcodes.FRETURN));
                    } else if (methodDescription.endsWith("D")) {
                        toInject.add(new LdcInsnNode(returnObject));
                        toInject.add(new InsnNode(Opcodes.DRETURN));
                    } else if (returnObject == null) {
                        toInject.add(new InsnNode(1));
                        toInject.add(new InsnNode(Opcodes.ARETURN));
                    } else {
                        System.out.println("[ASM] Can't return a special object!");
                        return false;
                    }
                    toInject.add(label);
                }

                AbstractInsnNode targetNode = null;
                Iterator<AbstractInsnNode> it = m.instructions.iterator();
                int nodeId = 0;
                while (it.hasNext()) {
                    AbstractInsnNode insn = it.next();
                    if ((!after && nodeId == 0)
                            || (after && insn.getNext() != null && insn.getNext().getType() == 0 && insn.getNext().getOpcode() == Opcodes.RETURN
                            && (insn.getNext().getNext() == null) || (insn.getNext().getNext().getNext() == null))) {
                        targetNode = insn;
                        break;
                    }
                    nodeId++;
                }
                if (targetNode != null) {
                    m.instructions.insert(targetNode, toInject);
                    return true;
                } else {
                    System.out.println("[ASM] Can't find insert node for calling " + callMethodName + " from " + classNode.name + "." + methodName);
                    return false;
                }
            }
        }
        System.out.println("[ASM] Can't find insert method for calling " + callMethodName + " from " + classNode.name + "." + methodName);
        return false;
    }

}
