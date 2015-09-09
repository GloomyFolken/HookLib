# HookLib
Мод, делающий разработку трансформеров необычайно лёгкой.

Благодаря этому моду становится возможно вставлять хуки (вызовы своих статических методов) в код майнкрафта, форджа и других модов, при этом не обладая никакими познаниями ни в JVM-байткоде, ни в использовании библиотеки ASM.

Запуск в IDE
-----------
Чтобы запустить HookLib и пример к ней в IDE, необходимо дописать в VM arguments: 
```
-Dfml.coreMods.load=gloomyfolken.hooklib.minecraft.HookLibPlugin,gloomyfolken.hooklib.example.ExampleHookLoader
```
В IntelliJ IDEA: Run -> Edit configurations

В Eclipse: Run -> Run (Debug) configurations

Запуск из .jar-файла
--------------------
Чтобы кормод запускался из джарника, лежащего в папке mods, в джарнике должен быть файл META-INF/MANIFEST.MF с таким содержимым:
```
Manifest-Version: 1.0
FMLCorePlugin: name.of.YourHookLoader
FMLCorePluginContainsFMLMod: true
Created-By: 1.7.0 (Oracle Corporation)
```

Пример использования
-------------------
Полный код и больше примеров есть в gloomyfolken.hooklib.example
```java
@Hook(injectOnExit = true, returnCondition = ReturnCondition.ALWAYS)
public static int getTotalArmorValue(ForgeHooks fh, EntityPlayer player, @ReturnValue int returnValue) {
    return returnValue/2;
}
```

Поддержка версий Minecraft
--------------------------
HookLib не использует никаких классов Майнкрафта, поэтому с выходом новых версий ничего переписывать не надо. Небольшой проблемой являются обновления форджа: он слегка меняется со временем, и с очередной версий может потребоваться какой-нибудь фикс. Сейчас HookLib протестирована на Minecraft 1.6.4 и 1.7.10. На 1.8 работать не будет из-за того, что пакет cpw.mods.fml теперь называется net.minecraftforge.fml. После правки на 1.8 тоже должно заработать.

