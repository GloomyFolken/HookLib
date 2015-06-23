package gloomyfolken.hooklib.asm;

import org.objectweb.asm.MethodVisitor;

/**
 * Фабрика, задающая тип инжектора хуков. Фактически, от выбора фабрики зависит то, в какие участки кода попадёт хук.
 * "Из коробки" доступно два типа инжекторов: MethodEnter, который вставляет хук на входе в метод,
 * и MethodExit, который вставляет хук на каждом выходе.
 */
public interface IHookInjectorFactory {

    HookInjector createHookInjector(MethodVisitor mv, int access, String name, String desc, AsmHook hook);

    class MethodEnter implements IHookInjectorFactory {

        public static final MethodEnter INSTANCE = new MethodEnter();

        private MethodEnter(){}

        @Override
        public HookInjector createHookInjector(MethodVisitor mv, int access, String name, String desc, AsmHook hook) {
            return new HookInjector.MethodEnter(mv, access, name, desc, hook);
        }
    }

    class MethodExit implements IHookInjectorFactory {

        public static final MethodExit INSTANCE = new MethodExit();

        private MethodExit(){}

        @Override
        public HookInjector createHookInjector(MethodVisitor mv, int access, String name, String desc, AsmHook hook) {
            return new HookInjector.MethodExit(mv, access, name, desc, hook);
        }
    }

}
