package encoding.util;

/**
 * 该类包含可能用到的多种哈希方法，所有的方法都是静态方法，可以直接用类名进行访问
 */
public class HashFunction {
    // HASHFUNCTIONS 保存对静态哈希方法的引用
    public static HashFunctionInterface[] HASHFUNCTIONS = {HashFunction::BKDRHash, HashFunction::APHash, HashFunction::JSHash,
            HashFunction::RSHash, HashFunction::SDBMHash, HashFunction::PJWHash, HashFunction::ELFHash, HashFunction::DJBHash,
            HashFunction::DEKHash};

    public static void main(String[] args) {
        int length = 4;
        getBitOfOne(length, "香蕉");
        getBitOfOne(length, "葛王");
        getBitOfOne(length, "王泽");
        getBitOfOne(length, "王泽兰");
    }
    public static void getBitOfOne(int length, String str) {
        long h1 = BKDRHash(str) % length;  // 22012942876
        long h2 = APHash(str) % length;    // 1881724040

        System.out.println(h1 + "  " + h2);
        System.out.println(h1);
    }

    public static long BKDRHash(String str) {
        long seed = 131; // 31 131 1313 13131 131313 etc..
        long hash = 0;
        long mod = 1000000007;

        for(int i = 0; i < str.length(); i++)
        {
            hash = ((hash * seed) + str.charAt(i)) % mod;
        }

        return hash;
    }

    public static long APHash(String str) {
        long hash = 0;

        for (int i = 0; i < str.length(); i++) {
            if ((i & 1) == 0) {
                hash ^= (hash << 7) ^ (str.charAt(i)) ^ (hash >> 3);
            } else {
                hash ^= ~((hash << 11) ^ (str.charAt(i)) ^ (hash >> 5));
            }
        }

        return hash & 0x7FFFFFFF;
    }

    public static int JSHash(String str) {
        int hash = 0;

        for (int i = 0; i < str.length(); i++) {
            hash ^= (hash << 5) + (int)str.charAt(i) + (hash >> 2);
        }

        return hash & 0x7FFFFFFF;
    }

    public static int RSHash(String str) {
        int hash = 0;

        int a = 63689;
        final int b = 378551;

        for (int i = 0; i < str.length(); i++) {
            hash = hash * a + (int)str.charAt(i);
            a *= b;
        }

        return hash & 0x7FFFFFFF;
    }

    public static int SDBMHash(String str) {
        int hash = 0;

        for (int i = 0; i < str.length(); i++) {
            hash = (int)str.charAt(i) + (hash << 6) + (hash << 16) - hash;
        }

        return hash & 0x7FFFFFFF;
    }

    public static int PJWHash(String str) {
        int BitsInUnignedInt = 32;
        int ThreeQuarters    = 24;
        int OneEighth        = 4;
        int HighBits         = (int)(0xFFFFFFFF) << (BitsInUnignedInt - OneEighth);
        int hash             = 0;
        int test             = 0;

        for (int i = 0; i < str.length(); i++) {
            hash = (hash << OneEighth) + (int)str.charAt(i);
            if ((test = hash & HighBits) != 0)
            {
                hash = ((hash ^ (test >> ThreeQuarters)) & (~HighBits));
            }
        }

        return hash & 0x7FFFFFFF;
    }

    public static int ELFHash(String str) {
        int hash = 0;
        int x = 0;

        for (int i = 0; i < str.length(); i++) {
            hash = (hash << 4) + (int)str.charAt(i);

            if ((x & hash & 0xF0000000L) != 0) {
                hash ^= x >> 24;
                hash &= ~x;
            }
        }

        return hash & 0x7FFFFFFF;
    }

    public static int DJBHash(String str) {
        int hash = 5381;

        for (int i = 0; i < str.length(); i++) {
            hash += (hash << 5) + (int)str.charAt(i);
        }

        return hash & 0x7FFFFFFF;
    }

    public static int DEKHash(String str) {
        int hash = str.length();

        for (int i = 0; i < str.length(); i++) {
            hash = (hash << 5) ^ (hash >> 27) ^ (int)str.charAt(i);
        }

        return hash & 0x7FFFFFFF;
    }

    public static int BPHash(String str) {
        int hash = str.length();

        for (int i = 0; i < str.length(); i++) {
            hash = (hash << 7) ^ (int)str.charAt(i);
        }

        return hash & 0x7FFFFFFF;
    }

    public static int FNVHash(String str) {
        int fnvprime = 0x811C9DC5;
        int hash = 0;

        for (int i = 0; i < str.length(); i++) {
            hash *= fnvprime;
            hash ^= (int)str.charAt(i);
        }

        return hash & 0x7FFFFFFF;
    }
}

