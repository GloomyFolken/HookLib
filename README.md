# HookLib
Мод, делающий разработку трансформеров необычайно лёгкой.

Благодаря этому моду становится возможно вставлять хуки (вызовы своих статических методов) в код майнкрафта, форджа и других модов, при этом не обладая никакими познаниями ни в JVM-байткоде, ни в использовании библиотеки ASM.

Запуск в IDE
-----------
Чтобы запустить HookLib и пример к ней в IDE, необходимо дописать в VM arguments: 
```
-Dfml.coreMods.load=gloomyfolken.hooklib.asm.HookLibPlugin,gloomyfolken.hooklib.example.ExampleLoadingPlugin
```
В IntelliJ IDEA: Run -> Edit configurations

В Eclipse: Run -> Run (Debug) configurations

Запуск из .jar-файла
--------------------
Чтобы кормод запускался из джарника, лежащего в папке mods, в джарнике должен быть файл META-INF/MANIFEST.MF со следующим содержанием:
```
Manifest-Version: 1.0
FMLCorePlugin: name.of.YourLoadingPlugin
FMLCorePluginContainsFMLMod: true
Created-By: 1.7.0 (Oracle Corporation)
```

Пример использования
-------------------

```java
AsmHook.newBuilder()
    .setTargetClassName("net.minecraft.client.Minecraft") // модифицируем класс Minecraft
    .setTargetMethodName("resize") // модифицируем метод resize
    .setTargetMethodObfuscatedName("a") // указываем обфусцированное название метода
    .appendTargetMethodParameters(Type.INT_TYPE, Type.INT_TYPE) // указываем параметры метода
    .setHooksClassName("gloomyfolken.hooklib.example.ClientHooks") // хук-метод находится в этом классе
    .setHookMethodName("onResize") // хук-метод называется onResize
    .appendHookMethodParameter(Type.INT_TYPE, 1) // добавляем параметр хук-метода, передаем в него par1
    .appendHookMethodParameter(Type.INT_TYPE, 2) // добавляем параметр хук-метода, передаем в него par2
    .buildAndRegister(); // создаем и регистрируем хук
```

Поддержка версий Minecraft
--------------------------
HookLib не использует никаких классов Майнкрафта, поэтому с выходом новых версий ничего переписывать не надо. Небольшой проблемой являются обновления форджа: он слегка меняется со временем, и с очередной версий может потребоваться какой-нибудь фикс. Сейчас HookLib протестирована на Minecraft 1.6.4 и 1.7.10. На 1.8 работать не будет из-за того, что пакет cpw.mods.fml теперь называется net.minecraftforge.fml. После правки на 1.8 тоже должно заработать.

