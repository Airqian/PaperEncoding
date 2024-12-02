package encoding.util;

// @FunctionalInterface 注解用于表示某个接口是函数式接口
@FunctionalInterface
public interface HashFunctionInterface {
    long apply(String content);
}