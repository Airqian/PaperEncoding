package HGMatch.graph.util;

import HGMatch.graph.StaticHyperGraph;

import java.util.List;
import java.util.Optional;

// 定义一个接口来模拟Rust中的Canonize特征，接口中的方法对应原Rust代码中实现的那些方法
// TODO 还差一个 canonizal 方法，该方法返回一个超图的规范形式，用于判断两个超图是否同构
interface Canonize {
    int size();

    StaticHyperGraph applyMorphism(int[] p);

    Optional<List<Integer>> invariantColoring();

    List<List<Integer>> invariantNeighborhood(int u);
}
