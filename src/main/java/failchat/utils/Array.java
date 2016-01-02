package failchat.utils;

public class Array {

    @SafeVarargs
    public static <T> T[] of(T... elements) {
        return elements;
    }
}
