package experiment.ex2;

public class False_drop {
    public static void main(String[] args) {
        int m = 2; // hash函数的个数
        int M = 120; // 属性编码长度
        int n = 6;   // 单个顶点的平均属性个数
        int qv = 10; // 查询编码中 1 的个数
        int dv = 13; // 数据编码中 1 的个数
        double k = m * dv;

        double false_drop = Math.pow(1 - Math.exp(-1.0 * n * m * qv / M), k);
        System.out.println(String.format("%.9f", false_drop) );
    }
}
