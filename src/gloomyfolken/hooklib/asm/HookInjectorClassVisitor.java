package gloomyfolken.hooklib.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HookInjectorClassVisitor extends ClassVisitor {

    List<AsmHook> hooks;
    List<AsmHook> notInjectedHooks = new ArrayList<>(0);
    boolean visitingHook;
    HookClassTransformer transformer;

    String superName;

    public HookInjectorClassVisitor(HookClassTransformer transformer, ClassWriter cv, List<AsmHook> hooks) {
        super(Opcodes.ASM5, cv);
        this.hooks = hooks;
        this.transformer = transformer;
    }

    @Override public void visit(int version, int access, String name,
                                String signature, String superName, String[] interfaces) {
        this.superName = superName;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        Iterator<AsmHook> it = hooks.iterator();
        while (it.hasNext()) {
            AsmHook hook = it.next();
            if (isTargetMethod(hook, name, desc)) {
                // добавляет MethodVisitor в цепочку
                mv = hook.getInjectorFactory().createHookInjector(mv, access, name, desc, hook, this);
                it.remove();
            }
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        while (!hooks.isEmpty()) {
            AsmHook hook = hooks.get(0);
            if (hook.getCreateMethod()) {
                hook.createMethod(this);
            }

            if (hooks.contains(hook)) {
                // Если был вызван hook.createMethod, то по идее хук должен удалиться в createMethod,
                // так как оттуда будет вызван visitMethod, но если используется кривой инжектор или
                // еще что-то пошло не так, то тоже удалить его руками
                notInjectedHooks.add(hook);
                hooks.remove(0);
            }
        }
        super.visitEnd();
    }

    protected boolean isTargetMethod(AsmHook hook, String name, String desc) {
        return hook.isTargetMethod(name, desc);
    }
}
