package gloomyfolken.hooklib.asm;

public enum InjectionPoint {

    /**
     * Начало метода
     */
    HEAD,

    /**
     * Конец метода
     */
    RETURN,

    /**
     * Когда происходит вызов другого метода где-то в теле хукнутого
     */
    METHOD_CALL

}
