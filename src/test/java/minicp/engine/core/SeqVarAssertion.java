package minicp.engine.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Contains the static methods for asserting that a {@link SeqVar} matches the expected requirements
 */
public class SeqVarAssertion {

    /**
     * Asserts that a {@link SeqVar} corresponds to the expected arrays
     *
     * @param seqVar {@link SeqVar} to test
     * @param member member nodes of the sequence, ordered by appearance in the sequence, omitting begin and end node
     * @param possible possible nodes of the sequence
     * @param excluded excluded nodes of the sequence
     * @param memberPredInsert member predecessor insertions of each {@link InsertVar}.
     *                         First indexing = id of the variable. Must contain the beginning node if present
     * @param possiblePredInsert possible predecessor insertions of each {@link InsertVar}.
     *                         First indexing = id of the variable. Must contain the beginning node if present
     * @param memberSuccInsert member successor insertions of each {@link InsertVar}.
     *                         First indexing = id of the variable. Must contain the ending node if present
     * @param possibleSuccInsert possible successor insertions of each {@link InsertVar}.
     *                         First indexing = id of the variable. Must contain the ending node if present
     * @throws IllegalArgumentException if the given insertions array do not make any sense
     */
    public static void assertGraphSeqValid(SeqVar seqVar, int[] member, int[] possible, int[] excluded,
                                           int[][] memberPredInsert, int[][] possiblePredInsert,
                                           int[][] memberSuccInsert, int[][] possibleSuccInsert) {
        assertGraphSeqValid(seqVar, member, null, possible, excluded,
                memberPredInsert, possiblePredInsert, memberSuccInsert, possibleSuccInsert);
    }

    /**
     * Asserts that a {@link SeqVar} corresponds to the expected arrays
     *
     * @param seqVar {@link SeqVar} to test
     * @param member member nodes of the sequence, ordered by appearance in the sequence, omitting begin and end node
     * @param required required nodes from the sequence. If null, assumes that the required nodes are the member nodes
     * @param possible possible nodes of the sequence
     * @param excluded excluded nodes of the sequence
     * @param memberPredInsert member predecessor insertions of each {@link InsertVar}.
     *                         First indexing = id of the variable. Must contain the beginning node if present
     * @param possiblePredInsert possible predecessor insertions of each {@link InsertVar}.
     *                         First indexing = id of the variable. Must contain the beginning node if present
     * @param memberSuccInsert member successor insertions of each {@link InsertVar}.
     *                         First indexing = id of the variable. Must contain the ending node if present
     * @param possibleSuccInsert possible successor insertions of each {@link InsertVar}.
     *                         First indexing = id of the variable. Must contain the ending node if present
     * @throws IllegalArgumentException if the given insertions array do not make any sense
     */
    public static void assertGraphSeqValid(SeqVar seqVar, int[] member, int[] required, int[] possible, int[] excluded,
                                           int[][] memberPredInsert, int[][] possiblePredInsert,
                                           int[][] memberSuccInsert, int[][] possibleSuccInsert) {
        /*
        first ensures that the arguments are somewhat valid:
         - i in member => i in required
         - i in memberPredInsert[j]   && isPossible(j) <=> j in possibleSuccInsert[i] && isMember(i)   [xx]
         - i in possiblePredInsert[j] && isPossible(j) <=> j in possibleSuccInsert[i] && isPossible(i) [xx]
         - i in possiblePredInsert[j] && isMember(j)   <=> j in memberSuccInsert[i]   && isPossible(i) [  ]
         - i in member   => memberPredInsert[i].length == 0 && memberSuccInsert[i].length == 0
         - i in excluded => memberPredInsert[i].length == 0 && possiblePredInsert[i].length == 0 &&
                            memberSuccInsert[i].length == 0 && possibleSuccInsert[i].length == 0
         */
        if (required == null)
            required = member;
        int nNode = seqVar.nNode();
        Set<Integer>[] memberSuccInsertSet = new Set[nNode];
        Set<Integer>[] possibleSuccInsertSet = new Set[nNode];
        Set<Integer> memberSet = Arrays.stream(member).boxed().collect(Collectors.toSet());
        Set<Integer> possibleSet = Arrays.stream(possible).boxed().collect(Collectors.toSet());
        Set<Integer> excludedSet = Arrays.stream(excluded).boxed().collect(Collectors.toSet());
        Set<Integer> requiredSet = Arrays.stream(required).boxed().collect(Collectors.toSet());
        Set<Integer> possibleAndRequired = IntStream.concat(possibleSet.stream().mapToInt(i -> i),
                requiredSet.stream().filter(i -> !memberSet.contains(i)).mapToInt(i -> i)).boxed().collect(Collectors.toSet());
        for (int i = 0 ; i < nNode ; ++i) {
            memberSuccInsertSet[i] = Arrays.stream(memberSuccInsert[i]).boxed().collect(Collectors.toSet());
            possibleSuccInsertSet[i] = Arrays.stream(possibleSuccInsert[i]).boxed().collect(Collectors.toSet());
        }
        Set<Integer> allNodes = new HashSet<>();
        allNodes.addAll(requiredSet);
        allNodes.addAll(possibleSet);
        allNodes.addAll(excludedSet);
        if (allNodes.size() != nNode || requiredSet.size() + possibleSet.size() + excludedSet.size() != nNode)
            throw new IllegalArgumentException("The given arrays do not cover nNode elements");
        if (IntStream.range(0, nNode)
                .filter(i -> !requiredSet.contains(i) && !possibleSet.contains(i) && !excludedSet.contains(i))
                .findAny()
                .isPresent()) {
            throw new IllegalArgumentException("Not all nodes are covered within the given arrays");
        }

        Set<Integer>[] memberPredInsertSet = new Set[nNode];
        Set<Integer>[] possiblePredInsertSet = new Set[nNode];
        for (int i = 0 ; i < nNode ; ++i) {
            memberPredInsertSet[i] = Arrays.stream(memberPredInsert[i]).boxed().collect(Collectors.toSet());
            possiblePredInsertSet[i] = Arrays.stream(possiblePredInsert[i]).boxed().collect(Collectors.toSet());
        }

        for (int j : possible) {
            // i in memberPredInsert[j]   && isPossible(j) => j in possibleSuccInsert[i] && isMember(i)
            for (int i : memberPredInsert[j]) {
                if (!(possibleSuccInsertSet[i].contains(j) && memberSet.contains(i)))
                    throw new IllegalArgumentException(String.format("You said that node %d has %d as a predecessor " +
                            "insertion but %d is not a successor insertion for %d", j, i, j, i));
            }
            // i in possiblePredInsert[j] && isPossible(j) => j in possibleSuccInsert[i] && isPossible(i)
            for (int i : possiblePredInsert[j]) {
                if (!(possibleSuccInsertSet[i].contains(j) && possibleAndRequired.contains(i)))
                    throw new IllegalArgumentException(String.format("You said that node %d has %d as a predecessor " +
                            "insertion but %d is not a successor insertion for %d", j, i, j, i));
            }
            // i in possibleSuccInsert[j] && isPossible(j) => j in possiblePredInsert[i] && isPossible(i)
            for (int i : possibleSuccInsert[j]) {
                if (!(possiblePredInsertSet[i].contains(j) && possibleAndRequired.contains(i)))
                    throw new IllegalArgumentException(String.format("You said that node %d has %d as a successor " +
                            "insertion but %d is not a predecessor insertion for %d", j, i, j, i));
            }
            // i in memberSuccInsert[j] && isPossible(j) => j in possiblePredInsert[i] && isMember(i)
            for (int i : memberSuccInsert[j]) {
                if (!(possiblePredInsertSet[i].contains(j) && memberSet.contains(i)))
                    throw new IllegalArgumentException(String.format("You said that node %d has %d as a successor " +
                            "insertion but %d is not a predecessor insertion for %d", j, i, j, i));
            }
        }
        for (int j : member) {
            if (memberPredInsert[j].length != 0 || memberSuccInsert[j].length != 0)
                throw new IllegalArgumentException("You said that a member node has some member insertions");
            // i in possiblePredInsert[j] && isMember(j) => j in memberSuccInsert[i] && isPossible(i)
            for (int i : possiblePredInsert[j]) {
                if (!(memberSuccInsertSet[i].contains(j) && possibleAndRequired.contains(i)))
                    throw new IllegalArgumentException(String.format("You said that node %d has %d as a predecessor " +
                            "insertion but %d is not a successor insertion for %d", j, i, j, i));
            }
            // i in possibleSuccInsert[j] && isMember(j) => j in memberPredInsert[i] && isPossible(i)
            for (int i : possibleSuccInsert[j]) {
                if (!(memberPredInsertSet[i].contains(j) && possibleAndRequired.contains(i)))
                    throw new IllegalArgumentException(String.format("You said that node %d has %d as a successor " +
                            "insertion but %d is not a predecessor insertion for %d", j, i, j, i));
            }
            if (!requiredSet.contains(j))
                throw new IllegalArgumentException(String.format("You said that node %d is a member but it is not a required node", j));
        }
        for (int i : excluded) {
            if (memberPredInsert[i].length != 0 || possiblePredInsert[i].length != 0 ||
                    memberSuccInsert[i].length != 0 || possibleSuccInsert[i].length != 0)
                throw new IllegalArgumentException("You said that an excluded node as some insertions");
        }
        if (memberPredInsert.length != nNode ||
                possiblePredInsert.length != nNode ||
                memberSuccInsert.length != nNode ||
                possibleSuccInsert.length != nNode) {
            throw new IllegalArgumentException("The insertions array do not cover nNode elements");
        }

        // the number of required nodes is at least the number of member nodes
        assertTrue(seqVar.nRequired() >= seqVar.nMember());
        assertEquals(member.length, seqVar.nMember());
        assertEquals(required.length, seqVar.nRequired());
        assertEquals(possible.length, seqVar.nPossible());
        assertEquals(excluded.length, seqVar.nExcluded());
        assertEquals(seqVar.begin(), seqVar.nextMember(seqVar.end()));
        assertEquals(seqVar.end(), seqVar.predMember(seqVar.begin()));

        // tests the fill operations
        int[] actual;
        int[] expected;
        int[] values = new int[nNode];
        record fillTest(int[] array, Function<int[], Integer> f) {}
        for (fillTest test : new fillTest[] {
                new fillTest(required.clone(), seqVar::fillRequired),
                new fillTest(member.clone(), seqVar::fillMember),
                new fillTest(possible.clone(), seqVar::fillPossible),
                new fillTest(excluded.clone(), seqVar::fillExcluded),}) {
            assertEquals(test.array.length, (int) test.f.apply(values));
            actual = Arrays.copyOfRange(values, 0, test.array.length);
            Arrays.sort(actual);
            expected = test.array;
            Arrays.sort(expected);
            assertArrayEquals(expected, actual);
        }
        // test that a fillRequired without the member do not give any member
        int nExpected = required.length - member.length;
        assertEquals(nExpected, seqVar.fillRequired(values, false));
        assertEquals(nExpected, seqVar.nRequired(false));
        for (int i = 0 ; i < nExpected ; ++i) {
            assertTrue(requiredSet.contains(values[i]));
            assertFalse(memberSet.contains(values[i]));
        }

        // test the ordering
        int[] ordering = new int[member.length];
        assertEquals(member.length, seqVar.fillOrder(ordering, true));
        for (int i = 0 ; i < ordering.length ; ++i) {
            assertEquals(member[i], ordering[i]);
        }

        int[] insertions = IntStream.range(0, nNode).toArray();
        int pred = seqVar.end();
        for (int i: member) {
            assertTrue(seqVar.isMember(i));
            assertFalse(seqVar.isPossible(i));
            assertFalse(seqVar.isExcluded(i));
            // a member node has no predecessor / successor insertions that are member nodes
            assertEquals(0, seqVar.fillMemberPredInsert(i, insertions));
            assertEquals(0, seqVar.nMemberPredInsert(i));
            assertEquals(0, seqVar.fillMemberSuccInsert(i, insertions));
            assertEquals(0, seqVar.nMemberSuccInsert(i));

            // test the possible predecessor insertions
            assertEquals(possiblePredInsert[i].length, seqVar.nPossiblePredInsert(i));
            assertEquals(possiblePredInsert[i].length, seqVar.fillPossiblePredInsert(i, insertions));
            actual = Arrays.copyOfRange(insertions, 0, possiblePredInsert[i].length);
            Arrays.sort(actual);
            expected = possiblePredInsert[i];
            Arrays.sort(expected);
            assertArrayEquals(expected, actual);
            for (int j = 0 ; j < possiblePredInsert[i].length ; ++j) {
                int n = insertions[j];
                assertTrue(seqVar.isPredInsert(insertions[j], i));
                assertTrue(seqVar.isPossible(n) || !seqVar.isMember(n) && seqVar.isRequired(n));
            }

            // test the possible successor insertions
            assertEquals(possibleSuccInsert[i].length, seqVar.nPossibleSuccInsert(i));
            assertEquals(possibleSuccInsert[i].length, seqVar.fillPossibleSuccInsert(i, insertions));
            actual = Arrays.copyOfRange(insertions, 0, possibleSuccInsert[i].length);
            Arrays.sort(actual);
            expected = possibleSuccInsert[i];
            Arrays.sort(expected);
            assertArrayEquals(expected, actual);
            for (int j = 0 ; j < possibleSuccInsert[i].length ; ++j) {
                int n = insertions[j];
                assertTrue(seqVar.isPredInsert(i, n));
                assertTrue(seqVar.isPossible(n) || !seqVar.isMember(n) && seqVar.isRequired(n));
            }

            assertEquals(i, seqVar.nextMember(pred));
            assertEquals(pred, seqVar.predMember(i));
            pred = i;
        }
        for (int i: possible) {
            assertFalse(seqVar.isMember(i));
            assertTrue(seqVar.isPossible(i));
            assertFalse(seqVar.isExcluded(i));

            assertEquals(memberPredInsert[i].length, seqVar.nMemberPredInsert(i));
            assertEquals(memberPredInsert[i].length, seqVar.fillMemberPredInsert(i, insertions));
            actual = Arrays.copyOfRange(insertions, 0, memberPredInsert[i].length);
            Arrays.sort(actual);
            expected = memberPredInsert[i];
            Arrays.sort(expected);
            assertArrayEquals(expected, actual);
            for (int j = 0 ; j < memberPredInsert[i].length ; ++j) {
                int n = insertions[j];
                assertTrue(seqVar.isPredInsert(insertions[j], i));
                assertTrue(seqVar.isMember(insertions[j]));
            }

            assertEquals(possiblePredInsert[i].length, seqVar.nPossiblePredInsert(i));
            assertEquals(possiblePredInsert[i].length, seqVar.fillPossiblePredInsert(i, insertions));
            actual = Arrays.copyOfRange(insertions, 0, possiblePredInsert[i].length);
            Arrays.sort(actual);
            expected = possiblePredInsert[i];
            Arrays.sort(expected);
            assertArrayEquals(expected, actual);
            for (int j = 0 ; j < possiblePredInsert[i].length ; ++j) {
                int n = insertions[j];
                assertTrue(seqVar.isPredInsert(n, i));
                assertTrue(seqVar.isPossible(n) || !seqVar.isMember(n) && seqVar.isRequired(n));
            }

            // test the possible successor insertions
            assertEquals(possibleSuccInsert[i].length, seqVar.nPossibleSuccInsert(i));
            assertEquals(possibleSuccInsert[i].length, seqVar.fillPossibleSuccInsert(i, insertions));
            actual = Arrays.copyOfRange(insertions, 0, possibleSuccInsert[i].length);
            Arrays.sort(actual);
            expected = possibleSuccInsert[i];
            Arrays.sort(expected);
            assertArrayEquals(expected, actual);
            for (int j = 0 ; j < possibleSuccInsert[i].length ; ++j) {
                int n = insertions[j];
                assertTrue(seqVar.isPredInsert(i, n));
                assertTrue(seqVar.isPossible(n) || !seqVar.isMember(n) && seqVar.isRequired(n));
            }

            // test the member successor insertions
            assertEquals(memberSuccInsert[i].length, seqVar.nMemberSuccInsert(i));
            assertEquals(memberSuccInsert[i].length, seqVar.fillMemberSuccInsert(i, insertions));
            actual = Arrays.copyOfRange(insertions, 0, memberSuccInsert[i].length);
            Arrays.sort(actual);
            expected = memberSuccInsert[i];
            Arrays.sort(expected);
            assertArrayEquals(expected, actual);
            for (int j = 0 ; j < memberSuccInsert[i].length ; ++j) {
                int n = insertions[j];
                assertTrue(seqVar.isPredInsert(i, n));
                assertTrue(seqVar.isMember(n));
            }

        }
        for (int i: excluded) {
            assertFalse(seqVar.isMember(i));
            assertFalse(seqVar.isPossible(i));
            assertTrue(seqVar.isExcluded(i));
        }
        assertEquals(member.length + excluded.length == nNode, seqVar.isFixed());
    }

    /**
     * Asserts that a {@link SeqVar} corresponds to the expected arrays
     *
     * @param seqVar {@link SeqVar} to test
     * @param member member nodes, ordered by appearance in the sequence, omitting begin and end node
     * @param possible possible nodes of the sequence
     * @param excluded excluded nodes of the sequence
     * @param memberPredInsert member predecessor insertions of each {@link InsertVar}.
     *                         First indexing = id of the variable. Must contain the beginning node if present
     * @param possiblePredInsert possible predecessor insertions of each {@link InsertVar}.
     *                         First indexing = id of the variable. Must contain the beginning node if present
     */
    public static void assertGraphSeqValid(SeqVar seqVar, int[] member, int[] possible, int[] excluded,
                                           int[][] memberPredInsert, int[][] possiblePredInsert) {
        assertGraphSeqValid(seqVar, member, null, possible, excluded,
                memberPredInsert, possiblePredInsert);
    }

    /**
     * Asserts that a {@link SeqVar} corresponds to the expected arrays
     *
     * @param seqVar {@link SeqVar} to test
     * @param member member nodes, ordered by appearance in the sequence, omitting begin and end node
     * @param required required nodes from the sequence. If null, assumes that the required nodes are the member nodes
     * @param possible possible nodes of the sequence
     * @param excluded excluded nodes of the sequence
     * @param memberPredInsert member predecessor insertions of each {@link InsertVar}.
     *                         First indexing = id of the variable. Must contain the beginning node if present
     * @param possiblePredInsert possible predecessor insertions of each {@link InsertVar}.
     *                         First indexing = id of the variable. Must contain the beginning node if present
     */
    public static void assertGraphSeqValid(SeqVar seqVar, int[] member, int[] required, int[] possible, int[] excluded,
                                           int[][] memberPredInsert, int[][] possiblePredInsert) {
        // initialize the successors array and call the full test
        if (required == null)
            required = member.clone();
        Set<Integer> memberSet = Arrays.stream(member).boxed().collect(Collectors.toSet());
        int nNode = seqVar.nNode();
        ArrayList<Integer>[] memberSuccInsertList = new ArrayList[nNode];
        ArrayList<Integer>[] possibleSuccInsertList = new ArrayList[nNode];
        for (int i = 0 ; i < nNode ; ++i) {
            memberSuccInsertList[i] = new ArrayList<>();
            possibleSuccInsertList[i] = new ArrayList<>();
        }
        for (int i : possible) {
            // if j is a predecessor for i, then i is a successor for j
            for (int j : memberPredInsert[i])
                possibleSuccInsertList[j].add(i); // i is possible and j is member
            for (int j : possiblePredInsert[i])
                possibleSuccInsertList[j].add(i);
        }
        /*
        for (int i : member) {
            // if j is a predecessor for i, then i is a successor for j
            for (int j : possiblePredInsert[i])
                memberSuccInsertList[j].add(i); // i is member and j is possible
        }

         */
        for (int i : required) {
            // if j is a predecessor for i, then i is a successor for j
            if (!memberSet.contains(i)) {
                for (int j : possiblePredInsert[i])
                    possibleSuccInsertList[j].add(i);
                for (int j : memberPredInsert[i])
                    possibleSuccInsertList[j].add(i); // i is required and j is member
            } else {
                for (int j : possiblePredInsert[i])
                    memberSuccInsertList[j].add(i); // i is member and j is possible
            }
        }
        int[][] memberSuccInsert = new int[nNode][];
        int[][] possibleSuccInsert = new int[nNode][];
        for (int i = 0 ; i < nNode ; ++i) {
            memberSuccInsert[i] = memberSuccInsertList[i].stream().mapToInt(j -> j).toArray();
            possibleSuccInsert[i] = possibleSuccInsertList[i].stream().mapToInt(j -> j).toArray();
        }
        assertGraphSeqValid(seqVar, member, required, possible, excluded,
                memberPredInsert, possiblePredInsert, memberSuccInsert, possibleSuccInsert);
    }


    /**
     * Asserts that a {@link SeqVar} corresponds to the expected arrays
     * Considers that no individual insertion has been removed outside the ones related to the exclusion of nodes
     *
     * @param member member nodes of the sequence, ordered by appearance in the sequence, omitting begin and end node
     * @param possible possible nodes of the sequence
     * @param excluded excluded nodes of the sequence
     */
    public static void assertGraphSeqValid(SeqVar seqVar, int[] member, int[] possible, int[] excluded) {
        assertGraphSeqValid(seqVar, member, null, possible, excluded);
    }

    /**
     * Asserts that a {@link SeqVar} corresponds to the expected arrays
     * Considers that no individual insertion has been removed outside the ones related to the exclusion of nodes
     *
     * @param member member nodes of the sequence, ordered by appearance in the sequence, omitting begin and end node
     * @param required required nodes from the sequence. If null, assumes that the required nodes are the member nodes
     * @param possible possible nodes of the sequence
     * @param excluded excluded nodes of the sequence
     */
    public static void assertGraphSeqValid(SeqVar seqVar, int[] member, int[] required, int[] possible, int[] excluded) {
        if (required == null)
            required = member.clone();
        Set<Integer> memberSet = Arrays.stream(member).boxed().collect(Collectors.toSet());
        int[] requiredNotMember = Arrays.stream(required).filter(i -> !memberSet.contains(i)).toArray();
        int[][] memberPredInsert = new int[seqVar.nNode()][];
        int[][] possiblePredInsert = new int[seqVar.nNode()][];
        int[] memberPred = Arrays.stream(member).filter(i -> i != seqVar.end()).toArray();
        for (int i : member) {
            memberPredInsert[i] = new int[0];
            possiblePredInsert[i] = i == seqVar.begin() ? new int[0] : IntStream.concat(Arrays.stream(possible), Arrays.stream(requiredNotMember)).toArray();
        }
        for (int i : possible) {
            memberPredInsert[i] = memberPred;
            possiblePredInsert[i] = IntStream.concat(Arrays.stream(possible), Arrays.stream(requiredNotMember))
                    .filter(j -> j != i).toArray();;
        }
        for (int i : required) {
            if (memberPredInsert[i] != null)
                continue; // the node is a member and its predecessors have already been set
            memberPredInsert[i] = memberPred;
            possiblePredInsert[i] = IntStream.concat(Arrays.stream(possible), Arrays.stream(requiredNotMember))
                    .filter(j -> j != i).toArray();;
        }
        for (int i : excluded) {
            memberPredInsert[i] = new int[0];
            possiblePredInsert[i] = new int[0];
        }
        assertGraphSeqValid(seqVar, member, required, possible, excluded, memberPredInsert, possiblePredInsert);
    }

}
