package zHGMatch.extend;

import zHGMatch.graph.EdgePartition;
import zHGMatch.graph.PartitionedEdges;
import zHGMatch.graph.QueryGraph;
import zHGMatch.graph.util.Pair;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MatchDriver {
    private static final Logger logger = Logger.getLogger(MatchDriver.class.getName());
    private final static int MAX_NUM_NODE_AUTOMORPHISM = 10;

    private String name;
    private QueryGraph query;
    private List<ExecutionPlan> plan;
    private boolean print_results;
    private int automorphism; // 对应rust中option，MIN_VALUE 为 null

    public MatchDriver(String name, QueryGraph query, List<ExecutionPlan> plan, boolean printResults) {
        this.name = name;
        this.query = query;
        this.plan = plan;
        this.print_results = printResults;

        if (query.num_nodes() <= MAX_NUM_NODE_AUTOMORPHISM) {
            System.out.println("Computing number of automorphism");
            int num = 0;
            // TODO 超图同构啊啊啊
            // int num = query.to_graph().canonicalForm().automorphisms().count();
            System.out.println(num + " automorphism");
            this.automorphism = num;
        } else {
            System.out.println("Query is too large, computing automorphism skipped");
            this.automorphism = Integer.MAX_VALUE;
        }
    }

    /**
     * 获取自 Unix 纪元以来的当前时间，单位为微秒。
     *
     * @return 当前时间的微秒数
     */
    public static long getCurrentTimeMicros() {
        Instant now = Instant.now();
        long epochSecond = now.getEpochSecond(); // 自 Unix 纪元以来的秒数
        int nano = now.getNano(); // 当前秒内的纳秒数
        return epochSecond * 1_000_000 + nano / 1_000; // 转换为微秒
    }

    public static void main(String[] args) {
        long startMicros = getCurrentTimeMicros();

        // 执行需要测量的操作
        for (int i = 0; i < 251; i++) {
            System.out.print(i);
        }

        long endMicros = getCurrentTimeMicros();
        long durationMicros = endMicros - startMicros;

        System.out.println("\n操作耗时: " + durationMicros + " 微秒");
    }

    // limit 对应rust中option，MIN_VALUE 为 null
    public Pair<Integer, Long> run(PartitionedEdges graph, int limit) {
        System.out.println(this.name + " - Running query");
        int total_count = 0;
        long total_time = 0l;

        for (int i = 0; i < this.plan.size(); i++) {
            System.out.println("Plan " + i);
            ExecutionPlan current_plan = this.plan.get(i);
            // 取出的是数据图中 与匹配顺序第一条边的标签相同的边集合
            EdgePartition edge_index = graph.get_partition(current_plan.getStartLabels());

            if (edge_index == null) {
                System.out.println(this.name + " - First hyperedge not found, 0 embeddings");
                return new Pair<>(0, 0l);
            }

            if (current_plan.getExtenders().size() == 0) {
                int count = edge_index.num_edges();
                System.out.println("'" + this.name + "' - Query one hyperedge: " + count + " embeddings");
                return new Pair<>(count, 0l);
            }

            int extendersSize = current_plan.getExtenders().size();
            Pair<Integer, Long> countAndTime;

            switch (extendersSize) {
                case 1:
                    countAndTime = run1(graph, limit, current_plan);
                    break;

                case 2:
                    countAndTime = run2(graph, limit, current_plan);
                    break;
                case 3:
                    countAndTime = run3(graph, limit, current_plan);
                    break;
                case 4:
                    countAndTime = run4(graph, limit, current_plan);
                    break;
                case 5:
                    countAndTime = run5(graph, limit, current_plan);
                    break;
                default:
                    System.out.println("Work stealing not enabled");
                    countAndTime = run_any(graph, limit, current_plan);
                    break;
            }

            total_count += countAndTime.getKey();
            total_time += countAndTime.getValue();
        }

        return new Pair<>(total_count, total_time);
    }

    public Pair<Integer, Long> run_any(PartitionedEdges graph, int limit, ExecutionPlan plan) {
        long start = System.currentTimeMillis();

        // 获取初始边的分区
        EdgePartition edge_index = graph.get_partition(plan.getStartLabels());
        System.out.println("Starting with "+ edge_index.num_edges() + " hyperedges");

        // 获取当前边分区中的所有边以及第一个扩展器
        List<Integer> edges = edge_index.getEdges();
        Extender first_ext = plan.getExtenders().get(0);

        // 定义枚举函数
        java.util.function.Function<List<Integer>, Integer> enum_func = edge -> {
            List<List<Integer>> iter = first_ext.extend(edge, graph);

            // 应用后续的扩展器
            for (int i = 1; i < plan.getExtenders().size(); i++) {
                Extender extender = plan.getExtenders().get(i);
                List<List<Integer>> newIter = new ArrayList<>();
                for (List<Integer> partial : iter)
                    newIter.addAll(extender.extend(partial, graph));
                iter = newIter;
            }

            if (print_results) {
                for (List<Integer> partial : iter) {
                    System.out.println(split_results(partial, plan.getResultArity()));
                }
            }

            return iter.size();
        };


        // 对每条边利用 enum_func 函数进行处理
        List<Integer> dataflow = new ArrayList<>();
        for (int i = 0; i < edge_index.num_edges(); i++) {
            List<Integer> edge = edge_index.getEdge(i);
            int apply = enum_func.apply(edge);
            dataflow.add(apply);
        }

        // TODO 目前实现的是一个单线程的版本
        // 下面这段代码是计算 dataflow 中所有元素的总和 count
        // 如果提供了 limit，则累加器中的元素直到总和达到limit
        // 如果没有提供limit，则计算所有元素的总和
        int sum = 0;
        if (limit != Integer.MIN_VALUE) { // 如果提供了 limit，创建一个原子计数器来跟踪当前累加的总和
            AtomicInteger counter = new AtomicInteger(0);

            for (int i = 0; i < dataflow.size(); i++) {
                int oldValue = counter.getAndAdd(dataflow.get(i));
                // 如果counter没有超过limit就加上当前的元素
                if (oldValue < limit) {
                    sum += dataflow.get(i);
                } else {
                    break;
                }
            }
        } else { // 如果没有 limit，直接计算所有元素的和
            sum = dataflow.stream().mapToInt(Integer::intValue).sum();
        }

        long end = System.currentTimeMillis();

        System.out.println(name + " - Computed in " + (end - start) + " ms, " + sum + " embeddings");

        if (this.automorphism != Integer.MIN_VALUE) { // 这里也是模拟Rust中的option，判断automorphism是不是空值
            System.out.println("run_any " + name + " - " + (sum * this.automorphism) +
                    " embeddings with symmetry (assuming no edge automorphism)\n");
        }

        // 返回的时间是毫秒
        return new Pair<>(sum, (end - start));
    }


    public Pair<Integer, Long> run1(PartitionedEdges graph, int limit, ExecutionPlan plan) {
        long start = System.currentTimeMillis();

        // 获取初始边的分区
        EdgePartition edge_index = graph.get_partition(plan.getStartLabels());
        System.out.println("Starting with "+ edge_index.num_edges() + " hyperedges");

        List<List<Integer>> edges = new ArrayList<>();
        for (int i = 0; i < edge_index.num_edges(); i++) {
            List<Integer> edge = edge_index.getEdge(i);
            edges.add(edge);
        }

        // 并行处理分块
        List<Integer> dataflow = edges.parallelStream()
                .map(partial -> {
                    List<List<Integer>> results = plan.getExtenders().get(0).extend(partial, graph);
                    if (this.print_results) {
                        for (List<Integer> result : results) {
                            System.out.println(split_results(result, plan.getResultArity()));
                        }
                    }
                    return results.size();
                })
                .collect(Collectors.toList());

        // 下面这段代码是计算 dataflow 中所有元素的总和 count
        // 如果提供了 limit，则累加器中的元素直到总和达到limit
        // 如果没有提供limit，则计算所有元素的总和
        int sum = 0;
        if (limit != Integer.MIN_VALUE) { // 如果提供了 limit，创建一个原子计数器来跟踪当前累加的总和
            AtomicInteger counter = new AtomicInteger(0);

            for (int i = 0; i < dataflow.size(); i++) {
                int oldValue = counter.getAndAdd(dataflow.get(i));
                // 如果counter没有超过limit就加上当前的元素
                if (oldValue < limit) {
                    sum += dataflow.get(i);
                } else {
                    break;
                }
            }
        } else { // 如果没有 limit，直接计算所有元素的和
            sum = dataflow.stream().mapToInt(Integer::intValue).sum();
        }

        long end = System.currentTimeMillis();

        System.out.println(name + " - Computed in " + (end - start) + " ms, " + sum + " embeddings");

        if (this.automorphism != Integer.MIN_VALUE) { // 这里也是模拟Rust中的option，判断automorphism是不是空值
            System.out.println("run1 " + name + " - " + (sum * this.automorphism) +
                    " embeddings with symmetry (assuming no edge automorphism)\n");
        }

        // 返回的时间是毫秒
        return new Pair<>(sum, (end - start));
    }

    public Pair<Integer, Long> run2(PartitionedEdges graph, int limit, ExecutionPlan plan) {
        long start = System.currentTimeMillis();

        // 获取初始边的分区
        EdgePartition edge_index = graph.get_partition(plan.getStartLabels());
        System.out.println("Starting with "+ edge_index.num_edges() + " hyperedges");

        List<List<Integer>> edges = new ArrayList<>();
        for (int i = 0; i < edge_index.num_edges(); i++) {
            List<Integer> edge = edge_index.getEdge(i);
            edges.add(edge);
        }

        // 并行处理分块
        List<Integer> dataflow = edges.parallelStream()
                // 第一次扩展
                .flatMap(partial -> {
                    List<List<Integer>> extend = plan.getExtenders().get(0).extend(partial, graph);
                    return extend.stream();
                })
                // 第二次扩展和计数
                .map(partial -> {
                    List<List<Integer>> results = plan.getExtenders().get(1).extend(partial, graph);
                    if (this.print_results) {
                        for (List<Integer> result : results) {
                            System.out.println(split_results(result, plan.getResultArity()));
                        }
                    }
                    return results.size();
                })
                .collect(Collectors.toList());

        // 下面这段代码是计算 dataflow 中所有元素的总和 count
        // 如果提供了 limit，则累加器中的元素直到总和达到limit
        // 如果没有提供limit，则计算所有元素的总和
        int sum = 0;
        if (limit != Integer.MIN_VALUE) { // 如果提供了 limit，创建一个原子计数器来跟踪当前累加的总和
            AtomicInteger counter = new AtomicInteger(0);

            for (int i = 0; i < dataflow.size(); i++) {
                int oldValue = counter.getAndAdd(dataflow.get(i));
                // 如果counter没有超过limit就加上当前的元素
                if (oldValue < limit) {
                    sum += dataflow.get(i);
                } else {
                    break;
                }
            }
        } else { // 如果没有 limit，直接计算所有元素的和
            sum = dataflow.stream().mapToInt(Integer::intValue).sum();
        }

        long end = System.currentTimeMillis();

        System.out.println(name + " - Computed in " + (end - start) + " ms, " + sum + " embeddings");

        if (this.automorphism != Integer.MIN_VALUE) { // 这里也是模拟Rust中的option，判断automorphism是不是空值
            System.out.println("run2 " + name + " - " + (sum * this.automorphism) +
                    " embeddings with symmetry (assuming no edge automorphism)\n");
        }

        // 返回的时间是毫秒
        return new Pair<>(sum, (end - start));
    }

    public Pair<Integer, Long> run3(PartitionedEdges graph, int limit, ExecutionPlan plan) {
        long start = System.currentTimeMillis();

        // 获取初始边的分区
        EdgePartition edge_index = graph.get_partition(plan.getStartLabels());
        System.out.println("Starting with "+ edge_index.num_edges() + " hyperedges");

        List<List<Integer>> edges = new ArrayList<>();
        for (int i = 0; i < edge_index.num_edges(); i++) {
            List<Integer> edge = edge_index.getEdge(i);
            edges.add(edge);
        }

        Comparator<List<Integer>> lexComparator = (list1, list2) -> {
            if (list1 == null && list2 == null) return 0;
            if (list1 == null) return -1;
            if (list2 == null) return 1;

            int size1 = list1.size();
            int size2 = list2.size();
            int minSize = Math.min(size1, size2);

            for (int i = 0; i < minSize; i++) {
                Integer elem1 = list1.get(i);
                Integer elem2 = list2.get(i);

                if (elem1 == null && elem2 == null) continue;
                if (elem1 == null) return -1;
                if (elem2 == null) return 1;

                int cmp = elem1.compareTo(elem2);
                if (cmp != 0) {
                    return cmp;
                }
            }
            // 如果前 minSize 个元素相同，较短的列表排前面
            return Integer.compare(size1, size2);
        };
        edges.sort(lexComparator);


        // 并行处理分块
//        List<Integer> dataflow = edges.stream()
//                // 第一次扩展
//                .flatMap(partial -> {
//                    List<List<Integer>> extend = plan.getExtenders().get(0).extend(partial, graph);
//                    return extend.stream();
//                })
//                .flatMap(partial -> {
//                    List<List<Integer>> extend = plan.getExtenders().get(1).extend(partial, graph);
//                    return extend.stream();
//                })
//                // 第二次扩展和计数
//                .map(partial -> {
//                    List<List<Integer>> results = plan.getExtenders().get(2).extend(partial, graph);
//                    System.out.println("results.size(): " + results.size());
//                    if (this.print_results) {
//                        for (List<Integer> result : results) {
//                            System.out.println(split_results(result, plan.getResultArity()));
//                        }
//                    }
//                    return results.size();
//                })
//                .collect(Collectors.toList());

        List<Integer> dataflow = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            List<Integer> edge = edges.get(i);
            // 第一层扩展
            List<List<Integer>> firstExtended = plan.getExtenders().get(0).extend(edge, graph);

            // 第二层扩展
            List<List<Integer>> secondExtended = new ArrayList<>();
            for (List<Integer> p : firstExtended) {
                List<List<Integer>> extended = plan.getExtenders().get(1).extend(p, graph);
                secondExtended.addAll(extended);
            }

            for (List<Integer> p : secondExtended) {
                List<List<Integer>> results = plan.getExtenders().get(2).extend(p, graph);
                System.out.println("results.size(): " + results.size());
                if (this.print_results) {
                    for (List<Integer> result : results) {
                        System.out.println(split_results(result, plan.getResultArity()));
                    }
                }
                dataflow.add(results.size());
            }
        }


        // 下面这段代码是计算 dataflow 中所有元素的总和 count
        // 如果提供了 limit，则累加器中的元素直到总和达到limit
        // 如果没有提供limit，则计算所有元素的总和
        int sum = 0;
        if (limit != Integer.MIN_VALUE) { // 如果提供了 limit，创建一个原子计数器来跟踪当前累加的总和
            AtomicInteger counter = new AtomicInteger(0);

            for (int i = 0; i < dataflow.size(); i++) {
                int oldValue = counter.getAndAdd(dataflow.get(i));
                // 如果counter没有超过limit就加上当前的元素
                if (oldValue < limit) {
                    sum += dataflow.get(i);
                } else {
                    break;
                }
            }
        } else { // 如果没有 limit，直接计算所有元素的和
            sum = dataflow.stream().mapToInt(Integer::intValue).sum();
        }

        long end = System.currentTimeMillis();

        System.out.println(name + " - Computed in " + (end - start) + " ms, " + sum + " embeddings");

        if (this.automorphism != Integer.MIN_VALUE) { // 这里也是模拟Rust中的option，判断automorphism是不是空值
            System.out.println("run3 " + name + " - " + (sum * this.automorphism) +
                    " embeddings with symmetry (assuming no edge automorphism)\n");
        }

        // 返回的时间是毫秒
        return new Pair<>(sum, (end - start));

    }

    public Pair<Integer, Long> run4(PartitionedEdges graph, int limit, ExecutionPlan plan) {
        long start = System.currentTimeMillis();

        // 获取初始边的分区
        EdgePartition edge_index = graph.get_partition(plan.getStartLabels());
        System.out.println("Starting with "+ edge_index.num_edges() + " hyperedges");

        List<List<Integer>> edges = new ArrayList<>();
        for (int i = 0; i < edge_index.num_edges(); i++) {
            List<Integer> edge = edge_index.getEdge(i);
            edges.add(edge);
        }

        // 并行处理分块
        List<Integer> dataflow = edges.parallelStream()
                // 第一次扩展
                .flatMap(partial -> {
                    List<List<Integer>> extend = plan.getExtenders().get(0).extend(partial, graph);
                    return extend.stream();
                })
                .flatMap(partial -> {
                    List<List<Integer>> extend = plan.getExtenders().get(1).extend(partial, graph);
                    return extend.stream();
                })
                .flatMap(partial -> {
                    List<List<Integer>> extend = plan.getExtenders().get(2).extend(partial, graph);
                    return extend.stream();
                })
                // 第二次扩展和计数
                .map(partial -> {
                    List<List<Integer>> results = plan.getExtenders().get(3).extend(partial, graph);
                    if (this.print_results) {
                        for (List<Integer> result : results) {
                            System.out.println(split_results(result, plan.getResultArity()));
                        }
                    }
                    return results.size();
                })
                .collect(Collectors.toList());

        // 下面这段代码是计算 dataflow 中所有元素的总和 count
        // 如果提供了 limit，则累加器中的元素直到总和达到limit
        // 如果没有提供limit，则计算所有元素的总和
        int sum = 0;
        if (limit != Integer.MIN_VALUE) { // 如果提供了 limit，创建一个原子计数器来跟踪当前累加的总和
            AtomicInteger counter = new AtomicInteger(0);

            for (int i = 0; i < dataflow.size(); i++) {
                int oldValue = counter.getAndAdd(dataflow.get(i));
                // 如果counter没有超过limit就加上当前的元素
                if (oldValue < limit) {
                    sum += dataflow.get(i);
                } else {
                    break;
                }
            }
        } else { // 如果没有 limit，直接计算所有元素的和
            sum = dataflow.stream().mapToInt(Integer::intValue).sum();
        }

        long end = System.currentTimeMillis();

        System.out.println(name + " - Computed in " + (end - start) + " ms, " + sum + " embeddings");

        if (this.automorphism != Integer.MIN_VALUE) { // 这里也是模拟Rust中的option，判断automorphism是不是空值
            System.out.println("run4 " + name + " - " + (sum * this.automorphism) +
                    " embeddings with symmetry (assuming no edge automorphism)\n");
        }

        // 返回的时间是毫秒
        return new Pair<>(sum, (end - start));
    }

    public Pair<Integer, Long> run5(PartitionedEdges graph, int limit, ExecutionPlan plan) {
        long start = System.currentTimeMillis();

        // 获取初始边的分区
        EdgePartition edge_index = graph.get_partition(plan.getStartLabels());
        System.out.println("Starting with "+ edge_index.num_edges() + " hyperedges");

        List<List<Integer>> edges = new ArrayList<>();
        for (int i = 0; i < edge_index.num_edges(); i++) {
            List<Integer> edge = edge_index.getEdge(i);
            edges.add(edge);
        }

        // 并行处理分块
        List<Integer> dataflow = edges.parallelStream()
                // 第一次扩展
                .flatMap(partial -> {
                    List<List<Integer>> extend = plan.getExtenders().get(0).extend(partial, graph);
                    return extend.stream();
                })
                .flatMap(partial -> {
                    List<List<Integer>> extend = plan.getExtenders().get(1).extend(partial, graph);
                    return extend.stream();
                })
                .flatMap(partial -> {
                    List<List<Integer>> extend = plan.getExtenders().get(2).extend(partial, graph);
                    return extend.stream();
                })
                .flatMap(partial -> {
                    List<List<Integer>> extend = plan.getExtenders().get(3).extend(partial, graph);
                    return extend.stream();
                })
                // 第二次扩展和计数
                .map(partial -> {
                    List<List<Integer>> results = plan.getExtenders().get(4).extend(partial, graph);
                    if (this.print_results) {
                        for (List<Integer> result : results) {
                            System.out.println(split_results(result, plan.getResultArity()));
                        }
                    }
                    return results.size();
                })
                .collect(Collectors.toList());

        // 下面这段代码是计算 dataflow 中所有元素的总和 count
        // 如果提供了 limit，则累加器中的元素直到总和达到limit
        // 如果没有提供limit，则计算所有元素的总和
        int sum = 0;
        if (limit != Integer.MIN_VALUE) { // 如果提供了 limit，创建一个原子计数器来跟踪当前累加的总和
            AtomicInteger counter = new AtomicInteger(0);

            for (int i = 0; i < dataflow.size(); i++) {
                int oldValue = counter.getAndAdd(dataflow.get(i));
                // 如果counter没有超过limit就加上当前的元素
                if (oldValue < limit) {
                    sum += dataflow.get(i);
                } else {
                    break;
                }
            }
        } else { // 如果没有 limit，直接计算所有元素的和
            sum = dataflow.stream().mapToInt(Integer::intValue).sum();
        }

        long end = System.currentTimeMillis();

        System.out.println(name + " - Computed in " + (end - start) + " ms, " + sum + " embeddings");

        if (this.automorphism != Integer.MIN_VALUE) { // 这里也是模拟Rust中的option，判断automorphism是不是空值
            System.out.println("run5 " + name + " - " + (sum * this.automorphism) +
                    " embeddings with symmetry (assuming no edge automorphism)\n");
        }

        // 返回的时间是毫秒
        return new Pair<>(sum, (end - start));
    }



    /**
     * 拆分结果，基于结果的 arity。
     *
     * @param results      部分嵌入
     * @param arity        结果的 arity 列表
     * @param <T>          泛型类型
     * @return 拆分后的结果列表
     */
    private <T> List<List<T>> split_results(List<T> results, List<Integer> arity) {
        List<List<T>> splitResults = new ArrayList<>(arity.size());
        int cur = 0;
        for (int a : arity) {
            if (cur + a > results.size()) {
                throw new IllegalArgumentException("Arity exceeds the size of the results.");
            }
            List<T> part = results.subList(cur, cur + a);
            splitResults.add(new ArrayList<>(part)); // 创建一个新的列表以避免对原列表的依赖
            cur += a;
        }
        return splitResults;
    }
}
