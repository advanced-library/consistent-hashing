package com.generoso.consistenthashing;

import java.util.*;

public class ConsistentHashing<T extends HashNode> {

    private final HashFunction hashFunction;
    private final int numberOfReplicas;
    private final SortedMap<Long, VirtualNode<T>> ring = new TreeMap<>();
    private final Map<T, Set<Long>> nodeToHashes = new HashMap<>();

    public ConsistentHashing(HashFunction hashFunction, int numberOfReplicas, Collection<T> nodes) {
        this.hashFunction = Objects.requireNonNull(hashFunction, "HashFunction must not be null.");

        if (numberOfReplicas <= 0) {
            throw new IllegalArgumentException("Number of replicas must be greater than 0.");
        }
        this.numberOfReplicas = numberOfReplicas;

        Objects.requireNonNull(nodes, "Node collection must not be null.");
        nodes.forEach(this::add);
    }

    public void add(T node) {
        Set<Long> hashes = new HashSet<>();
        var collisionAttempts = 0;

        for (var i = 0; i < numberOfReplicas; i++) {
            var baseKey = generateKeyFor(node, i);
            var key = baseKey;
            var hash = hashFunction.hash(key);

            // Linear probing
            while (ring.containsKey(hash)) {
                var existingVNode = ring.get(hash);
                if (existingVNode.key().equals(key)) {
                    throw new IllegalArgumentException("Node already in the ring.");
                }
                key = baseKey + "#" + (++collisionAttempts);
                hash = hashFunction.hash(key);
            }

            if (!ring.containsKey(hash) || ring.get(hash).key().equals(key)) {
                ring.put(hash, new VirtualNode<>(node, key));
                hashes.add(hash);
            }
        }

        nodeToHashes.put(node, hashes);
    }


    public boolean contains(T node) {
        return nodeToHashes.containsKey(node);
    }

    public int getRingSize() {
        return ring.size();
    }

    public void remove(T node) {
        var hashes = nodeToHashes.remove(node);
        if (hashes == null || hashes.isEmpty()) {
            throw new IllegalArgumentException("Node not found in the ring.");
        }

        for (Long hash : hashes) {
            var vNode = ring.get(hash);
            if (vNode != null && vNode.physicalNode().equals(node)) {
                ring.remove(hash);
            }
        }
    }
    public T get(Object key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("No nodes in the hash ring. Add nodes before calling get().");
        }

        long hash = hashFunction.hash(key.toString());
        if (!ring.containsKey(hash)) {
            SortedMap<Long, VirtualNode<T>> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return ring.get(hash).physicalNode();
    }


    private String generateKeyFor(T node, int replicaIndex) {
        return "%s-%s".formatted(node.identifier(), replicaIndex);
    }
}
