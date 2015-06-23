package gloomyfolken.hooklib.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.InsnNode;

/**
 * Класс, непосредственно вставляющий хук в метод.
 * Чтобы указать конкретное место вставки хука, нужно создать класс extends HookInjector.
 */
public abstract class HookInjector extends AdviceAdapter {

    protected final AsmHook hook;

    protected HookInjector(MethodVisitor mv, int access, String name, String desc, AsmHook hook) {
        super(Opcodes.ASM4, mv, access, name, desc);
        this.hook = hook;
    }

    /**
     * Вставляет хук в байткод.
     */
    protected final void visitHook(){
        hook.inject(this);
    }

    MethodVisitor getBasicVisitor(){
        return mv;
    }

    /**
     * Вставляет хук в начале метода.
     */
    public static class MethodEnter extends HookInjector {

        public MethodEnter(MethodVisitor mv, int access, String name, String desc, AsmHook hook) {
            super(mv, access, name, desc, hook);
        }

        @Override
        protected void onMethodEnter() {
            visitHook();
        }

    }

    /**
     * Вставляет хук на каждом выходе из метода, кроме выходов через throw.
     */
    public static class MethodExit extends HookInjector {

        public MethodExit(MethodVisitor mv, int access, String name, String desc, AsmHook hook) {
            super(mv, access, name, desc, hook);
        }

        private boolean visitingHook;

        @Override
        protected void onMethodExit(int opcode) {
            if(opcode != Opcodes.ATHROW && !visitingHook) {
                visitingHook = true;
                visitHook();
                visitingHook = false;
            }
        }

    }

}
