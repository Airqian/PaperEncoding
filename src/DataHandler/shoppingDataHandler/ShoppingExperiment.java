package DataHandler.shoppingDataHandler;

import  encoding.PPBitset;
import  encoding.PropertyEncodingConstructor;
import  encoding.util.PeriodType;
import  indextree.IndexTree;
import  indextree.hyperedge.DataHyperedge;
import  indextree.util.DataSetInfo;
import  indextree.util.ShoppingDataAnalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ShoppingExperiment {
    public static void main(String[] args) {
        IndexTree indexTree = buildShoppingTree(FilePathConstants.SHOPPIONG_EVENT_EXPERIMENT_FILE_PATH, PeriodType.MONTH);
        query(indexTree, FilePathConstants.SHOPPING_QUERY_FILE_PATH);
    }

    public static IndexTree buildShoppingTree(String dataFile, PeriodType type) {
        // 读取数据集，数据集格式固定第一项是超边ID
        DataSetInfo dataSetInfo = ShoppingDataAnalyzer.readShoppingFile(dataFile, type);

        // 构建索引树
        int windowSize = 10;
        int encodingLength = 100; // 统一的编码长度
        int hashFuncCount = 3;

        int minInternalNodeChilds = 10;
        int maxInternalNodeChilds = 15;
        int secondaryIndexSize = 16;

        IndexTree indexTree = new IndexTree(windowSize, encodingLength, hashFuncCount, minInternalNodeChilds, maxInternalNodeChilds, secondaryIndexSize);
        indexTree.buildShoppingTree(dataSetInfo);
        return indexTree;
    }

    public static void query(IndexTree indexTree, String query_file) {
        // 给超边对应上主体属性的id
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(new File(query_file)));
            String line;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            int encodingLength = indexTree.getEncodingLength();
            int hashFuncCount = indexTree.getHashFuncCount();

            while ((line = bufferedReader.readLine()) != null) {
                String[] items = line.split("\\t");
                Date date  = format.parse(items[items.length - 1]);
                long time = date.getTime();
                DataHyperedge hyperedge = new DataHyperedge(time, encodingLength);

                PPBitset totalBitSet = new PPBitset(encodingLength);
                for (int i = 1, j = 0; i < items.length - 1; i++, j++) {
                    PPBitset ppBitset = PropertyEncodingConstructor.encoding(items[i], encodingLength, hashFuncCount);
                    totalBitSet.or(ppBitset);
                }
                hyperedge.setEncoding(totalBitSet);

                List<Long> ids = indexTree.singleEdgeSearchWithSecondaryIndex(hyperedge);
                for (Long id : ids)
                    System.out.println(id);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
