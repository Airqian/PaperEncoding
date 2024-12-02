package experiment.ex1;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class RandomHyperedgeSelector {
    private final static String DATA_FILE = "src/dataset/temporal-restricted/tags-stack-overflow/hyperedge-id-unique.txt";

    private final static String OUTPUT_FILE = "src/experiment/ex1/files/tags-stack-overflow-selectedEdges.txt";

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
        String outputFilePath = OUTPUT_FILE;

        // 1. 读取超边数据
        List<Hyperedge> hyperedges = loadHyperedges(inputFilePath);

        // 2. 按顶点数量分类
        Map<Integer, List<Hyperedge>> groupedByVertexCount = hyperedges.stream()
                .collect(Collectors.groupingBy(edge -> edge.vertices.size()));

        // 3. 随机选择顶点数量为 5、6、7、8 的超边各 NUM_EDGE_PER_GROUP 条
        List<Hyperedge> selectedHyperedges = new ArrayList<>();
        Random random = new Random();
        for (int vertexCount = 2; vertexCount <= 5; vertexCount++) {
            System.out.println("vertexCount = " + vertexCount);
            List<Hyperedge> group = groupedByVertexCount.getOrDefault(vertexCount, new ArrayList<>());
            Collections.shuffle(group); // 打乱顺序
            selectedHyperedges.addAll(group.stream().limit(NUM_EDGE_PER_GROUP).collect(Collectors.toList()));
        }

        // 4. 保存到文件
        saveHyperedges(selectedHyperedges, outputFilePath);

        System.out.println("随机选择的超边已保存到文件：" + outputFilePath);
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

