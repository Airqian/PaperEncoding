package  experiment.ex1;

import DataHandler.TempralGraphDataHandler.IndexTreeBuilder;
import encoding.PPBitset;
import encoding.PropertyEncodingConstructor;
import indextree.IndexTree;
import indextree.hyperedge.DataHyperedge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static experiment.ex1.RandomUniqueVertexSelector.NUM_EDGE_PER_GROUP;

public class Main {
    public static void main(String[] args) {
        String dataset = "coauth-DBLP";
        final int nProperty = 3;
        String hyperedgeIdFile = "src/dataset/temporal-restricted/" + dataset + "/hyperedge-id-unique.txt";
        String hyperedgeLabelFile = "src/dataset/temporal-restricted/" + dataset + "/hyperedge-label-unique.txt";
        String propertyFile = "src/dataset/temporal-restricted/" + dataset + "/node-property" + nProperty + ".txt";
        String queryFile = "src/experiment/ex1/files/" + dataset + "-selectedEdges.txt";
        String treeInfo = "src/experiment/ex1/files/" + dataset + "-TreeInfo.txt";

        // 构建索引树所需要的参数
        int windowSize = 128;
        int hashFuncCount = 2;
        int secondaryIndexSize = 8;
        int minInternalNodeChilds = 15;
        int maxInternalNodeChilds = 15;
        Map<String, List<String>> proMap = getId2PropertyMap(propertyFile);


        int[] arr = new int[]{80,90,100,110,120,130,140,150,160};
        for (int i = 0; i < arr.length; i++) {
            System.out.println("编码长度：" + arr[i]);
            IndexTree indexTree = IndexTreeBuilder.build(hyperedgeIdFile, hyperedgeLabelFile, propertyFile,
                    windowSize, arr[i], hashFuncCount, minInternalNodeChilds,
                    maxInternalNodeChilds, secondaryIndexSize, false);
            query(indexTree, queryFile, proMap);

            System.out.println();
        }

    }


    // 按组别进行查询即可
    public static void query(IndexTree indexTree, String queryFile, Map<String, List<String>> proMap) {
        // 获取文件中的所有超边，并计算好编码
        BufferedReader bufferedReader;
        List<DataHyperedge> dataHyperedges = new ArrayList<>();

        try {
            bufferedReader = new BufferedReader(new FileReader(new File(queryFile)));
            String line;

            int encodingLength = indexTree.getEncodingLength();
            int hashFuncCount = indexTree.getHashFuncCount();

            while ((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\\t");
                long time = Long.parseLong(items[items.length - 1]);
                DataHyperedge hyperedge = new DataHyperedge(time, encodingLength);

                // 对该超边中包含的所有顶点的所有属性进行编码，合成超边编码
                PPBitset bitset = new PPBitset(encodingLength);
                for (int j = 1; j < items.length - 1; j++) {
                    String vertexId = items[j];
                    hyperedge.addVertexId(Long.valueOf(vertexId));
                    for (String prop : proMap.get(vertexId)) {
                        PPBitset tmp = PropertyEncodingConstructor.encoding(prop, encodingLength, hashFuncCount);
                        bitset.or(tmp);
                    }
                }
                hyperedge.setEncoding(bitset);
                dataHyperedges.add(hyperedge);
            }

            bufferedReader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < dataHyperedges.size() / NUM_EDGE_PER_GROUP; i++) {
            System.out.print("组别 " + i + " 的总候选数：");

            int start = i * NUM_EDGE_PER_GROUP;
            int end = (i + 1) * NUM_EDGE_PER_GROUP;
            int sum = 0;

            for (; start < end; start++) {
                sum += indexTree.singleEdgeSearch(dataHyperedges.get(start)).size();
            }
            System.out.println(sum);
        }
    }

    private static Map<String, List<String>> getId2PropertyMap(String propertyFile) {
        Map<String, List<String>> map = new HashMap<>();
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(propertyFile));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] items = line.split("\\t");
                map.putIfAbsent(items[0], new ArrayList<>());

                for (int i = 1; i < items.length; i++) {
                    map.get(items[0]).add(items[i]);
                }
            }

            reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return map;
    }
}
