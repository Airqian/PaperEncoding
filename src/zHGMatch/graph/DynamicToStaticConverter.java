package zHGMatch.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// 实现从DynamicHyperGraph转换为StaticHyperGraph的逻辑，这里通过构造函数来模拟From特征实现（Java中没有完全一样的语法）
class DynamicToStaticConverter {
    public static StaticHyperGraph from(DynamicHyperGraph graph) {
        // TODO 转的是key还是value
        List<Integer> newLabels = graph.getLabels().values().stream().collect(Collectors.toList());
        List<List<Integer>> newEdges = new ArrayList<>();

        for (List<Integer> e : graph.getEdges()) {
            List<Integer> edge = new ArrayList<>();
            for (int v : e) {
                edge.add(graph.getLabels().get(v));
            }
            newEdges.add(edge);
        }
        return new StaticHyperGraph(newLabels, newEdges);
    }
}
