package HGMatch.graph;

import java.util.List;

// serde.rs
public class EdgePartitionWrapper {
    private int arity;
    private List<Integer> labels;
    private List<Integer> edges;
    private List<Pair<Integer, List<Integer>>> index;  // 表示点到边的倒排索引，我觉得用HashMap也够了

    public EdgePartitionWrapper(int arity, List<Integer> labels, List<Integer> edges, List<Pair<Integer, List<Integer>>> index) {
        this.arity = arity;
        this.labels = labels;
        this.edges = edges;
        this.index = index;
    }

    // 获取arity属性的方法
    public int getArity() {
        return arity;
    }

    // 获取labels属性的方法
    public List<Integer> getLabels() {
        return labels;
    }

    // 获取edges属性的方法
    public List<Integer> getEdges() {
        return edges;
    }

    // 获取index属性的方法
    public List<Pair<Integer, List<Integer>>> getIndex() {
        return index;
    }

    /**
     * 简单的键值对类，用于表示 Pair。
     *
     * @param <K> 键的类型
     * @param <V> 值的类型
     */
    class Pair<K, V> {
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
}
