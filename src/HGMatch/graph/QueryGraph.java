package HGMatch.graph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QueryGraph {
    private List<Integer> labels;
    private List<List<Integer>> edges;

    public QueryGraph(List<Integer> labels, List<List<Integer>> edges) {
        this.labels = labels;
        this.edges = edges;
    }

    public int num_nodes() {
        return this.labels.size();
    }

    public DynamicHyperGraph to_graph() {
        QueryGraph queryGraph = new QueryGraph(new ArrayList<>(this.labels), new ArrayList<>(this.edges));
        for (List<Integer> edge : queryGraph.edges) {
            edge.sort(Integer::compareTo);
            edge = new ArrayList<>(new HashSet<>(edge));
        }

        // 定义一个比较器，按字典序比较两条边
        Comparator<List<Integer>> listComparator = (list1, list2) -> {
            int size1 = list1.size();
            int size2 = list2.size();
            int minSize = Math.min(size1, size2);
            for (int i = 0; i < minSize; i++) {
                int cmp = Integer.compare(list1.get(i), list2.get(i));
                if (cmp != 0) {
                    return cmp;
                }
            }
            // 如果前 minSize 个元素相同，较短的列表排前面
            return Integer.compare(size1, size2);
        };

        // 使用 TreeSet 进行排序和去重
        Set<List<Integer>> sortedSet = new TreeSet<>(listComparator);
        sortedSet.addAll(queryGraph.edges);

        // 更新 edges 为去重和排序后的列表
        queryGraph.edges = new ArrayList<>(sortedSet);

        return toDynamicHyperGraph(queryGraph);
    }

    public DynamicHyperGraph toDynamicHyperGraph(QueryGraph graph) {
        DynamicHyperGraph dynamicHyperGraph = DynamicHyperGraph.withCapacity(graph.labels.size(), graph.edges.size());

        Map<Integer, Integer> res = IntStream.range(0, graph.labels.size())
                .boxed().collect(Collectors.toMap(index -> index, graph.labels::get));
        dynamicHyperGraph.extendNodes(res.entrySet().iterator());

        for (List<Integer> edge : graph.edges) {
            edge.sort(Integer::compareTo);
        }
        dynamicHyperGraph.extendEdges(graph.edges.stream().iterator());
        return dynamicHyperGraph;
    }

    private List<Long> sort_edge(List<Long> edge) {
        List<Long> sortedEdge = new ArrayList<>(edge);
        sortedEdge.sort(Long::compareTo);
        return sortedEdge;
    }
}
