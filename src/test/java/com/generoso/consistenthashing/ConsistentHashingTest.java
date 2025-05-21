package com.generoso.consistenthashing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ConsistentHashingTest {

    private static final int REPLICAS = 3;
    private static final HashFunction MOCKED_HASH_FUNCTION = mock(HashFunction.class);

    private final ConsistentHashing<HashNode> consistentHashing =
            new ConsistentHashing<>(MOCKED_HASH_FUNCTION, REPLICAS, new ArrayList<>());

    @BeforeEach
    void setUp() {
        when(MOCKED_HASH_FUNCTION.hash(anyString())).thenReturn(1L, 2L, 3L);
    }

    @Nested
    @DisplayName("Test the construction of the consistent hashing")
    class ConstructionTests {
        @Test
        void hashFunctionShouldNotBeNull() {
            var exception = assertThrows(NullPointerException.class, () ->
                    new ConsistentHashing<>(null, 0, List.of(new DummyNode("NodeA"))));
            assertEquals("HashFunction must not be null.", exception.getMessage());
        }

        @Test
        void testConstructorThrowsWithZeroReplicas() {
            var exception = assertThrows(IllegalArgumentException.class, () ->
                    new ConsistentHashing<>(MOCKED_HASH_FUNCTION, 0, List.of(new DummyNode("NodeA"))));
            assertEquals("Number of replicas must be greater than 0.", exception.getMessage());
        }

        @Test
        void nodesShouldNotBeNull() {
            var exception = assertThrows(NullPointerException.class, () ->
                    new ConsistentHashing<>(MOCKED_HASH_FUNCTION, 3, null));
            assertEquals("Node collection must not be null.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Test adding nodes to the ring")
    class AddingNodesToTheRing {
        @Test
        void addSingleNodeWithoutCollision() {
            // Arrange
            var node = new DummyNode("NodeA");

            // Act
            consistentHashing.add(node);

            // Assert
            assertTrue(consistentHashing.contains(node));
        }

        @Test
        void createsTheCorrectNumberOfReplicas() {
            // Arrange
            var node = new DummyNode("NodeA");

            // Act
            consistentHashing.add(node);

            // Assert
            assertEquals(REPLICAS, consistentHashing.getRingSize());
        }

        @Test
        void doesNotIncludeDuplicatedNodes() {
            // Arrange
            var node = new DummyNode("NodeA");

            when(MOCKED_HASH_FUNCTION.hash(anyString())).thenReturn(1L, 2L, 3L, 1L);

            // Act
            consistentHashing.add(node);
            var exception = assertThrows(IllegalArgumentException.class, () -> consistentHashing.add(node));

            // Assert
            assertEquals("Node already in the ring.", exception.getMessage());
        }

        @Test
        void addTwoDifferentNodesWithoutCollision() {
            // Arrange
            var nodeA = new DummyNode("NodeA");
            var nodeB = new DummyNode("NodeB");

            when(MOCKED_HASH_FUNCTION.hash(anyString())).thenReturn(1L, 2L, 3L, 4L, 5L, 6L);

            // Act
            consistentHashing.add(nodeA);
            consistentHashing.add(nodeB);

            // Assert
            assertTrue(consistentHashing.contains(nodeA));
            assertTrue(consistentHashing.contains(nodeB));
            assertEquals(REPLICAS * 2, consistentHashing.getRingSize());
        }

        @Test
        void makesSureTheHashingFunctionIsCalled() {
            // Arrange
            var node = new DummyNode("NodeA");
            var mockedHashFunction = mock(HashFunction.class);
            when(mockedHashFunction.hash(anyString())).thenReturn(1L, 2L, 3L);
            var consistentHashing = new ConsistentHashing<>(mockedHashFunction, REPLICAS, new ArrayList<>());

            // Assert
            consistentHashing.add(node);

            // Assert
            verify(mockedHashFunction, times(REPLICAS)).hash(anyString());
        }

        @Test
        void addDifferentNodeWhenHashCollisionHappens() {
            // Arrange
            var nodeA = new DummyNode("NodeA");
            var nodeB = new DummyNode("NodeB");

            when(MOCKED_HASH_FUNCTION.hash(anyString())).thenReturn(10L, 20L, 30L, // nodeA
                    10L, 10L, 10L, 11L, 21L, 31L // nodeB - collision + linear probing
            );

            // Act
            consistentHashing.add(nodeA);
            consistentHashing.add(nodeB);

            // Assert
            assertTrue(consistentHashing.contains(nodeA));
            assertTrue(consistentHashing.contains(nodeB));
            assertEquals(REPLICAS * 2, consistentHashing.getRingSize());
        }
    }

    @Nested
    @DisplayName("Test removing nodes to the ring")
    class RemovingNodesToTheRing {
        @Test
        void removeSingleNodeSuccessfully() {
            // Arrange
            var node = new DummyNode("A");
            when(MOCKED_HASH_FUNCTION.hash(anyString())).thenReturn(1L, 2L, 3L);
            consistentHashing.add(node);

            // Act
            consistentHashing.remove(node);

            // Assert
            assertFalse(consistentHashing.contains(node));
            assertEquals(0, consistentHashing.getRingSize());
        }

        @Test
        void removeNodeWhenMultipleNodesExist() {
            // Arrange
            var nodeA = new DummyNode("A");
            var nodeB = new DummyNode("B");

            when(MOCKED_HASH_FUNCTION.hash(anyString())).thenReturn(10L, 20L, 30L, 40L, 50L, 60L);

            consistentHashing.add(nodeA);
            consistentHashing.add(nodeB);

            // Act
            consistentHashing.remove(nodeA);

            // Assert
            assertFalse(consistentHashing.contains(nodeA));
            assertTrue(consistentHashing.contains(nodeB));
            assertEquals(REPLICAS, consistentHashing.getRingSize());
        }

        @Test
        void removeANonExistentNodeThrowsException() {
            // Assert
            var node = new DummyNode("Ghost");

            // Act
            var exception = assertThrows(IllegalArgumentException.class, () -> consistentHashing.remove(node));

            // Assert
            assertEquals("Node not found in the ring.", exception.getMessage());
        }

        @Test
        void addANodeAfterItBeingRemoved() {
            // Arrange
            var node = new DummyNode("ReAdd");
            when(MOCKED_HASH_FUNCTION.hash(anyString()))
                    .thenReturn(100L, 200L, 300L) // add first
                    .thenReturn(101L, 201L, 301L); // re-add after remove

            // Act & Assert
            consistentHashing.add(node);
            consistentHashing.remove(node);
            assertFalse(consistentHashing.contains(node));
            assertEquals(0, consistentHashing.getRingSize());

            consistentHashing.add(node);
            assertTrue(consistentHashing.contains(node));
            assertEquals(REPLICAS, consistentHashing.getRingSize());
        }
    }

    @Nested
    @DisplayName("Test retrieving the nodes")
    class GetNodesFromTheRing {
        @Test
        void getThrowsAnExceptionWhenRingIsEmpty() {
            // Act & Assert
            assertThrows(IllegalStateException.class, () -> consistentHashing.get("any-key"));
        }

        @Test
        void getReturnsCorrectNodeForKey() {
            // Arrange
            var nodeA = new DummyNode("NodeA");
            when(MOCKED_HASH_FUNCTION.hash(anyString())).thenReturn(10L, 20L, 30L, 25L);

            consistentHashing.add(nodeA);

            // Act
            var result = consistentHashing.get("my-key");

            // Assert
            assertEquals(nodeA, result);
        }

        @Test
        void getWrapsAroundWhenKeyIsGreaterThanAllHashes() {
            // Arrange
            var nodeA = new DummyNode("NodeA");

            when(MOCKED_HASH_FUNCTION.hash(anyString())).thenReturn(10L, 20L, 30L, 99L);

            consistentHashing.add(nodeA);

            // Act
            var result = consistentHashing.get("some-key");

            // Assert
            assertEquals(nodeA, result);
        }

        @Test
        void getReturnsCorrectNodeAmongMultiple() {
            // Arrange
            var nodeA = new DummyNode("A");
            var nodeB = new DummyNode("B");

            // Hashes for A: 100, 200, 300
            // Hashes for B: 400, 500, 600
            // KEY between 400 and 500 in NodeB
            when(MOCKED_HASH_FUNCTION.hash(anyString()))
                    .thenReturn(100L, 200L, 300L, 400L, 500L, 600L, 420L);

            consistentHashing.add(nodeA);
            consistentHashing.add(nodeB);

            // Act
            var result = consistentHashing.get("key-420");

            // Assert
            assertEquals(nodeB, result);
        }
    }
}
