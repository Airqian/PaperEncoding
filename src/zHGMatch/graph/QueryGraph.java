package zHGMatch.graph;

import org.json.JSONArray;
import org.json.JSONObject;

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

    public List<Integer> getLabels() {
        return labels;
    }

    public List<List<Integer>> getEdges() {
        return edges;
    }

    public int num_nodes() {
        return this.labels.size();
    }

    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>();
        list.add(5);
        list.add(3);
        list.add(8);
        list.add(3);
        list.add(1);
        list.add(5);

        List<Integer> collect = list.stream()
                .distinct()   // 去重
                .sorted()     // 排序
                .collect(Collectors.toList());
        list = collect;

        System.out.println(list);
    }

    // 静态方法，用于从 JSONObject 创建 GraphData 实例
    public static QueryGraph fromJSONObject(JSONObject obj) {
        JSONArray labelsArray = obj.getJSONArray("labels");
        List<Integer> labels = new ArrayList<>();
        for (int i = 0; i < labelsArray.length(); i++) {
            labels.add(labelsArray.getInt(i));
        }

        JSONArray edgesArray = obj.getJSONArray("edges");
        List<List<Integer>> edges = new ArrayList<>();

        for (int i = 0; i < edgesArray.length(); i++) {
            JSONArray edge = edgesArray.getJSONArray(i);
            List<Integer> edgeList = new ArrayList<>();
            for (int j = 0; j < edge.length(); j++) {
                edgeList.add(edge.getInt(j));
            }
            edges.add(edgeList);
        }

        return new QueryGraph(labels, edges);
    }

    public DynamicHyperGraph to_graph() {
        QueryGraph queryGraph = new QueryGraph(new ArrayList<>(this.labels), new ArrayList<>(this.edges));
        // 对edge进行排序、去重
        for (List<Integer> edge : queryGraph.edges) {
            edge.sort(Integer::compareTo);
            deduplicateInPlace(edge);
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

    /**
     * 对已排序的 ArrayList<Integer> 进行去重，直接在原列表上操作。
     *
     * @param list 已排序的 ArrayList<Integer>
     */
    public static void deduplicateInPlace(List<Integer> list) {
        if (list.isEmpty()) {
            return;
        }

        // 使用一个索引来跟踪当前唯一元素的位置
        int uniqueIndex = 0;

        // 从第二个元素开始遍历
        for (int current = 1; current < list.size(); current++) {
            if (!list.get(current).equals(list.get(uniqueIndex))) {
                // 找到一个新的唯一元素，移动到 uniqueIndex + 1 位置
                uniqueIndex++;
                list.set(uniqueIndex, list.get(current));
            }
        }

        // 删除所有重复的元素
        // 从 uniqueIndex + 1 开始删除
        while (list.size() > uniqueIndex + 1) {
            list.remove(list.size() - 1);
        }
    }

    public DynamicHyperGraph toDynamicHyperGraph(QueryGraph graph) {
        DynamicHyperGraph dynamicHyperGraph = DynamicHyperGraph.withCapacity(graph.labels.size(), graph.edges.size());

        // 对labels构建一个下标到值的map
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
