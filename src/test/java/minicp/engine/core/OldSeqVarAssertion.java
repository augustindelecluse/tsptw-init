package minicp.engine.core;

import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class OldSeqVarAssertion {
    
    /**
     * test if a sequence corresponds to the expected arrays
     * @param scheduled scheduled nodes of the sequence. Ordered by appearance in the sequence, omitting begin and end node
     * @param possible possible nodes of the sequence
     * @param excluded excluded nodes of the sequence
     * @param scheduledInsert scheduled insertions of each InsertionVar. first indexing = id of the InsertionVar.
     *                        Must contain the beginning node if present
     * @param possibleInsert possible insertions of each InsertionVar. first indexing = id of the InsertionVar.
     *                       Must contain the beginning node if present
     */
    public static void isSequenceValid(OldSeqVar sequence, int[] scheduled, int[] possible, int[] excluded,
                                       int[][] scheduledInsert, int[][] possibleInsert) {
        assertEquals(scheduled.length, sequence.nMember());
        assertEquals(possible.length, sequence.nPossible());
        assertEquals(excluded.length, sequence.nExcluded());
        assertEquals(sequence.begin(), sequence.nextMember(sequence.end()));
        assertEquals(sequence.end(), sequence.predMember(sequence.begin()));
        assertEquals(sequence.nNode(), scheduledInsert.length);
        // test the ordering
        int[] ordering = new int[scheduled.length];
        assertEquals(scheduled.length, sequence.fillOrder(ordering, true));
        for (int i = 0 ; i < ordering.length ; ++i) {
            assertEquals(scheduled[i], ordering[i]);
        }

        int[] insertions = IntStream.range(0, sequence.nNode()).toArray();
        int[] actual;
        int[] expected;
        int pred = sequence.end();
        for (int i: scheduled) {
            assertTrue(sequence.isMember(i));
            assertFalse(sequence.isPossible(i));
            assertFalse(sequence.isExcluded(i));
            assertEquals(0, sequence.fillMemberPredInsert(i, insertions));
            assertEquals(0, sequence.fillPossiblePredInsert(i, insertions));
            assertEquals(0, sequence.nMemberPredInsert(i));
            assertEquals(0, sequence.nPossiblePredInsert(i));
            assertEquals(i, sequence.nextMember(pred));
            assertEquals(pred, sequence.predMember(i));
            pred = i;
        }
        for (int i: possible) {
            assertFalse(sequence.isMember(i));
            assertTrue(sequence.isPossible(i));
            assertFalse(sequence.isExcluded(i));

            assertEquals(sequence.fillMemberPredInsert(i, insertions), sequence.nMemberPredInsert(i));
            assertEquals(scheduledInsert[i].length, sequence.fillMemberPredInsert(i, insertions));
            actual = Arrays.copyOfRange(insertions, 0, scheduledInsert[i].length);
            Arrays.sort(actual);
            expected = scheduledInsert[i];
            Arrays.sort(expected);
            assertArrayEquals(expected, actual);

            assertEquals(sequence.fillPossiblePredInsert(i, insertions), sequence.nPossiblePredInsert(i));
            assertEquals(possibleInsert[i].length, sequence.fillPossiblePredInsert(i, insertions));
            actual = Arrays.copyOfRange(insertions, 0, possibleInsert[i].length);
            Arrays.sort(actual);
            expected = possibleInsert[i];
            Arrays.sort(expected);
            assertArrayEquals(expected, actual);
        }
        for (int i: excluded) {
            assertFalse(sequence.isMember(i));
            assertFalse(sequence.isPossible(i));
            assertTrue(sequence.isExcluded(i));
            assertEquals(0, sequence.nMemberPredInsert(i));
            assertEquals(0, sequence.nPossiblePredInsert(i));
        }
    }

    /**
     * test if a sequence corresponds to the expected arrays
     * assume that no exclusion of a node for a particular InsertionVar has occurred
     * @param scheduled scheduled nodes of the sequence. Ordered by appearance in the sequence, omitting begin and end node
     * @param possible possible nodes of the sequence
     * @param excluded excluded nodes of the sequence
     */
    static void isSequenceValid(OldSeqVar sequence, int[] scheduled, int[] possible, int[] excluded) {
        assertEquals(scheduled.length, sequence.nMember());
        assertEquals(possible.length, sequence.nPossible());
        assertEquals(excluded.length, sequence.nExcluded());
        assertEquals(sequence.begin(), sequence.nextMember(sequence.end()));
        assertEquals(sequence.end(), sequence.predMember(sequence.begin()));

        int[] sorted_scheduled = new int[scheduled.length - 1]; // used for scheduled insertions. includes begin node but not ending node
        for (int i = 0; i < sorted_scheduled.length; ++i)
            sorted_scheduled[i] = scheduled[i];
        Arrays.sort(sorted_scheduled);
        int[] sorted_possible = Arrays.copyOf(possible, possible.length);
        Arrays.sort(sorted_possible);
        int[] val;

        int nbScheduledInsertions = scheduled.length - 1; // number of scheduled nodes - end node
        int nbPossibleInsertions = possible.length - 1;   // number of possible nodes - node being tested
        int[] insertions = IntStream.range(0, sequence.nNode()).toArray();

        int pred = sequence.end();
        for (int i: scheduled) {
            assertTrue(sequence.isMember(i));
            assertFalse(sequence.isPossible(i));
            assertFalse(sequence.isExcluded(i));
            assertEquals(0, sequence.fillMemberPredInsert(i, insertions));
            assertEquals(0, sequence.fillPossiblePredInsert(i, insertions));

            assertEquals(i, sequence.nextMember(pred));
            assertEquals(pred, sequence.predMember(i));
            pred = i;
        }
        for (int i: possible) {
            assertFalse(sequence.isMember(i));
            assertTrue(sequence.isPossible(i));
            assertFalse(sequence.isExcluded(i));

            assertEquals(nbScheduledInsertions, sequence.fillMemberPredInsert(i, insertions));
            val = Arrays.copyOfRange(insertions, 0, nbScheduledInsertions);
            Arrays.sort(val);
            assertArrayEquals(sorted_scheduled, val);

            assertEquals(nbPossibleInsertions, sequence.fillPossiblePredInsert(i, insertions));
            val = Arrays.copyOfRange(insertions, 0, nbPossibleInsertions);
            Arrays.sort(val);
            assertArrayEquals(Arrays.stream(sorted_possible).filter(j -> j != i).toArray(), val);
        }
        for (int i: excluded) {
            assertFalse(sequence.isMember(i));
            assertFalse(sequence.isPossible(i));
            assertTrue(sequence.isExcluded(i));
        }
    }
}
