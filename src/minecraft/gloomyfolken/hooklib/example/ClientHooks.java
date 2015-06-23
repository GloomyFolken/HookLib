package gloomyfolken.hooklib.example;

/**
 * Клиентские хуки стоит вытаскивать в отдельный класс,
 * чтобы не крашнуть сервер из-за отсутствия на нём клиентских классов.
 */
public class ClientHooks {

    public static void onResize(int x, int y){
        System.out.println("Resize, x=" + x + ", y=" + y);
    }

}
