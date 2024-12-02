package encoding;

import encoding.util.HashFunction;
import encoding.util.HashFunctionInterface;



public class PropertyEncodingConstructor {
    // 传参为待映射字符串，编码长度以及哈希映射函数个数
    public static PPBitset encoding(String content, int length, int hashFuncCount) {
        if (content == null || content.length() == 0)
            throw new IllegalArgumentException("The property value is null");
        if (length <= 0 || hashFuncCount <= 0)
            throw new IllegalArgumentException(String.format("the value of encoding length or counts of hash function cannot be zero." +
                    "length=%d, hashCounts=%d", length, hashFuncCount));

        PPBitset bitSet = new PPBitset(length);
        for (int i = 0; i < hashFuncCount; i++) {
            HashFunctionInterface hashFunction = HashFunction.HASHFUNCTIONS[i];
            long l = hashFunction.apply(content);
            int bit = (int) (hashFunction.apply(content) % length);
            bitSet.set(bit);
        }

        return bitSet;
    }
}





