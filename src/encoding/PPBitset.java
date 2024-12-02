package encoding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * 属性编码位串类
 */
public class PPBitset extends BitSet implements Comparable<PPBitset> {
    private final static int ADDRESS_BITS_PER_WORD = 5;
    private final static int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;

    /* Used to shift left or right for a partial word mask */
    private static final int WORD_MASK = 0xffffffff;

    private int[] words;

    /* 表示该位串可使用位的最大数量，一经初始化便无法修改 */
    private int maxBitsInUse = 0;

    /* 表示words数组的长度，一经初始化便无法修改 */
    private int wordsInUse = 0;

    public PPBitset(int maxBitsInUse) {
        if (maxBitsInUse < 0)
            throw new NegativeArraySizeException("bitsInUse < 0: " + maxBitsInUse);

        initWords(maxBitsInUse);
        this.maxBitsInUse = maxBitsInUse;
        this.wordsInUse = this.words.length;
    }

    // nbits=32
    private void initWords(int nbits) {
        words = new int[wordIndex(nbits-1) + 1];
    }

    /**
     * Given a bit index, return word index containing it.
     */
    private static int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }

    public int length() {
        return this.maxBitsInUse;
    }

    public boolean isEmpty() {
        return this.maxBitsInUse == 0;
    }

    public int[] toIntArray() {
        return Arrays.copyOf(words, wordsInUse);
    }

    public void set(int bitIndex, boolean value) {
        if (value)
            set(bitIndex);
        else
            clear(bitIndex);
    }

    public void set(int bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        if (bitIndex >= maxBitsInUse)
            throw new IndexOutOfBoundsException("bitIndex exceeds the valid range of bits for the bitset: " +
                    bitIndex + " >= " + this.length());

        int wordIndex = wordIndex(bitIndex);
        words[wordIndex] |= (1 << bitIndex);
    }

    public void clear(int bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        if (bitIndex >= maxBitsInUse)
            throw new IndexOutOfBoundsException("bitIndex exceeds the valid range of bits for the bitset: " + bitIndex + " >= " + this.length());

        int wordIndex = wordIndex(bitIndex);
        words[wordIndex] &= ~(1 << bitIndex);
    }

    public void clear() {
        int bit = nextSetBit(0);
        while (bit != -1) {
            set(bit, false);

            bit = nextSetBit(bit);
        }
    }


    public boolean get(int bitIndex) {
        if (bitIndex < 0)
            throw new IndexOutOfBoundsException("bitIndex < 0: " + bitIndex);

        if (bitIndex >= maxBitsInUse)
            throw new IndexOutOfBoundsException("bitIndex exceeds the valid range of bits for the bitset: " + bitIndex + " >= " + this.length());

        int wordIndex = wordIndex(bitIndex);
        return (words[wordIndex] & (1 << bitIndex)) != 0;
    }

    public List<Integer> getAllOneBits() {
        List<Integer> bits = new ArrayList<>();

        int i = nextSetBit(0);
        if (i != -1) {
            bits.add(i);
            while (true) {
                if (++i < 0) break;
                if ((i = nextSetBit(i)) < 0) break;
                int endOfRun = nextClearBit(i);
                do { bits.add(i); }
                while (++i != endOfRun);
            }
        }

        return bits;
    }

    /**
     * Returns the number of bits set to {@code true} in this {@code PPBitSet}.
     *
     * @return Hamming distance
     */
    public int cardinality() {
        int sum = 0;
        for (int i = 0; i < wordsInUse; i++)
            sum += Integer.bitCount(words[i]);

        return sum;
    }

    /**
     * Performs a logical <b>AND</b> of this bit set with the bit set argument.
     * @param bitset
     * @return
     */
    public PPBitset and(PPBitset bitset) {
        if (this == bitset)
            return this;
        if (this.length() != bitset.length())
            throw new IllegalArgumentException("The encoding lengths of the two are not equal, unable to perform AND operation.");

        PPBitset newbitSet = new PPBitset(maxBitsInUse);
        for (int i = 0; i < maxBitsInUse; i++) {
            boolean bit1 = this.get(i);
            boolean bit2 = bitset.get(i);
            if(bit1 && bit2)
                newbitSet.set(i, true);
        }

        return newbitSet;
    }

    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set argument.
     * @param bitset
     * @return
     */
    public void or(PPBitset bitset) {
        if (this == bitset)
            return;
        if (this.length() != bitset.length())
            throw new IllegalArgumentException("The encoding lengths of the two are not equal, unable to perform OR operation.");

//        PPBitset newbitSet = new PPBitset(maxBitsInUse);
        for (int i = 0; i < maxBitsInUse; i++) {
            boolean bit1 = this.get(i);
            boolean bit2 = bitset.get(i);
            if (bit1 || bit2)
                this.set(i, true);
        }

//        return newbitSet;
    }

    /**
     * Perform an AND operation between a and b, and check if the result is equal to a.
     * check (this & bitset == this)
     * @param bitset
     * @return
     */
    public boolean isBitwiseSubset(PPBitset bitset) {
        if (this == bitset)
            return true;
        if (this.length() != bitset.length())
            throw new IllegalArgumentException("The encoding lengths of the two are not equal, unable to perform bitwise operation.");

        for (int i = 0; i < maxBitsInUse; i++) {
            boolean bit1 = this.get(i);
            boolean bit2 = bitset.get(i);
            if ((bit1 & bit2) != bit1)
                return false;
        }
        return true;
    }

    /**
     * if maxBitsInUse = 64 and bitset.set(63,true) then bitset.nextSetBit(63)=63
     */
    public int nextSetBit(int fromIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        if (fromIndex >= maxBitsInUse)
            return -1;

        int u = wordIndex(fromIndex);
        int word = words[u] & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Integer.numberOfTrailingZeros(word);
            if (++u == wordsInUse)
                return -1;
            word = words[u];
        }
    }

    /**
     * if maxBitsInUse = 64 and bitset.set(63,true) then bitset.nextClearBit(63) = 64
     */
    public int nextClearBit(int fromIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);

        if (fromIndex >= maxBitsInUse)
            return -1;

        int u = wordIndex(fromIndex);
        int word = ~words[u] & (WORD_MASK << fromIndex);

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Integer.numberOfTrailingZeros(word);
            if (++u == wordsInUse)
                return wordsInUse * BITS_PER_WORD;
            word = ~words[u];
        }
    }

    /**
     * 比较两个编码是否相等
     */
    public boolean equals(Object object) {
        if (this == object)
            return true;

        if (object == null || object.getClass() != getClass())
            return false;

        PPBitset ppBitset = (PPBitset) object;
        if (ppBitset.length() != this.length())
            return false;

        for (int i = 0; i < length(); i++) {
            if (this.get(i) != ppBitset.get(i))
                return false;
        }

        return true;
    }

    // 编码的比较规则
    @Override
    public int compareTo(PPBitset p) {
        if (this == p)
            return 0;
        if (this.length() != p.length())
            throw new IllegalArgumentException("The encoding lengths of the two are not equal, unable to perform bitwise operation.");

        int i = 0;
        int j = 0;
        while (i <= maxBitsInUse && j < maxBitsInUse) {
            i = this.nextSetBit(i);
            j = p.nextSetBit(j);

            if (i == -1 && j == -1) {
                return 0;
            } else if (i == -1) {
                return  1;
            } else if (j == -1) {
                return -1;
            } else if (i != j){
                return Integer.compare(i, j);
            }

            i++;
            j++;
        }

        return 0;
    }
}
