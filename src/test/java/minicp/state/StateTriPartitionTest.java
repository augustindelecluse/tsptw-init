package minicp.state;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class StateTriPartitionTest extends StateManagerTest{

    protected static Stream<Arguments> managerAndPartition() {
        return stateManager().map(sm -> Arguments.of(sm.get()[0], new StateTriPartition((StateManager) sm.get()[0], 9)));
    }

    /**
     * test the methods exclude, nPossible, nExcluded, nRequired, getPossible, getExcluded, getRequired
     * use removal of values as well as their correctness when backtracking
     */
    @ParameterizedTest
    @MethodSource("managerAndPartition")
    public void testExclude(StateManager sm, StateTriPartition set) {
        // every node should be considered as possible
        assertEquals(9, set.nPossible());
        assertEquals(0, set.nExcluded());
        assertEquals(0, set.nIncluded());
        sm.saveState();
        set.exclude(0);
        set.exclude(5);
        sm.saveState();
        set.exclude(8);
        assertEquals(6, set.nPossible());
        assertEquals(3, set.nExcluded());
        assertEquals(0, set.nIncluded());

        int[] values = new int[9];
        int size = set.fillPossible(values);
        assertEquals(6, size);
        int[] slice = java.util.Arrays.stream(values, 0, 6).toArray();
        java.util.Arrays.sort(slice);
        assertArrayEquals(new int[] {1,2,3,4,6,7}, slice);

        size = set.fillExcluded(values);
        assertEquals(3, size);
        slice = java.util.Arrays.stream(values, 0, 3).toArray();
        java.util.Arrays.sort(slice);
        assertArrayEquals(new int[] {0,5,8}, slice);

        size = set.fillIncluded(values);
        assertEquals(0, size);

        sm.restoreState();
        assertEquals(7, set.nPossible());
        assertEquals(2, set.nExcluded());
        assertEquals(0, set.nIncluded());
        sm.restoreState();
        assertEquals(9, set.nPossible());
        assertEquals(0, set.nExcluded());
        assertEquals(0, set.nIncluded());
    }

    /**
     * test the methods require, nPossible, nExcluded, nRequired, getPossible, getExcluded, getRequired
     * use requiring of values as well as their correctness when backtracking
     */
    @ParameterizedTest
    @MethodSource("managerAndPartition")
    public void testRequire(StateManager sm, StateTriPartition set) {
        // every node should be considered as possible
        assertEquals(9, set.nPossible());
        assertEquals(0, set.nExcluded());
        assertEquals(0, set.nIncluded());
        sm.saveState();
        set.include(0);
        set.include(5);
        sm.saveState();
        set.include(8);
        assertEquals(6, set.nPossible());
        assertEquals(0, set.nExcluded());
        assertEquals(3, set.nIncluded());

        int[] values = new int[9];
        int size = set.fillPossible(values);
        assertEquals(6, size);
        int[] slice = java.util.Arrays.stream(values, 0, 6).toArray();
        java.util.Arrays.sort(slice);
        assertArrayEquals(new int[] {1,2,3,4,6,7}, slice);

        size = set.fillExcluded(values);
        assertEquals(0, size);

        size = set.fillIncluded(values);
        assertEquals(3, size);
        slice = java.util.Arrays.stream(values, 0, 3).toArray();
        java.util.Arrays.sort(slice);
        assertArrayEquals(new int[] {0,5,8}, slice);

        sm.restoreState();
        assertEquals(7, set.nPossible());
        assertEquals(0, set.nExcluded());
        assertEquals(2, set.nIncluded());
        sm.restoreState();
        assertEquals(9, set.nPossible());
        assertEquals(0, set.nExcluded());
        assertEquals(0, set.nIncluded());
    }

    /**
     * test the methods exclude, require, nPossible, nExcluded, nRequired, getPossible, getExcluded, getRequired
     * use both removal and scheduling of values as well as their correctness when backtracking
     */
    @ParameterizedTest
    @MethodSource("managerAndPartition")
    public void testExcludeAndRequire(StateManager sm, StateTriPartition set) {
        // every node should be considered as possible
        assertEquals(9, set.nPossible());
        assertEquals(0, set.nExcluded());
        assertEquals(0, set.nIncluded());
        sm.saveState();
        set.include(0);
        set.exclude(2);
        set.include(5);
        sm.saveState();
        set.include(8);
        set.include(2); // should not be required as it was excluded
        set.exclude(3);
        set.exclude(3);
        assertEquals(4, set.nPossible());
        assertEquals(2, set.nExcluded());
        assertEquals(3, set.nIncluded());

        int[] values = new int[9];
        int size = set.fillPossible(values);
        assertEquals(4, size);
        int[] slice = java.util.Arrays.stream(values, 0, 4).toArray();
        java.util.Arrays.sort(slice);
        assertArrayEquals(new int[] {1,4,6,7}, slice);

        size = set.fillExcluded(values);
        assertEquals(2, size);
        slice = java.util.Arrays.stream(values, 0, 2).toArray();
        java.util.Arrays.sort(slice);
        assertArrayEquals(new int[] {2,3}, slice);

        size = set.fillIncluded(values);
        assertEquals(3, size);
        slice = java.util.Arrays.stream(values, 0, 3).toArray();
        java.util.Arrays.sort(slice);
        assertArrayEquals(new int[] {0,5,8}, slice);

        sm.restoreState();
        assertEquals(6, set.nPossible());
        assertEquals(1, set.nExcluded());
        assertEquals(2, set.nIncluded());

        size = set.fillPossible(values);
        assertEquals(6, size);
        slice = java.util.Arrays.stream(values, 0, 6).toArray();
        java.util.Arrays.sort(slice);
        assertArrayEquals(new int[] {1,3,4,6,7,8}, slice);

        size = set.fillExcluded(values);
        assertEquals(1, size);
        slice = java.util.Arrays.stream(values, 0, 1).toArray();
        java.util.Arrays.sort(slice);
        assertArrayEquals(new int[] {2}, slice);

        size = set.fillIncluded(values);
        assertEquals(2, size);
        slice = java.util.Arrays.stream(values, 0, 2).toArray();
        java.util.Arrays.sort(slice);
        assertArrayEquals(new int[] {0,5}, slice);

        sm.restoreState();
        assertEquals(9, set.nPossible());
        assertEquals(0, set.nExcluded());
        assertEquals(0, set.nIncluded());
    }

    /**
     * test the scheduling of one value. All other values should be considered as excluded
     */
    @ParameterizedTest
    @MethodSource("managerAndPartition")
    public void testRequireOne(StateManager sm, StateTriPartition set) {
        sm.saveState();

        set.include(7);
        set.include(6);
        set.include(3);
        assertSequenceState(set, new int[] {3, 6, 7}, new int[] {0, 1, 2, 4, 5, 8}, new int[] {});

        sm.saveState();

        assertSequenceState(set, new int[] {3, 6, 7}, new int[] {0, 1, 2, 4, 5, 8}, new int[] {});
        assertFalse(set.includeAndExcludeOthers(6));
        assertSequenceState(set, new int[] {3, 6, 7}, new int[] {0, 1, 2, 4, 5, 8}, new int[] {});

        sm.restoreState();

        assertSequenceState(set, new int[] {3, 6, 7}, new int[] {0, 1, 2, 4, 5, 8}, new int[] {});

        sm.restoreState();
        sm.saveState();

        assertTrue(set.includeAndExcludeOthers(6));
        assertSequenceState(set, new int[] {6}, new int[] {}, new int[] {0, 1, 2, 3, 4, 5, 7, 8});

        sm.restoreState();
        assertSequenceState(set, new int[] {}, new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8}, new int[] {});
    }

    private void assertSequenceFromSetInit(StateTriPartition set, Set<Integer> values, int min, int max) {
        assertEquals(0, set.nIncluded());
        assertEquals(0, set.nExcluded());
        assertEquals(values.size(), set.nPossible());
        for (int i = min-1; i <= max + 1; ++i ) {
            if (values.contains(i)) {
                assertTrue(set.isPossible(i));
                assertTrue(set.contains(i));
            } else {
                assertFalse(set.isPossible(i));
                assertFalse(set.contains(i));
            }
            assertFalse(set.isIncluded(i));
            assertFalse(set.isExcluded(i));
        }
    }

    /**
     * test operations on a sequence created from a set of integers values
     */
    @ParameterizedTest
    @MethodSource("stateManager")
    public void testSequenceFromSet(StateManager sm) {

        Set<Integer> values = Set.of(5, 7, 3, 4, 9, 2);
        int min = values.stream().min(Integer::compareTo).get();
        int max = values.stream().max(Integer::compareTo).get();
        StateTriPartition set = new StateTriPartition(sm, values);
        assertSequenceFromSetInit(set, values, min, max);

        sm.saveState();

        assertTrue(set.include(7));  // R: {7}
        assertTrue(set.include(9));  // R: {7, 9}
        assertFalse(set.include(8)); // not in the set
        assertTrue(set.exclude(4));  // E: {4}
        assertFalse(set.include(7)); // R: {7, 9}
        assertTrue(set.exclude(2));  // E: {2, 4}
        assertFalse(set.exclude(7));
        // set at this point: {R: {7, 9}, P: {3, 5}, E: {2, 4}}
        assertSequenceState(set, new int[] {7, 9}, new int[] {3, 5}, new int[] {2, 4});

        sm.saveState();

        assertTrue(set.excludeAllPossible());
        assertSequenceState(set, new int[] {7, 9}, new int[] {}, new int[] {2, 3, 4, 5});

        sm.restoreState();
        assertSequenceState(set, new int[] {7, 9}, new int[] {3, 5}, new int[] {2, 4});
        sm.saveState();

        assertTrue(set.include(3));  // R: {3, 7, 9}
        assertTrue(set.include(5));  // R: {3, 5, 7, 9}
        assertFalse(set.exclude(5));
        assertFalse(set.include(5));
        assertFalse(set.excludeAllPossible());
        assertSequenceState(set, new int[] {3, 5, 7, 9}, new int[] {}, new int[] {2, 4});

        sm.restoreState();

        assertSequenceState(set, new int[] {7, 9}, new int[] {3, 5}, new int[] {2, 4});

        sm.restoreState();

        assertSequenceFromSetInit(set, values, min, max);
    }

    /**
     * assert the state of a StateTriPartition
     * test the methods {@link StateTriPartition#nPossible()}, {@link StateTriPartition#nIncluded()}, {@link StateTriPartition#nExcluded()}
     * {@link StateTriPartition#fillPossible(int[])}, {@link StateTriPartition#fillIncluded(int[])}, {@link StateTriPartition#fillExcluded(int[])},
     * {@link StateTriPartition#contains(int)}, {@link StateTriPartition#size()}
     * @param set set that will be tested
     * @param sortedRequired required values, sorted
     * @param sortedPossible possible values, sorted
     * @param sortedExcluded excluded values, sorted
     */
    private void assertSequenceState(StateTriPartition set, int[] sortedRequired, int[] sortedPossible, int[] sortedExcluded) {
        int[] values = new int[Math.max(Math.max(sortedRequired.length, sortedPossible.length), sortedExcluded.length)];
        int[] slice;
        int[] expected;
        int len1;
        int len2;
        for (int i = 0; i < 3; ++i) {
            switch (i) {
                case 0 -> {
                    len1 = set.nIncluded();
                    len2 = set.fillIncluded(values);
                    expected = sortedRequired;
                }
                case 1 -> {
                    len1 = set.nPossible();
                    len2 = set.fillPossible(values);
                    expected = sortedPossible;
                }
                default -> {
                    len1 = set.nExcluded();
                    len2 = set.fillExcluded(values);
                    expected = sortedExcluded;
                }
            }
            assertEquals(expected.length, len1);
            assertEquals(expected.length, len2);
            slice = Arrays.copyOfRange(values, 0, len1);
            java.util.Arrays.sort(slice);
            assertArrayEquals(expected, slice);
        }
        assertEquals(sortedRequired.length + sortedPossible.length + sortedExcluded.length, set.size());
    }

    /**
     * assert the creation of a sequence from min and max values
     * @param set StateTriPartition that has been created
     * @param min minimum value inclusive within the set
     * @param max maximum value inclusive within the set
     */
    private void assertSequenceFromMinMaxInit(StateTriPartition set, int min, int max) {
        assertEquals(0, set.nIncluded());
        assertEquals(0, set.nExcluded());
        assertEquals(max - min + 1, set.nPossible());
        for (int i = min; i <= max; ++i) {
            assertTrue(set.isPossible(i));
            assertTrue(set.contains(i));
            assertFalse(set.isIncluded(i));
            assertFalse(set.isExcluded(i));
        }

        int[] possible = IntStream.range(min, max+1).toArray();
        int[] values = new int[max-min+1];
        assertEquals(set.nPossible(), possible.length);
        int len = set.fillPossible(values);
        assertEquals(possible.length, len);
        java.util.Arrays.sort(values);
        assertArrayEquals(possible, values);

        assertEquals(0, set.nExcluded());
        assertEquals(0, set.nIncluded());
    }

    /**
     * test operations on a sequence created from min and max values
     */
    @ParameterizedTest
    @MethodSource("stateManager")
    public void testSequenceFromMinMax(StateManager sm) {
        int min = 5;
        int max = 12;
        StateTriPartition set = new StateTriPartition(sm, 5, 12);
        assertSequenceFromMinMaxInit(set, min, max);
        for (int i = min-3; i < min; ++i) {
            assertFalse(set.isPossible(i));
            assertFalse(set.isIncluded(i));
            assertFalse(set.isExcluded(i));
            assertFalse(set.include(i));
            assertFalse(set.exclude(i));
            assertFalse(set.contains(i));
        }
        for (int i = max+1; i < max+4; ++i) {
            assertFalse(set.isPossible(i));
            assertFalse(set.isIncluded(i));
            assertFalse(set.isExcluded(i));
            assertFalse(set.include(i));
            assertFalse(set.exclude(i));
            assertFalse(set.contains(i));
        }

        sm.saveState();
        // exclude and require some values
        assertTrue(set.include(6));  // R: {6}
        assertFalse(set.include(6)); // R: {6}
        assertFalse(set.exclude(6)); // E: {}
        assertTrue(set.exclude(12)); // E: {12}
        assertTrue(set.exclude(5));  // E: {12, 5}
        assertTrue(set.exclude(10)); // E: {12, 5, 10}
        assertFalse(set.exclude(1)); // does not belong to the set
        assertFalse(set.exclude(12));
        assertTrue(set.include(8));  // R: {6, 8}
        // set at this point: {R: {6, 8}, P: {7, 9, 11}, E: {5, 10, 12}}
        assertSequenceState(set, new int[] {6, 8}, new int[] {7, 9, 11}, new int[] {5, 10, 12});

        sm.saveState();

        assertTrue(set.include(9));
        assertTrue(set.include(7));
        assertTrue(set.include(11));
        assertFalse(set.exclude(11));
        assertFalse(set.include(9));
        // set at this point: {R: {6, 7, 8, 9, 11}, P: {}, E: {5, 10, 12}}
        assertSequenceState(set, new int[] {6, 7, 8, 9, 11}, new int[] {}, new int[] {5, 10, 12});

        sm.restoreState();
        assertSequenceState(set, new int[] {6, 8}, new int[] {7, 9, 11}, new int[] {5, 10, 12});
        sm.saveState();

        assertTrue(set.exclude(9));
        assertTrue(set.exclude(7));
        assertTrue(set.exclude(11));
        assertFalse(set.include(11));
        assertFalse(set.exclude(9));
        // set at this point: {R: {6, 8}, P: {}, E: {5, 7, 9, 10, 11, 12}}
        assertSequenceState(set, new int[] {6, 8}, new int[] {}, new int[] {5, 7, 9, 10, 11, 12});

        sm.restoreState();

        assertSequenceState(set, new int[] {6, 8}, new int[] {7, 9, 11}, new int[] {5, 10, 12});

        sm.restoreState();

        assertSequenceFromMinMaxInit(set, min, max);

    }

}
