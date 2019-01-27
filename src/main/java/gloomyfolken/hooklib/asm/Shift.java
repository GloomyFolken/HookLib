package gloomyfolken.hooklib.asm;

//Todo: еще не реализовано

public enum Shift {

    /**
     * До указанной точки вставки
     */
    BEFORE,

    /**
     * После указанной точки вставки
     */
    AFTER,

    /**
     * Вместо указанной точки вставки
     */
    INSTEAD;

    public static Shift valueOfNullable(String shift) {
        return shift == null ? Shift.AFTER : valueOf(shift);
    }
}
