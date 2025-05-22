# Consistent Hashing

This repository contains an implementation of the **Consistent Hashing** algorithm, a strategy used in distributed systems to evenly distribute keys across a dynamic set of nodes with minimal disruption when nodes are added or removed.

## What is Consistent Hashing?

**Consistent Hashing** is a distributed hashing technique used to distribute data across a set of nodes (e.g., servers or caches) such that minimal data is reassigned when nodes join or leave the system.

Traditional hashing methods cause large-scale key remapping when the number of nodes changes. In contrast, consistent hashing reduces the number of keys that need to be moved, improving scalability and availability.

### Key Characteristics:
- **Scalable**: Adding or removing nodes affects only a small portion of keys.
- **Load Balancing**: When combined with virtual nodes, load is distributed more evenly.
- **Fault-Tolerant**: Easy to reassign keys in the event of node failure.

## How does it work?

- The hash space is treated as a ring (0 to 2³² - 1).
- Both keys and nodes are mapped to points on the ring using a hash function.
- Each key is assigned to the closest node **clockwise** on the ring.
- When a node is added or removed, only a fraction of the keys are remapped.

### Virtual Nodes

To avoid uneven distribution due to poor hash dispersion or node count imbalance, **virtual nodes** are used. 
A single physical node can be represented by multiple points on the ring, improving distribution and fault tolerance.