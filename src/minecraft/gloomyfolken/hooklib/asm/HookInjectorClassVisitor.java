package gloomyfolken.hooklib.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Iterator;
import java.util.List;

public class HookInjectorClassVisitor extends ClassVisitor {

    List<AsmHook> hooks;

    public HookInjectorClassVisitor(ClassWriter cv, List<AsmHook> hooks) {
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
            if (isTargetMethod(hook, name, desc)){
                mv = hook.getInjectorFactory().createHookInjector(mv, access, name, desc, hook);
                it.remove();
            }
        }
        return mv;
    }

    protected boolean isTargetMethod(AsmHook hook, String name, String desc) {
        return hook.isTargetMethod(name, desc);
    }
}
