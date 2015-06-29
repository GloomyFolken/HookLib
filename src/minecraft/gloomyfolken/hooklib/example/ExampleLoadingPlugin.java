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
                .setTargetClass("net.minecraft.client.Minecraft") // модифицируем класс Minecraft
                .setTargetMethod("resize") // модифицируем метод resize
                .setTargetMethodObfName("a") // указываем обфусцированное название метода
                .addTargetMethodParameters(Type.INT_TYPE, Type.INT_TYPE) // указываем параметры метода
                .setHookClass("gloomyfolken.hooklib.example.ClientHooks") // хук-метод находится в этом классе
                .setHookMethod("onResize") // хук-метод называется onResize
                .addHookMethodParameter(Type.INT_TYPE, 1) // добавляем параметр хук-метода, передаем в него par1
                .addHookMethodParameter(Type.INT_TYPE, 2) // добавляем параметр хук-метода, передаем в него par2
                .buildAndRegister(); // создаем и регистрируем хук

        /**
         * Цель: увеличить урон всех мечей вдвое
         */
        AsmHook.newBuilder()
                .setTargetClass("net.minecraft.item.ItemSword")
                .setTargetMethod("<init>") // модифицируем конструктор, обфусцированное имя не нужно
                .addTargetMethodParameters(Type.INT_TYPE, TypeHelper.getType("net.minecraft.item.EnumToolMaterial"))
                .setHookClass("gloomyfolken.hooklib.example.CommonHooks")
                .setHookMethod("twiceDamage")
                // нулевой параметр - это this
                .addHookMethodParameter("net.minecraft.item.ItemSword", 0)
                .setInjectorFactory(AsmHook.ON_EXIT_FACTORY) // вставляем хук в конец метода
                .buildAndRegister();

        /**
         * Цель: уменьшить вдвое показатели брони у всех игроков.
         * P.S: фордж перехватывает получение показателя брони, ну а мы перехватим перехватчик :D
         */
        AsmHook.newBuilder()
                .setTargetClass("net.minecraftforge.common.ForgeHooks")
                .setTargetMethod("getTotalArmorValue")
                .addTargetMethodParameters("net.minecraft.entity.player.EntityPlayer")
                .setTargetMethodReturnType(Type.INT_TYPE) // целевой метод возвращает int
                .setHookClass("gloomyfolken.hooklib.example.CommonHooks")
                .setHookMethod("halfArmor")
                // передаем в хук-метод значение, которое иначе было бы передано в return
                .addReturnValueToHookMethodParameters()
                .setInjectorFactory(AsmHook.ON_EXIT_FACTORY)
                .setReturnCondition(ReturnCondition.ALWAYS) // всегда вызываем return после хука
                .setReturnValue(ReturnValue.HOOK_RETURN_VALUE) // передаем в return значение, которое вернул хук-метод
                .buildAndRegister();

        /**
         * Цель: запретить возможность телепортироваться в ад и обратно чаще, чем раз в пять секунд.
         */
        AsmHook.newBuilder()
                .setTargetClass("net.minecraft.entity.player.EntityPlayer")
                .setTargetMethod("getPortalCooldown")
                .setTargetMethodObfName("ac")
                .setTargetMethodReturnType(Type.INT_TYPE)
                // Хук-метод можно не указывать, если используется ReturnCondition.ALWAYS.
                .setReturnCondition(ReturnCondition.ALWAYS)
                .setReturnValue(ReturnValue.PRIMITIVE_CONSTANT)
                .setPrimitiveConstant(100)
                .buildAndRegister();
    }
}
