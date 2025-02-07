package zNIWGraph.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PartitionedEdgesWrapper {
    private List<Integer> nodeLabels;
    private List<EdgePartitionMapEntry> map;

    public PartitionedEdgesWrapper(List<Integer> nodeLabels, List<EdgePartitionMapEntry> map) {
        this.nodeLabels = nodeLabels;
        this.map = map;
    }

    public PartitionedEdges toPartitionedEdge() {
        // 这里能起到一个去重的作用？
        HashMap<List<Integer>, EdgePartition> newMap = new HashMap<>();
        for (EdgePartitionMapEntry mapEntry : map) {
            newMap.put(new ArrayList<>(mapEntry.key), mapEntry.value.toEdgePartition());
        }
        return new PartitionedEdges(new ArrayList<>(nodeLabels), newMap);
    }

    // 不用 HashMap 的原因是可能有重复的 key
    static class EdgePartitionMapEntry {
        private List<Integer> key;
        private EdgePartitionWrapper value;

        public EdgePartitionMapEntry(List<Integer> key, EdgePartitionWrapper value) {
            this.key = key;
            this.value = value;
        }

        public List<Integer> getKey() {
            return key;
        }

        public EdgePartitionWrapper getValue() {
            return value;
        }
    }
}
