package zNIWGraph.graph.util;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class RemoveHyperedgeDuplicate {
    public static void main(String[] args) throws IOException {
        String path = "/Users/wqian/Coding/Java/PaperCoding/src/dataset/hypergraph/WT/";
        // 对超边进行去重
        // 读取文件
        File inputFile = new File(path + "hyperedges-walmart-trips.txt");  // 假设文件名为 edges.txt
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));

        Set<String> uniqueEdges = new HashSet<>();  // 用于去重
        String line;

        // 逐行读取文件
        while ((line = reader.readLine()) != null) {
            // 将当前行的数字拆分成整数并放入 Set 中
            String[] tokens = line.split(",");
            Set<Integer> edge = new HashSet<>();
            for (String token : tokens) {
                edge.add(Integer.parseInt(token.trim()));
            }

            // 将 Set 转换为字符串，加入 HashSet 去重
            if (uniqueEdges.contains(edge.toString()))
                System.out.println(edge.toString());
            uniqueEdges.add(edge.toString());
        }

        reader.close();

        // 如果需要保存去重后的数据到新文件
        File outputFile = new File(path + "hyperedge-removeduplicate.txt");
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

        for (String edge : uniqueEdges) {
            String e = edge.replaceAll("\\s+", "");
            writer.write(e.substring(1, e.length()-1));
            writer.newLine();
        }

        writer.close();
        System.out.println("去重后的数据已保存到 hyperedge-removeduplicate.txt");

    }
}
