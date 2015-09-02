package gloomyfolken.hooklib.asm;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import org.objectweb.asm.Type;

/**
 * Класс, позволяющий создавать типы из разных входных данных.
 * Эти типы нужны для того, чтобы задавать параметры и возвращаемые значения методов.
 */
public class TypeHelper {

    /**
     * Создает тип по названию класса.
     * Этот метод стоит использовать для классов майнкрафта, форджа и других модов.
     * Пример использования: getType("net.minecraft.world.World") - вернёт тип для World
     * @param className необфусцированное название класса
     * @return соответствующий тип
     */
    public static Type getType(String className){
        return getArrayType(className, 0);
    }

    /**
     * Создает тип для одномерного массива указанного класса.
     * Пример использования: getArrayType("net.minecraft.world.World") - вернёт тип для World[]
     * @param className необфусцированное название класса
     * @return соответствующий классу тип одномерного массива
     */
    public static Type getArrayType(String className){
        return getArrayType(className, 1);
    }

    /**
     * Создает тип для n-мерного массива указанного класса.
     * Пример использования: getArrayType("net.minecraft.world.World", 2) - вернёт тип для World[][]
     * @param className необфусцированное название класса
     * @return соответствующий классу тип n-мерного массива
     */
    public static Type getArrayType(String className, int arrayDimensions){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrayDimensions; i++){
            sb.append("[");
        }
        sb.append("L");
        sb.append(className.replace(".", "/"));
        sb.append(";");
        /*Type mapped = Type.getType(sb.toString());
        Type unmapped = unmap(mapped);
        FMLRelaunchLog.info("[HOOKLIB] Unmapped from " + mapped.getInternalName() + " to " + unmapped.getInternalName());
        return unmap(Type.getType(sb.toString()));*/
        return Type.getType(sb.toString());
    }

    /**
     * Создает тип по указанному классу.
     * ИСПОЛЬЗУЙТЕ ЭТОТ МЕТОД ТОЛЬКО ЧТОБЫ ДОБАВИТЬ ПРИМИТИВЫ, СИСТЕМНЫЕ КЛАССЫ И КЛАССЫ ИЗ БИБЛИОТЕК.
     * НИКОГДА НЕ ПЕРЕДАВАЙТЕ В ЭТОТ МЕТОД КЛАССЫ МАЙНКРАФТА, ФОРДЖА ИЛИ ЛЮБОГО МОДА.
     * ВО ВРЕМЯ ПРИМЕНЕНИЯ ТРАНСФОРМЕРОВ ЭТИ КЛАССЫ ЕЩЁ НЕ ЗАГРУЖЕНЫ.
     * Можно: getType(String.class) - вернёт тип для String
     * Можно: getType(Integer.class) - вернёт тип для Integer
     * Можно: getType(Integer.TYPE) - вернёт тип для int (различайте int и Integer)
     * Можно: getType(new int[0]) - вернёт тип для int[]
     * Можно: getType(List.class) - вернёт тип для List (его генерик-тип роли не играет)
     * НЕЛЬЗЯ: getType(EntityPlayer.class) - это загрузит класс майнкрафта, чего делать нельзя
     * НЕЛЬЗЯ: getType(Class.forName(net.minecraft.entity.player.EntityPlayer)) - то же самое
     * НЕЛЬЗЯ: getType(new EntityPlayer[0]) - и даже это загрузит класс из майна.
     * @param clazz Может быть только примитивом, системным классом или классом из подключенной либы.
     * @return соответствующий тип
     */
    public static Type getType(Class clazz){
        return Type.getType(clazz);
    }

    static String mapDesc(String desc) {
        if (!HookLibPlugin.getObfuscated()) return desc;

        Type methodType = Type.getMethodType(desc);
        Type mappedReturnType = map(methodType.getReturnType());
        Type[] argTypes = methodType.getArgumentTypes();
        Type[] mappedArgTypes = new Type[argTypes.length];
        for (int i = 0; i < mappedArgTypes.length; i++) {
            mappedArgTypes[i] = map(argTypes[i]);
        }
        return Type.getMethodDescriptor(mappedReturnType, mappedArgTypes);
    }

    static Type map(Type type){
        if (!HookLibPlugin.getObfuscated()) return type;

        // void or primitive
        if (type.getSort() < 9) return type;

        //array
        if (type.getSort() == 9){
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < type.getDimensions(); i++){
                sb.append("[");
            }
            sb.append("L");
            sb.append(map(type.getElementType()).getInternalName());
            sb.append(";");
            return Type.getType(sb.toString());
        } else if (type.getSort() == 10){
            String unmappedName = FMLDeobfuscatingRemapper.INSTANCE.map(type.getInternalName());
            return Type.getType("L" + unmappedName + ";");
        } else {
            throw new IllegalArgumentException("Can not map method type!");
        }
    }

}
