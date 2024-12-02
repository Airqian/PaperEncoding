package DataHandler.TempralGraphDataHandler;


import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 该类用于组装超边以及生成，生成 HYPEREDGE_ID_FILE、HYPEREDGE_LABEL_FILE、NODE_PROPERTY 文件
 */
public class PropertyGenerator {
    // 节点标签文件
    private static final String NODE_LABELS = "src/dataset/temporal-restricted/coauth-MAG-History/coauth-MAG-History-node-labels.txt";

    private static final String NODE_PROPERTY = "src/dataset/temporal-restricted/coauth-MAG-History/node-property";

    public static void main(String[] args) {
        Map<String, String> vertexId2Label = readNodeLabels(); // 节点id到label的映射
        for (int i = 1; i <= 10; i++)
            System.out.println(vertexId2Label.get(String.valueOf(i)));

        generateProperty(vertexId2Label);
    }


    private static void generateProperty(Map<String, String> vertexId2Label) {
        BufferedWriter writer;
        int[] nProperty = {1};

        try {

            for (int i = 0; i < nProperty.length; i++) {
                writer = new BufferedWriter(new FileWriter(new File(NODE_PROPERTY + nProperty[i] + ".txt")));

                StringBuilder builder = new StringBuilder();
                for (String key : vertexId2Label.keySet()) {
                    System.out.println("正在处理顶点：" + key);
                    Set<String> propertyIds = new HashSet<>();
                    while (propertyIds.size() != nProperty[i]) {
//                        propertyIds.add(String.valueOf(RandomUtil.randomInt(vertexId2Label.size())));
                    }

                    builder.append(key).append("\t");
                    for (String propertyId : propertyIds)
                        builder.append(vertexId2Label.get(propertyId)).append("\t");
                    builder.append("\n");
                }

                writer.write(builder.toString());
                writer.close();
            }
        } catch (Exception e) {

        }
    }


    public static Map<String, String> readNodeLabels() {
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(new File(NODE_LABELS)));
            String line;
            Map<String, String> id2Label = new HashMap<>();
            Set<String> set = new HashSet<>();

            int total = 0;
            while ((line = reader.readLine()) != null) {
                String[] items = line.split(" ");
                String value = "";
                for (int i = 1; i < items.length - 1; i++) {
                    value += items[i];
                    value += " ";
                }
                value += items[items.length - 1];
                id2Label.put(items[0], value);
                set.add(value);
                total ++;

//                String[] items = line.split("\t");
//                id2Label.put(items[0], items[1]);
//                set.add(items[1]);
//                total++;
            }

            System.out.println("------------ node-labels 文件 ------------");
            System.out.println("node-labels 文件中总顶点数：" + total);
            System.out.println("node-labels 文件中不同的顶点标签数：" + set.size());
            reader.close();
            return id2Label;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

