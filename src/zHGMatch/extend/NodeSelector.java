package zHGMatch.extend;

import zHGMatch.graph.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NodeSelector {
    /**
     * 从大局来看，当前的部分查询（嵌入）中会包含存在于待匹配超边中的顶点，全量的这些顶点就保存在 indices 当中
     * 然后通过 degree（公共顶点出现的次数）和 not_in（不可能出现在待匹配超边中的顶点）过滤掉某些顶点
     * 接着将剩余的顶点从倒排索引中找到可能出现的超边，取这些超边的交集，就得到了候选超边集合。
     *
     * NodeSelector 产生于每一条 match_edge 的每一个公共顶点 adj，计算每一个 NodeSelector 都会产生相应的可能顶点结果列表，这些可能顶点结果列表的
     * 交集才有可能是最终的答案
     *
     * 总之，NodeSelector 类的作用就是尽可能的过滤掉一部分顶点留下公共顶点，接着通过公共顶点寻找超边
     */
    private List<Integer> indices;
    private Pair<Integer, List<Integer>> degree;
    private List<Integer> not_in;

    public NodeSelector(List<Integer> indices) {
        this.indices = indices;
        this.degree = null;
        this.not_in = null;
    }

    public void setDegree(Pair<Integer, List<Integer>> degree) {
        this.degree = degree;
    }

    public void setNot_in(List<Integer> not_in) {
        this.not_in = not_in;
    }

    public List<Integer> getIndices() {
        return indices;
    }

    // ‼️‼️‼️‼️ 返回的是部分嵌入中可能的候选顶点，然后通过这些候选顶点找候选超边
    public List<Integer> select(List<Integer> partial) {
        /**
         * partial 是当前的部分嵌入，下标和 indices 对应
         * this.indices 表示当前待匹配超边的「某条邻边」中，与 adj 标签相同的顶点索引位置，也就是其邻边里有 indices.size() 个顶点的标签与 adj 相同
         * nids 为根据 indices 从 partial 中取出的对应的顶点 id，索引位置标记的顶点标签和取出的顶点的标签是对得上的，前面会对顶点、标签进行排序
         * nids 保存的是邻边的候选超边的顶点
         */
        List<Integer> nids = this.indices.stream()
                .map(partial::get) // 根据 indices 里的索引从 partial 列表中获取对应的元素，得到一个元素流
                .sorted().distinct().collect(Collectors.toList());

        /**
         * this.degree 为 Pair<Integer, List<Integer>> 结构
         * key 为部分查询中所有包含 adj 的邻边数量，而 value 表示这些超边中标签为 adj_label 的顶点索引
         *
         * 接下来，counter 记录这些索引代表的点出现的次数，key 的候选顶点 id，value 为出现次数
         * 然后在 nids 中筛选掉出现次数不等于 degree 的点，筛选掉的点一定不是公共顶点，也不会出现在待匹配超边的候选超边中
         */
        if (this.degree != null) {
            HashMap<Integer, Integer> counter = new HashMap<>();
            for (int index : this.degree.getValue()) {
                counter.put(partial.get(index), counter.getOrDefault(partial.get(index), 0) + 1);
            }
            nids.removeIf(n -> counter.get(n) != this.degree.getKey());
        }

        /**
         * not_in 记录不与 cur_edge 共享顶点 adj 的超边中标签为 adj_label 的顶点索引，这些顶点也不可能出现在 eq 中，相应的也不可能出现在 f(eq) 中
         * 从 nids 中过滤掉不可能出现在 f(eq) 中的顶点
         */
        if (this.not_in != null) {
            HashSet<Integer> notInNids = not_in.stream().map(partial::get)
                    .collect(Collectors.toCollection(HashSet::new));
            nids.removeIf(notInNids::contains);
        }

        return nids;
    }

    // 重写 equals() 方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // 同一对象
        if (o == null || getClass() != o.getClass()) return false; // 不同类或空

        NodeSelector that = (NodeSelector) o;

        // 使用 Objects.equals 处理可能为 null 的字段
        if (!Objects.equals(indices, that.indices)) return false;
        if (!Objects.equals(degree, that.degree)) return false;
        return Objects.equals(not_in, that.not_in);
    }

    // 重写 hashCode() 方法
    @Override
    public int hashCode() {
        return Objects.hash(indices, degree, not_in);
    }
}
