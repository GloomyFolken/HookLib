package gloomyfolken.hooklib.asm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface At {
    /**
     * Тип точки инъекции
     */
    public InjectionPoint point();

    /**
     * Сдвиг относительно точки инъекции
     */
    public Shift shift() default Shift.AFTER;

    /**
     * Конкретизация, имя метода, например
     */
    public String target() default "";

    /**
     * Какая по счету операция. -1, если все
     */
    public int ordinal() default -1;
}
