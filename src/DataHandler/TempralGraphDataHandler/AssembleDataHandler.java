package DataHandler.TempralGraphDataHandler;

import indextree.util.IdGenerator;

import java.io.*;
import java.util.*;

/**
 * 该类用于组装超边以及生成，生成 HYPEREDGE_ID_FILE、HYPEREDGE_LABEL_FILE、NODE_PROPERTY 文件
 */
public class AssembleDataHandler {
    // 节点标签文件
    private static final String NODE_LABELS = "dataset/temporal-restricted/tags-stack-overflow/tags-stack-overflow-node-labels.txt";

    // 每条超边包含的顶点数文件
    private static final String NVERTS = "dataset/temporal-restricted/tags-stack-overflow/tags-stack-overflow-nverts.txt";

    // 超边包含的具体顶点
    private static final String SIMPLICES = "dataset/temporal-restricted/tags-stack-overflow/tags-stack-overflow-simplices.txt";

    private static final String TIMES = "dataset/temporal-restricted/tags-stack-overflow/tags-stack-overflow-times.txt";

    private static final String HYPEREDGE_ID_FILE = "dataset/temporal-restricted/tags-stack-overflow/hyperedge-id.txt";

    private static final String HYPEREDGE_LABEL_FILE = "dataset/temporal-restricted/tags-stack-overflow/hyperedge-label.txt";

    private static final String NODE_PROPERTY = "dataset/temporal-restricted/tags-stack-overflow/node-property";

    public static void main(String[] args) {
        Map<String, String> vertexId2Label = readNodeLabels(); // 节点id到label的映射
        LinkedList<Integer> nverts = readNverts(); // 每条超边包含的顶点数量
        LinkedList<Integer> simplices = readSimplices(); // 按超边罗列包含的顶点id
        LinkedList<Long> timeList = readTime(); // 超边的时间列表

        for (int i = 1; i <= 10; i++)
            System.out.println(vertexId2Label.get(String.valueOf(i)));

//        handleLabelAndID(vertexId2Label, nverts, simplices, timeList);
        generateProperty(vertexId2Label);
    }

    // 处理超边到节点的映射
    private static void handleLabelAndID(Map<String, String> vertexId2Label, List<Integer> nverts, LinkedList<Integer> simplices, LinkedList<Long> times) {
        BufferedWriter labelWriter;
        BufferedWriter idWriter;

        try {
            labelWriter = new BufferedWriter(new FileWriter(HYPEREDGE_LABEL_FILE));
            idWriter = new BufferedWriter(new FileWriter(HYPEREDGE_ID_FILE));
            StringBuilder idBuilder = new StringBuilder();
            StringBuilder labelBuilder = new StringBuilder();

            int j = 0; // 在 simplices 中的下标，如果是分组进行的话输出的就是下一个要处理的下标
            for (int i = 0; i < nverts.size(); i++) { // i 是 nverts中的下标
                System.out.println("第 " + i + " 条超边");
                int nvertex = nverts.get(i);
                if (nvertex == 1) {
                    j++;
                    continue;
                }

                long edgeId = IdGenerator.getNextId();
                idBuilder.append(edgeId).append("\t"); // 超边id
                labelBuilder.append(edgeId).append("\t");

                int k = j;
                j = j + nvertex;
                for (; k < j; k++) {
                    int vertexId = simplices.get(k);
                    idBuilder.append(vertexId).append("\t"); // 顶点id
                    labelBuilder.append(vertexId2Label.get(String.valueOf(vertexId))).append("\t");
                }

                idBuilder.append(times.get(i)).append("\n");
                labelBuilder.append(times.get(i)).append("\n");
            }

            System.out.println("j = " + j);
            idWriter.write(idBuilder.toString());
            labelWriter.write(labelBuilder.toString());

            labelWriter.close();
            idWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateProperty(Map<String, String> vertexId2Label) {
        BufferedWriter writer;
        int[] nProperty = {1,2,3,4,5,6};

        try {

            for (int i = 0; i < nProperty.length; i++) {
                writer = new BufferedWriter(new FileWriter(new File(NODE_PROPERTY + nProperty[i] + ".txt")));

                StringBuilder builder = new StringBuilder();
                for (String key : vertexId2Label.keySet()) {
                    System.out.println("正在处理顶点：" + key);
                    Set<String> propertyIds = new HashSet<>();
                    while (propertyIds.size() != nProperty[i]) {
//                        propertyIds.add(String.valueOf(Random.randomInt(vertexId2Label.size())));
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
    public static LinkedList<Integer> readNverts() {
        BufferedReader reader;
        LinkedList<Integer> nvertex = new LinkedList<>();

        try {
            reader = new BufferedReader(new FileReader(new File(NVERTS)));
            String line;

            int totalEdges = 0; // 超边数量，相当于文件中的行数
            int totalVerties = 0; // 整个数据超图的总顶点数
            int geqOneVerticesTotalNum = 0; // 剔除了单节点超边后数据超图的顶点规模
            int geqOneVertexEdgesNum = 0; // 顶点数大于1的超边数量
            int most = 0;

            while ((line = reader.readLine()) != null) {
                int vertexNum = Integer.parseInt(line);
                if (vertexNum != 1) {
                    geqOneVertexEdgesNum++;
                    geqOneVerticesTotalNum += vertexNum;
                }

                totalEdges ++;
                totalVerties += vertexNum;
                most = Math.max(most, vertexNum);
                nvertex.add(vertexNum);
            }

            System.out.println("------------ nvertes 文件 ------------");
            System.out.println("总超边数：" + totalEdges);
            System.out.println("总顶点数：" + totalVerties);
            System.out.println("节点数大于 1 的超边数：" + geqOneVertexEdgesNum);
            System.out.println("剔除了单节点超边后数据超图的顶点总数：" + geqOneVerticesTotalNum);
            System.out.println("超边中的最大顶点数：" + most);
            reader.close();
            return nvertex;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    public static LinkedList<Integer> readSimplices() {
        BufferedReader reader;
        LinkedList<Integer> list = new LinkedList<>();

        try {
            reader = new BufferedReader(new FileReader(new File(SIMPLICES)));
            String line;

            Set<Integer> set = new HashSet<>();
            while ((line = reader.readLine()) != null) {
                int nodeId = Integer.parseInt(line);
                list.add(nodeId);
                set.add(nodeId);
            }

            System.out.println("------------ simplices 文件 ------------");
            System.out.println("simplices 中顶点总数为：" + list.size());
            System.out.println("不同顶点标签数量为：" + set.size());
            reader.close();
            return list;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static LinkedList<Long> readTime() {
        BufferedReader reader;
        LinkedList<Long> timeList = new LinkedList<>();

        try {
            reader = new BufferedReader(new FileReader(new File(TIMES)));
            String line;

            int total = 0;
            while ((line = reader.readLine()) != null) {
                long time = Long.parseLong(line);
                timeList.add(time);
                total++;
            }

            System.out.println("------------ times 文件 ------------");
            System.out.println("时间的数量为：" + total);
            reader.close();
            return timeList;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
