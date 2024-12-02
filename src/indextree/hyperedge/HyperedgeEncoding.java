package indextree.hyperedge;

import encoding.PPBitset;

import java.util.ArrayList;
import java.util.List;


/**
 * 这个类保存事件超边的编码，并封装了 PPBitset 类的位操作。
 * 编码的顺序必须严格与属性的顺序对齐。
 */
@Deprecated
public class HyperedgeEncoding {
    /**
     * 预先在此处保存一份，其是下划线形式，如"propertyNameA_propertyNameB_propertyNameC"
     * 查询编码的时候要指定propertyName
     */
//    private String vertexType;

    private long hyperedgeId;

    private List<PPBitset> propertyBitsets;

    public HyperedgeEncoding(long hyperedgeId) {
        this.hyperedgeId = hyperedgeId;
        propertyBitsets = new ArrayList<>();
    }

    public void addEncoding(PPBitset bitset) {
        propertyBitsets.add(bitset);
    }

    public PPBitset andOperation(int index, PPBitset another) {
        PPBitset bitset = propertyBitsets.get(index);
        return bitset.and(another);
    }

    public int cardinality(int index) {
        return propertyBitsets.get(index).cardinality();
    }

    public int size() {
        return propertyBitsets.size();
    }

    public PPBitset getProperty(int index) {
        return propertyBitsets.get(index);
    }

    public List<PPBitset> getPropertyBitsets() {
        return propertyBitsets;
    }

    public void setHyperedgeId(long id) {
        this.hyperedgeId = id;
    }

    public long getHyperedgeId() {
        return hyperedgeId;
    }

    public String printEncoding() {
        StringBuilder builder = new StringBuilder();
        for (PPBitset ppBitset : propertyBitsets) {
            builder.append(ppBitset.toString());
            builder.append(" ");
        }
        return builder.toString();
    }
}