package HGMatch.graph;

import java.util.List;

public class PartitionedEdgesWrapper {
    private List<Integer> nodeLabels;
    private List<EdgePartitionMapEntry> map;

    public PartitionedEdgesWrapper(List<Integer> nodeLabels, List<EdgePartitionMapEntry> map) {
        this.nodeLabels = nodeLabels;
        this.map = map;
    }

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
