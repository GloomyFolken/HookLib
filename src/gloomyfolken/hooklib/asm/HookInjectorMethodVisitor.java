package gloomyfolken.hooklib.asm;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import static gloomyfolken.hooklib.asm.InjectionPoint.HEAD;
import static gloomyfolken.hooklib.asm.InjectionPoint.METHOD_CALL;

/**
 * Класс, непосредственно вставляющий хук в метод.
 * Чтобы указать конкретное место вставки хука, нужно создать класс extends HookInjector.
 */
public abstract class HookInjectorMethodVisitor extends AdviceAdapter {

    protected final AsmHook hook;
    protected final HookInjectorClassVisitor cv;
    public final String methodName;
    public final Type methodType;
    public final boolean isStatic;

    protected HookInjectorMethodVisitor(MethodVisitor mv, int access, String name, String desc,
                                        AsmHook hook, HookInjectorClassVisitor cv) {
        super(Opcodes.ASM5, mv, access, name, desc);
        this.hook = hook;
        this.cv = cv;
        isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.methodName = name;
        this.methodType = Type.getMethodType(desc);
    }

    /**
     * Вставляет хук в байткод.
     */
    protected final void visitHook() {
        if (!cv.visitingHook) {
            cv.visitingHook = true;
            hook.inject(this);
            cv.visitingHook = false;
        }
    }

    MethodVisitor getBasicVisitor() {
        return mv;
    }

    /**
     * Вставляет хук в произвольном методе
     */

    public static class ByAnchor extends HookInjectorMethodVisitor {

        private Integer ordinal;

        public ByAnchor(MethodVisitor mv, int access, String name, String desc, AsmHook hook, HookInjectorClassVisitor cv) {
            super(mv, access, name, desc, hook, cv);

            ordinal =hook.getAnchorOrdinal();
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if(hook.getAnchorPoint()==METHOD_CALL && hook.getAnchorTarget().equals(name))
                visitOrderedHook();

        }

        protected void onMethodEnter() {
            if(hook.getAnchorPoint()==HEAD)
                visitHook();
        }
        protected void onMethodExit(int opcode) {
            if(hook.getAnchorPoint()==InjectionPoint.RETURN && opcode != Opcodes.ATHROW)
                visitOrderedHook();

        }

        private void visitOrderedHook() {
            if (ordinal == 0) {
                visitHook();
                ordinal = -2;
            } else if(ordinal == -1) {
                visitHook();
            } else if(ordinal >0)
                ordinal -= 1;
        }

    }

    /**
     * Вставляет хук в начале метода.
     */
    public static class MethodEnter extends HookInjectorMethodVisitor {

        public MethodEnter(MethodVisitor mv, int access, String name, String desc,
                           AsmHook hook, HookInjectorClassVisitor cv) {
            super(mv, access, name, desc, hook, cv);
        }

        @Override
        protected void onMethodEnter() {
            visitHook();
        }

    }

    /**
     * Вставляет хук на каждом выходе из метода, кроме выходов через throw.
     */
    public static class MethodExit extends HookInjectorMethodVisitor {

        public MethodExit(MethodVisitor mv, int access, String name, String desc,
                          AsmHook hook, HookInjectorClassVisitor cv) {
            super(mv, access, name, desc, hook, cv);
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
    public static class LineNumber extends HookInjectorMethodVisitor {

        private int lineNumber;

        public LineNumber(MethodVisitor mv, int access, String name, String desc,
                          AsmHook hook, HookInjectorClassVisitor cv, int lineNumber) {
            super(mv, access, name, desc, hook, cv);
            this.lineNumber = lineNumber;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            super.visitLineNumber(line, start);
            if (this.lineNumber == line) visitHook();
        }
    }

}
