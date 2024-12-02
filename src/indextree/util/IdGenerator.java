package indextree.util;

public class IdGenerator {
    private static long id = 10000000000l;
    public static long getNextId() {
        return ++id;
    }
}
