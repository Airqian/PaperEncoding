package zHGMatch.graph;

import java.util.*;

public class EdgeProcessor {
    private List<List<Integer>> edges;

    public EdgeProcessor() {
        this.edges = new ArrayList<>();
    }

    /**
     * 添加一条边到 edges 列表中。
     *
     * @param edge 边的节点列表
     */
    public void addEdge(List<Integer> edge) {
        edges.add(edge);
    }

    /**
     * 对 edges 进行去重并排序。
     * 使用 TreeSet 和自定义比较器。
     */
    public void deduplicateAndSortEdges() {
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
        sortedSet.addAll(edges);

        // 更新 edges 为去重和排序后的列表
        edges = new ArrayList<>(sortedSet);
    }

    /**
     * 获取处理后的 edges 列表。
     *
     * @return 处理后的 edges
     */
    public List<List<Integer>> getEdges() {
        return edges;
    }

    public static void main(String[] args) {
        EdgeProcessor processor = new EdgeProcessor();

        // 添加示例边
        processor.addEdge(Arrays.asList(2, 3, 5));
        processor.addEdge(Arrays.asList(3, 4, 5));
        processor.addEdge(Arrays.asList(1, 2, 3));
        processor.addEdge(Arrays.asList(2, 3, 4));
        processor.addEdge(Arrays.asList(1, 2, 3)); // 重复边
        processor.addEdge(Arrays.asList(2, 3, 5)); // 重复边

        System.out.println("原始 edges:");
        processor.getEdges().forEach(System.out::println);

        // 进行去重和排序
        processor.deduplicateAndSortEdges();

        System.out.println("\n去重并排序后的 edges:");
        processor.getEdges().forEach(System.out::println);
    }
}

