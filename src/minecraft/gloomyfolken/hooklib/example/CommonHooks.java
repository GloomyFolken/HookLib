package gloomyfolken.hooklib.example;

import net.minecraft.item.ItemSword;

public class CommonHooks {

    public static void twiceDamage(ItemSword sword){
        // поле нужно объявить публичным, чтобы оно скомпилировалось, но никаких изменений в собранном майне
        // не потребуется. Фордж при запуске делает все поля публичными.
        sword.weaponDamage *= 2;
    }

    public static int halfArmor(int value){
        return value / 2;
    }

}
