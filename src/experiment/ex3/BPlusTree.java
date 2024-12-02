package experiment.ex3;

/**
 * B+树的定义：
 * 1.任意非叶子节点最多有M个子节点；且 M>2；M 为B+树的阶数
 * 2.除根节点以外的非叶子结点至少有 (M+1)/2个子节点；
 * 3.根节点至少有2个子节点；
 * 4.除根节点外每个节点存放至少（M-1）/2和至多M-1个关键字；（至少1个关键字）
 * 5.非叶子节点的子树指针比关键字多1个，最多 M-1 个关键字
 * 6.非叶子节点的所有key按升序存放，假设节点的关键字分别为K[0], K[1] … K[M-2],指向子女的指针分别为P[0], P[1]…P[M-1]。则有：
 *     P[0] < K[0] <= P[1] < K[1] …..< K[M-2] <= P[M-1]
 * 7.所有叶子节点位于同一层；
 * 8.为所有叶子节点增加一个链指针；
 * 9.所有关键字都在叶子节点出现
 */

/**
 * 对于超图b+树来说，需要先按编码比较再按编码时间进行比较
 * K 是编码和时间
 * V 是超边
 */
@SuppressWarnings("all")
public class BPlusTree<K extends Comparable<K>, V> {
    // 根节点
    protected BPlusNode<K, V> root;

    // 阶数，M值
    protected int order;

    // 叶子节点的链表头
    protected BPlusNode<K, V> head;

    // 树高
    protected int height = 0;

    public BPlusNode<K, V> getHead() {
        return head;
    }

    public void setHead(BPlusNode<K, V> head) {
        this.head = head;
    }

    public BPlusNode<K, V> getRoot() {
        return root;
    }

    public void setRoot(BPlusNode<K, V> root) {
        this.root = root;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public V get(K key) {
        return root.get(key);
    }

    public V remove(K key) {
        return root.remove(key, this);
    }

    public void insertOrUpdate(K key, V value) {
        root.insertOrUpdate(key, value, this);
    }

    public BPlusTree(int order) {
        if (order < 3) {
            System.out.print("order must be greater than 2");
            System.exit(0);
        }
        this.order = order;
        root = new BPlusNode<K, V>(true, true);
        head = root;
    }

    public void printBPlusTree() {
        this.root.printBPlusTree(0);
    }
}