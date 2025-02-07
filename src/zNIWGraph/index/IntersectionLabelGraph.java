package zNIWGraph.index;

import zNIWGraph.graph.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 实现一个二元邻居交叉权重图，其中顶点为超边id，图中的边为两条超边公共顶点标签到数量的映射列表，数据图和查询图都可以构建二元邻居交叉权重图
 *
 * · 匹配顺序：选择查询图中公共顶点数量最少的超边最先开始匹配，在公共顶点数量相同的情况下，选择不同标签数更多的超边最先匹配
 * · 候选集生成：公共顶点信息已经预先计算出来，对于第一条匹配超边也可以根据权重图提前查看邻边信息，过滤掉一部分候选超边
 */
// 需要建立超边id->超边、超边->超边id的 map
public class IntersectionLabelGraph {
    private Map<Integer, Map<Integer, Integer>> vertexNumWGraph; // id -> <id, vertexNum>

    private Map<Integer, Map<Integer, List<Pair>>> labelWGraph; // id -> <id, <label, num>...>

    public IntersectionLabelGraph() {
        vertexNumWGraph = new HashMap<>();
        labelWGraph = new HashMap<>();
    }

    public IntersectionLabelGraph(Map<Integer, Map<Integer, Integer>> vertexNumWGraph, Map<Integer, Map<Integer, List<Pair>>> labelWGraph) {
        this.vertexNumWGraph = vertexNumWGraph;
        this.labelWGraph = labelWGraph;
    }

    public Map<Integer, Map<Integer, Integer>> getVertexNumWGraph() {
        return this.vertexNumWGraph;
    }

    public Map<Integer, Map<Integer, List<Pair>>> getLabelWGraph() {
        return this.labelWGraph;
    }


    public boolean isNeighbor(int queryEdgeId, int neighborEdgeId) {
        return vertexNumWGraph.get(queryEdgeId).containsKey(neighborEdgeId);
    }


}
