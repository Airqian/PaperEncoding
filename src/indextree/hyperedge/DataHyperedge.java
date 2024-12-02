package indextree.hyperedge;


import java.util.ArrayList;
import java.util.List;

/**
 * 事件超边类：保存相应的编码以及时间
 */
public class DataHyperedge extends Hyperedge implements Comparable<DataHyperedge>{
    /**
     * 事件发生的时间（ms）
     */
    private long edgeTime;

    // 超边包含的顶点id
    private List<Long> vertexIds;

    // 根据数据集构建查询超边时会用到构造查询超边时会用到
    public DataHyperedge(long edgeTime, int encodingLength) {
        super(encodingLength);

        this.edgeTime = edgeTime;
        vertexIds = new ArrayList<>();
    }

    // 读取数据集构建树时会遇到
    public DataHyperedge(long id, long edgeTime, int encodingLength) {
        super(id, encodingLength);

        this.edgeTime = edgeTime;
        vertexIds = new ArrayList<>();
    }

    public void addVertexId(long vertexId) {
        this.vertexIds.add(vertexId);
    }

    // 计算与给定超边之间的权重增量
    public double getWeightIncrease(DataHyperedge dataHyperedge) {
        double diff = (this.getEncoding().cardinality() - dataHyperedge.cardinality()) * 1.0;
        return diff;
    }

    @Override
    public int compareTo(DataHyperedge o) {
        if (!this.getEncoding().equals(o.getEncoding()))
            return this.getEncoding().compareTo(o.getEncoding());
        else
            return Long.compare(this.edgeTime, o.edgeTime);
    }

    public long getEdgeTime() {
        return edgeTime;
    }

    public void setEdgeTime(long edgeTime) {
        this.edgeTime = edgeTime;
    }

    public List<Long> getVertexIds() {
        return vertexIds;
    }

    public void setVertexIds(List<Long> vertexIds) {
        this.vertexIds = vertexIds;
    }
}
