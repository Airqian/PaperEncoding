package HGMatch.graph.util;

import java.util.*;

@Deprecated
public class Vec32<T> {
    private List<T> list;
    private Set<T> set;

    public Vec32() {
        this.list = new ArrayList<>();
        this.set = new HashSet<>();
    }

    public Vec32(Collection<T> collection) {
        this();
        for (T item : collection)
            add(item);
    }

    public boolean add(T element) {
        if (set.contains(element))
            return false;
        set.add(element);
        list.add(element);
        return true;
    }

    public boolean add(int index, T element) {
        if (set.contains(element))
            return false;
        set.add(element);
        list.add(index, element);
        return true;
    }

    public boolean remove(T element) {
        if (!set.contains(element))
            return false;
        set.remove(element);
        list.remove(element);
        return true;
    }

    public T get(int index) {
        return list.get(index);
    }

    public int size() {
        return list.size();
    }

    public void clear() {
        list.clear();
        set.clear();
    }

}
