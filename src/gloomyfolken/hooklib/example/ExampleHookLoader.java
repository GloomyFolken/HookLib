package gloomyfolken.hooklib.example;

import gloomyfolken.hooklib.minecraft.HookLoader;

public class ExampleHookLoader extends HookLoader {

    @Override
    public void registerHooks() {
        registerHookContainer("gloomyfolken.hooklib.example.AnnotationHooks");
    }
}
