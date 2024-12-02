package DataHandler.TempralGraphDataHandler;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * 该类用于对数据集进行去重
 */
public class RemoveDumplicateHandler {
    private static final String HYPEREDGE_ID_FILE = "dataset/temporal-restricted/DAWN/hyperedge-id.txt";

    private static final String HYPEREDGE_LABEL_FILE = "dataset/temporal-restricted/DAWN/hyperedge-label.txt";

    private static final String HYPEREDGE_ID_UNIQUE_FILE = "dataset/temporal-restricted/DAWN/hyperedge-id-unique.txt";

    private static final String HYPEREDGE_LABEL_UNIQUE_FILE = "dataset/temporal-restricted/DAWN/hyperedge-label-unique.txt";

    public static void main(String[] args) {
        removeDupllicate();
        getTotalVertexNum();
    }

    private static void removeDupllicate() {
        BufferedReader idReader;
        BufferedReader labelReader;
        BufferedWriter idWriter;
        BufferedWriter labelWriter;

        try {
            idReader = new BufferedReader(new FileReader(new File(HYPEREDGE_ID_FILE)));
            labelReader = new BufferedReader(new FileReader(new File(HYPEREDGE_LABEL_FILE)));
            idWriter = new BufferedWriter(new FileWriter(new File(HYPEREDGE_ID_UNIQUE_FILE)));
            labelWriter = new BufferedWriter(new FileWriter(new File(HYPEREDGE_LABEL_UNIQUE_FILE)));

            String idLine;
            String labelLine;
            Set<String> set = new HashSet<>();

            StringBuilder idBuilder = new StringBuilder();
            StringBuilder labelBuilder = new StringBuilder();
            while ((idLine = idReader.readLine()) != null && (labelLine = labelReader.readLine()) != null) {
                String[] ids = split(idLine);
                String[] labels = split(labelLine);

                if (set.contains(ids[1]))
                    continue;
                set.add(ids[1]);
                idBuilder.append(ids[0]).append("\t").append(ids[1]).append("\n");
                labelBuilder.append(labels[0]).append("\t").append(labels[1]).append("\n");
            }

            idWriter.write(idBuilder.toString());
            labelWriter.write(labelBuilder.toString());
            idWriter.close();
            labelWriter.close();
            idReader.close();
            labelReader.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String[] split(String idLine) {
        String[] res = new String[2];
        String[] items = idLine.split("\\t");

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < items.length - 1; i++)
            builder.append(items[i]).append("\t");
        builder.append(items[items.length - 1]);

        res[0] = items[0];
        res[1] = builder.toString();
        return res;
    }

    private static void getTotalVertexNum() {
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(HYPEREDGE_ID_UNIQUE_FILE));
            String line;

            int lines = 0;
            int total = 0;
            while ((line = reader.readLine()) != null) {
                lines++;
                String[] items = line.split("\t");
                total += items.length - 2;
            }
            System.out.println("去重之后数据超图中的超边总数为：" + lines);
            System.out.println("去重之后数据超图中的顶点总数为：" + total);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
