package zHGMatch.graph.hash;

// 假设的基于FnvHash的哈希构建器实现（模拟fnv库相关功能）
class FnvBuildHasher implements BuildHasher {
    @Override
    public int hash(Object key) {
        // 这里编写FnvHash相关的哈希计算逻辑，同样简单示例返回对象的hashCode
        return key.hashCode();
    }
}