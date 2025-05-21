package com.generoso.consistenthashing;

/**
 * Represents a hash function used for consistent hashing.
 * Implementation should ensure a good distribution and consistency.
 */
@FunctionalInterface
public interface HashFunction {

    /**
     * Computes the hash value for the given input.
     *
     * @param input the input to be hashed
     * @return the hash value as long
     */
    long hash(String input);
}