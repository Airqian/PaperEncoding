package zHyperISO.util;

/**
 * 简单的键值对类，用于表示 Pair。
 *
 * @param <K> 键的类型
 * @param <V> 值的类型
 */
public class Pair<K, V> {
    private final K key;
    private final V value;

    /**
     * 构造函数，用于创建一个 Pair 实例。
     *
     * @param key   键
     * @param value 值
     */
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    // Getter 方法
    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }


}