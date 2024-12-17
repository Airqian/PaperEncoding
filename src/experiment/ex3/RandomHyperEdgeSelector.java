package experiment.ex3;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

// 从数据集里随机选择 N 条超边进行检索（N有个范围？模拟简单查询和复杂查询）
// 数据集为 NC NS CB TA CG CD
public class RandomHyperEdgeSelector {
    private final static String dataset = "coauth-DBLP";

    private final static String DATA_FILE = "src/dataset/temporal-restricted/" + dataset + "/hyperedge-id-unique.txt";

    private final static String OUTPUT_FILE = "src/experiment/ex3/files/" + dataset + "-selectedEdges.txt";

    public final static int NUM_EDGE_PER_GROUP = 10;

    static class Hyperedge {
        long id; // 超边ID
        List<Long> vertices; // 顶点集合
        String time; // 超边时间

        Hyperedge(long id, List<Long> vertices, String time) {
            this.id = id;
            this.vertices = vertices;
            this.time = time;
        }

        @Override
        public String toString() {
            return id + "\t" + vertices.stream().map(String::valueOf).collect(Collectors.joining("\t")) + "\t" + time;
        }
    }

    public static void main(String[] args) throws IOException {
        // 数据文件路径
        String inputFilePath = DATA_FILE;


        // 1. 读取超边数据
        List<Hyperedge> hyperedges = loadHyperedges(inputFilePath);

        // 2. 按顶点数量分类
        int[] total = new int[]{100, 400};
        List<Hyperedge> selectedHyperedges = new ArrayList<>();
        Set<Long> ids = new HashSet<>();

        for (int i = 0; i < total.length; i++) {
            int N = total[i];
            String outputFilePath = "src/experiment/ex3/files/" + dataset + "-" + N + ".txt";

            while (ids.size() < total[i]) {
                int idx = (int) (Math.random() * hyperedges.size());
                Hyperedge hyperedge = hyperedges.get(idx);
                if (ids.contains(hyperedge.id))
                    continue;
                selectedHyperedges.add(hyperedge);
                ids.add(hyperedge.id);
            }

            // 4. 保存到文件
            saveHyperedges(selectedHyperedges, outputFilePath);

            System.out.println("随机选择的超边已保存到文件：" + outputFilePath);
        }
    }

    // 读取文件中的超边数据
    public static List<Hyperedge> loadHyperedges(String filePath) throws IOException {
        List<Hyperedge> hyperedges = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                long id = Long.parseLong(parts[0]); // 超边ID
                List<Long> vertices = new ArrayList<>();
                for (int i = 1; i < parts.length - 1; i++) {
                    vertices.add(Long.parseLong(parts[i])); // 顶点ID
                }
                String time = parts[parts.length - 1]; // 时间
                hyperedges.add(new Hyperedge(id, vertices, time));
            }
        }
        return hyperedges;
    }

    // 保存超边到文件
    public static void saveHyperedges(List<Hyperedge> hyperedges, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Hyperedge edge : hyperedges) {
                writer.write(edge.toString());
                writer.newLine();
            }
        }
    }
}
