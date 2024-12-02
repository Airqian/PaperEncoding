package indextree.hyperedge;

import encoding.PPBitset;
import indextree.util.IdGenerator;

import java.util.List;

public class Hyperedge {
    // 数据超边的id从数据集中获取，派生超边的id需要生成
    private long id;

    private int encodingLength;

    private PPBitset encoding;

    public Hyperedge(){}

    // 此构造方法用于Hyperedge#clone()方法、topHyperedge的initial方法以及构建查询超边
    public Hyperedge(int encodingLength) {
        this.id = IdGenerator.getNextId();
        this.encodingLength = encodingLength;
        this.encoding = new PPBitset(encodingLength);
    }

    // 读取数据集构建超边时会用到
    public Hyperedge(long id, int encodingLength) {
        this.id = id;
        this.encodingLength = encodingLength;
        this.encoding = new PPBitset(encodingLength);
    }

    // 将编码中所有的1全部置为0
    public void clear() {
        for (int i = 0; i < encoding.size(); i++)
            encoding.clear();
    }

    public Hyperedge clone() {
        Hyperedge hyperedge = new Hyperedge(encodingLength);
        PPBitset ppBitset = (PPBitset) encoding.clone();
        hyperedge.setEncoding(ppBitset);

        return hyperedge;
    }

    /**
     * 两条超边之间的属性编码OR操作，在构建索引树阶段同一个子节点中的所有超边向上汇聚成父节点时会用到（叶节点和非叶节点都会用到）
     * @param hyperedge 新插入的超边
     */
    public void encodingOr(Hyperedge hyperedge) {
        List<Integer> bits = hyperedge.getEncoding().getAllOneBits();
        for (int bit : bits) {
            this.getEncoding().set(bit, true);
        }
    }

    /**
     * 两条超边进行与操作，在索引树中进行自上而下的超边匹配会将用到，测试 hyperedge & ano_hyperedge == hyperedge
     * @param hyperedge
     * @return
     */
    public boolean isBitwiseSubset(Hyperedge hyperedge) {
        return encoding.isBitwiseSubset(hyperedge.encoding);
    }



    public int cardinality() {
        return encoding.cardinality();
    }

    // ---------------------------- Getter 和 Setter ----------------------------
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public PPBitset getEncoding() {
        return encoding;
    }

    public void setEncoding(PPBitset encoding) {
        this.encoding = encoding;
    }

    public String printEncoding() {
        return encoding.toString();
    }

    public void setEncodingLength(int encodingLength) {
        this.encodingLength = encodingLength;
    }

    public int getEncodingLength() {
        return encodingLength;
    }

}
