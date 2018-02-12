package utils;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Provides an md5 and sha1 hash function without producing deprecation warnings.
 * From: https://github.com/google/guava/issues/2841
 */

public class LegacyHashing {
    @SuppressWarnings("deprecation")
    public static HashFunction md5() {
        return Hashing.md5();
    }

    @SuppressWarnings("deprecation")
    public static HashFunction sha1() {
        return Hashing.sha1();
    }
}
