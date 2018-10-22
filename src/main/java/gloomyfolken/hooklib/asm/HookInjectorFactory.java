package gloomyfolken.hooklib.asm;

import org.objectweb.asm.MethodVisitor;

/**
 * Фабрика, задающая тип инжектора хуков. Фактически, от выбора фабрики зависит то, в какие участки кода попадёт хук.
 * "Из коробки" доступно два три инжекторов: MethodEnter, который вставляет хук на входе в метод,
 * MethodExit, который вставляет хук на каждом выходе,
 * и ByAnchor, который позволяет вставлять еще и в места вызовов других методов
 */
public abstract class HookInjectorFactory {

    /**
     * Метод AdviceAdapter#visitInsn() - штука странная. Там почему-то вызов следующего MethodVisitor'a
     * производится после логики, а не до, как во всех остальных случаях. Поэтому для MethodExit приоритет
     * хуков инвертируется.
     */
    protected boolean isPriorityInverted = false;

    abstract HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc,
                                                          AsmHook hook, HookInjectorClassVisitor cv);

    static class ByAnchor extends HookInjectorFactory {

        public static final ByAnchor INSTANCE = new ByAnchor();

        private ByAnchor() {}

        public HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc,
                                                            AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.ByAnchor(mv, access, name, desc, hook, cv);
        }

    }


    static class MethodEnter extends HookInjectorFactory {

        public static final MethodEnter INSTANCE = new MethodEnter();

        private MethodEnter() {}

        @Override
        public HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc,
                                                            AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.MethodEnter(mv, access, name, desc, hook, cv);
        }

    }

    static class MethodExit extends HookInjectorFactory {

        public static final MethodExit INSTANCE = new MethodExit();

        private MethodExit() {
            isPriorityInverted = true;
        }

        @Override
        public HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc,
                                                            AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.MethodExit(mv, access, name, desc, hook, cv);
        }
    }

    static class LineNumber extends HookInjectorFactory {

        private int lineNumber;

        public LineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        @Override
        public HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc,
                                                            AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.LineNumber(mv, access, name, desc, hook, cv, lineNumber);
        }
    }

}
