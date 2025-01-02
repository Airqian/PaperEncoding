package zHGMatch.extend;

import zHGMatch.graph.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class NodeSelector {
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

    public List<Integer> select(List<Integer> partial) {
        List<Integer> nids = this.indices.stream()
                .map(partial::get) // 根据 indices 里的索引从 partial 列表中获取对应的元素，得到一个元素流
                .sorted().distinct().collect(Collectors.toList());

        // counter 用于统计 partial 中节点出现的次数，并过滤 nids 列表中不符合 degree 条件的节点
        if (this.degree != null) {
            HashMap<Integer, Integer> counter = new HashMap<>();
            for (int index : this.degree.getValue()) {
                counter.put(partial.get(index), counter.getOrDefault(partial.get(index), 0) + 1);
            }
            nids.removeIf(n -> counter.get(n) != this.degree.getKey());
        }

        // 再从 nids 中过滤不该出现的节点
        if (this.not_in != null) {
            HashSet<Integer> notInNids = not_in.stream().map(partial::get)
                    .collect(Collectors.toCollection(HashSet::new));
            nids.removeIf(notInNids::contains);
        }

        return nids;
    }
}
