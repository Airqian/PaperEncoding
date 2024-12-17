package experiment.ex3;

import DataHandler.TempralGraphDataHandler.DataSetReader;
import DataHandler.TempralGraphDataHandler.IndexTreeBuilder;
import encoding.PPBitset;
import encoding.PropertyEncodingConstructor;
import indextree.IndexTree;
import indextree.hyperedge.DataHyperedge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class QueryTime {
    public static void main(String[] args) {
        Map<String, Integer> encodingLengthMap = new HashMap<>();
        encodingLengthMap.put("NDC-classes", 90);
        encodingLengthMap.put("NDC-substances", 100);
        encodingLengthMap.put("congress-bills", 120);
        encodingLengthMap.put("tags-ask-ubuntu", 90);
        encodingLengthMap.put("coauth-MAG-Geology", 83);
        encodingLengthMap.put("coauth-DBLP", 84);

        // 数据集信息
        String dataset = "NDC-classes";
        String queryFile = "src/experiment/ex3/queryFiles/" + dataset + "-100.txt";
        String srcSata = "src/dataset/temporal-restricted/";
        String hyperedgeIdFile = srcSata + dataset + "/hyperedge-id-unique.txt";
        String hyperedgeLabelFile = srcSata + dataset + "/hyperedge-label-unique.txt";
        String propertyFile = srcSata  + dataset + "/node-property3.txt";

        // 构建索引树数据集信息
        List<long[]> idToTime = DataSetReader.getAllEdgeTimeASC(hyperedgeIdFile); // 将超边按照时间升序排序
        Map<String, List<String>> proMap = DataSetReader.getId2PropertyMap(propertyFile); // 顶点到属性的映射
        Map<String, String> idMap = DataSetReader.getEdgeIdMap(hyperedgeIdFile); // 超边id到整条超边的映射（包含顶点id以及属性）
        Map<String, String> labelMap = DataSetReader.getEdgeLabelMap(hyperedgeLabelFile); // 超边标签到整条超边的映射（包含顶点label以及属性）

        // 索引树原信息
        int hashFuncCount = 2;
        int windowSize = 128;
        int secondaryIndexSize = 8;
        int minInternalNodeChilds = 15;
        int maxInternalNodeChilds = 15;
        boolean openSecondaryIndex = false;
        boolean ifParallel = true;

        IndexTree indexTree = IndexTreeBuilder.buildWithoutFileIOTime(idToTime, proMap, idMap, labelMap, windowSize, encodingLengthMap.get(dataset),
                hashFuncCount, minInternalNodeChilds, maxInternalNodeChilds, secondaryIndexSize, openSecondaryIndex);
        long start = System.currentTimeMillis();
        if (ifParallel)
            queryParallel(indexTree, queryFile, proMap);
        else
            query(indexTree, queryFile, proMap);
        long end = System.currentTimeMillis();
        System.out.println("检索时间 (ms): " + (end - start));

    }

    // 线程池优化查询
    public static void queryParallel(IndexTree indexTree, String queryFile, Map<String, List<String>> proMap) {
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

        // 使用线程池进行多线程处理
        int numProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(numProcessors);
        List<Future<Map<DataHyperedge, List<Long>>>> futures = new ArrayList<>();

        // 用于记录每个线程处理的超边数量
        AtomicInteger processedEdges = new AtomicInteger(0);

        // 为每条超边提交任务
        for (DataHyperedge hyperedge : dataHyperedges) {
            futures.add(executorService.submit(() -> {
                Map<DataHyperedge, List<Long>> result = new HashMap<>();
                List<Long> candidates = indexTree.singleEdgeSearch(hyperedge);
                result.put(hyperedge, candidates);

                // 更新每个线程处理的超边数
                processedEdges.incrementAndGet();
                return result;
            }));
        }

        // 获取并输出每条超边的候选超边结果
        for (Future<Map<DataHyperedge, List<Long>>> future : futures) {
            try {
                Map<DataHyperedge, List<Long>> result = future.get();
                result.forEach((hyperedge, candidates) -> {
                    System.out.println("超边 " + hyperedge + " 的候选超边数：" + candidates.size());
                });
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // 输出每个线程处理的超边数
        System.out.println("每个线程处理的超边数：" + processedEdges.get());

        // 关闭线程池
        executorService.shutdown();
    }

    // 无线程池优化查询
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

        for (int i = 0; i < dataHyperedges.size(); i++) {
            int res = indexTree.singleEdgeSearch(dataHyperedges.get(i)).size();
            System.out.println("超边一的候选集个数：" + res);
        }
    }
}
