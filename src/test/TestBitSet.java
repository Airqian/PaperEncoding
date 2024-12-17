package test;

import encoding.PPBitset;

public class TestBitSet {
    public static void main(String[] args) {
        PPBitset bitset1 = new PPBitset(30);
        PPBitset bitset2 = new PPBitset(30);
        bitset1.set(20);
        bitset1.set(2);
        bitset1.set(13);
        bitset1.set(4);
        bitset2.set(21);
        System.out.println(bitset1.compareTo(bitset2));
        System.out.println(bitset1);
    }
}
