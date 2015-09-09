package gloomyfolken.hooklib.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Класс, непосредственно вставляющий хук в метод.
 * Чтобы указать конкретное место вставки хука, нужно создать класс extends HookInjector.
 */
public abstract class HookInjector extends AdviceAdapter {

    protected final AsmHook hook;
    public final Type methodType;
    public final boolean isStatic;
    protected static boolean visitingHook;

    protected HookInjector(MethodVisitor mv, int access, String name, String desc, AsmHook hook) {
        super(Opcodes.ASM4, mv, access, name, desc);
        this.hook = hook;
        isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.methodType = Type.getMethodType(desc);
    }

    /**
     * Вставляет хук в байткод.
     */
    protected final void visitHook() {
        if (!visitingHook) {
            visitingHook = true;
            hook.inject(this);
            visitingHook = false;
        }
    }

    MethodVisitor getBasicVisitor() {
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

        @Override
        protected void onMethodExit(int opcode) {
            if (opcode != Opcodes.ATHROW) {
                visitHook();
            }
        }
    }

    /**
     * Вставляет хук по номеру строки.
     */
    public static class LineNumber extends HookInjector {

        private int lineNumber;

        public LineNumber(MethodVisitor mv, int access, String name, String desc, AsmHook hook, int lineNumber) {
            super(mv, access, name, desc, hook);
            this.lineNumber = lineNumber;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            super.visitLineNumber(line, start);
            if (this.lineNumber == line) visitHook();
        }

    }

}
