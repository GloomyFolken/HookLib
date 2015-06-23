package gloomyfolken.hooklib.example;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import gloomyfolken.hooklib.asm.AsmHook;
import gloomyfolken.hooklib.asm.ReturnCondition;
import gloomyfolken.hooklib.asm.ReturnValue;
import gloomyfolken.hooklib.asm.TypeHelper;
import org.objectweb.asm.Type;

import java.util.Map;

public class ExampleLoadingPlugin implements IFMLLoadingPlugin {

    // 1.6.x only
    public String[] getLibraryRequestClass() {
        return null;
    }

    // 1.7.x only
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        /**
         * Цель: при каждом ресайзе окна выводить в консоль новый размер
         */
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

        /**
         * Цель: увеличить урон всех мечей вдвое
         */
        AsmHook.newBuilder()
                .setTargetClassName("net.minecraft.item.ItemSword")
                .setTargetMethodName("<init>") // модифицируем конструктор, обфусцированное имя не нужно
                .appendTargetMethodParameters(Type.INT_TYPE, TypeHelper.getType("net.minecraft.item.EnumToolMaterial"))
                .setHooksClassName("gloomyfolken.hooklib.example.CommonHooks")
                .setHookMethodName("twiceDamage")
                // нулевой параметр - это this
                .appendHookMethodParameter(TypeHelper.getType("net.minecraft.item.ItemSword"), 0)
                .setInjectorFactory(AsmHook.ON_EXIT_FACTORY) // вставляем хук в конец метода
                .buildAndRegister();

        /**
         * Цель: уменьшить вдвое показатели брони у всех игроков.
         * P.S: фордж перехватывает получение показателя брони, ну а мы перехватим перехватчик :D
         */
        AsmHook.newBuilder()
                .setTargetClassName("net.minecraftforge.common.ForgeHooks")
                .setTargetMethodName("getTotalArmorValue")
                .appendTargetMethodParameters(TypeHelper.getType("net.minecraft.entity.player.EntityPlayer"))
                .setTargetMethodReturnType(Type.INT_TYPE) // целевой метод возвращает int
                .setHooksClassName("gloomyfolken.hooklib.example.CommonHooks")
                .setHookMethodName("halfArmor")
                // передаем в хук-метод значение, которое иначе было бы передано в return
                .appendReturnValueAsHookMethodParameter()
                .setInjectorFactory(AsmHook.ON_EXIT_FACTORY)
                .setReturnCondition(ReturnCondition.ALWAYS) // всегда вызываем return после хука
                .setReturnValue(ReturnValue.HOOK_RETURN_VALUE) // передаем в return значение, которое вернул хук-метод
                .buildAndRegister();

        /**
         * Цель: запретить возможность телепортироваться в ад и обратно чаще, чем раз в пять секунд.
         */
        AsmHook.newBuilder()
                .setTargetClassName("net.minecraft.entity.player.EntityPlayer")
                .setTargetMethodName("getPortalCooldown")
                .setTargetMethodObfuscatedName("ac")
                .setTargetMethodReturnType(Type.INT_TYPE)
                // Хук-метод можно не указывать, если используется ReturnCondition.ALWAYS.
                .setReturnCondition(ReturnCondition.ALWAYS)
                .setReturnValue(ReturnValue.PRIMITIVE_CONSTANT)
                .setPrimitiveConstant(100)
                .buildAndRegister();
    }
}
