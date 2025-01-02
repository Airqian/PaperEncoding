package zHGMatch.graph.hash;

// 假设的基于FxHash的哈希构建器实现
// https://docs.rs/ccl-fxhash/latest/fxhash/
class FxBuildHasher implements BuildHasher {
    @Override
    public int hash(Object key) {
        // 这里可以编写具体的FxHash相关的哈希计算逻辑，此处简单返回对象的hashCode
        return key.hashCode();
    }
}


