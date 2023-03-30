package minicp.engine.core;

import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;

/**
 * Modification of {@link OldSeqVar} where successors insertions are stored
 * in addition to predecessors insertions
 *
 * Moreover, a new operation can change the state:
 *   - Requiring nodes {@link SeqVar#require(int)}
 *
 * The insertions can be removed through {@link OldSeqVar#removePredInsert(int, int)},
 * removing both the predecessor and the successor
 */
public interface SeqVar extends OldSeqVar {

    /* ============================== successor insertions operations ============================== */

    /**
     * Copies the member successor insertions values of the domain into an array.
     *
     * @param node node whose member insertions (successor candidate) needs to be known
     * @param dest an array large enough {@code dest.length >= nMemberSuccInsert(node)}
     * @return the size of the member insertion domain and {@code dest[0,...,size-1]} contains
     *         the values in the member insertion domain in an arbitrary order
     */
    int fillMemberSuccInsert(int node, int[] dest);

    /**
     * Copies the possible successor insertions values of the domain into an array.
     *
     * @param node node whose possible insertions (successor candidate) needs to be known
     * @param dest an array large enough {@code dest.length >= nPossibleSuccInsert(node)}
     * @return the size of the possible insertion domain and {@code dest[0,...,size-1]} contains
     *         the values in the possible insertion domain in an arbitrary order
     */
    int fillPossibleSuccInsert(int node, int[] dest);

    /**
     * Copies the successor insertions values of the domain into an array.
     *
     * @param node node whose insertions (successor candidate) needs to be known
     * @param dest an array large enough {@code dest.length >= nSuccInsert(node)}
     * @return the size of the insertion domain and {@code dest[0,...,size-1]} contains
     *         the values in the insertion domain in an arbitrary order
     */
    int fillSuccInsert(int node, int[] dest);

    /**
     * Gives the number of successor insertions that are member nodes
     *
     * @param node node from which the member successor insertions are related to
     * @return number of successor insertions that are member nodes
     */
    int nMemberSuccInsert(int node);

    /**
     * Gives the number of successor insertions that are possible nodes
     *
     * @param node node from which the possible successor insertions are related to
     * @return number of successor insertions that are possible nodes
     */
    int nPossibleSuccInsert(int node);

    /**
     * Gives the number of successor insertions
     *
     * @param node node from which the successor insertions are related to
     * @return number of successor insertions
     */
    int nSuccInsert(int node);

    /* ============================== require operations ============================== */

    /**
     * Sets the node as required
     *
     * @param node node whose status needs to change
     * @throws InconsistencyException if the node cannot be required
     */
    void require(int node);

    /**
     * Copies the required nodes into an array.
     * The copied values always contain the member nodes
     *
     * @param dest an array large enough {@code dest.length >= nRequired(true)}
     * @return the size of the required domain and {@code dest[0,...,size-1]} contains
     *         the values in the required domain in an arbitrary order.
     */
    int fillRequired(int[] dest);

    /**
     * Copies the required nodes into an array.
     * The copied values might contain the member nodes as well, depending on {@code includeMember}
     *
     * @param dest an array large enough {@code dest.length >= nRequired(true)}
     * @param includeMember if true, includes the member nodes within the array
     * @return the size of the required domain and {@code dest[0,...,size-1]} contains
     *         the values in the required domain in an arbitrary order.
     */
    int fillRequired(int[] dest, boolean includeMember);

    /**
     * Returns the number of required elements
     *
     * @return number of required nodes in the sequence, including the member nodes
     */
    int nRequired();

    /**
     * Returns the number of required elements
     * The number might include the member nodes as well, depending on {@code includeMember}
     *
     * @param includeMember if true, counts the member nodes
     * @return number of required nodes in the sequence, including the member nodes
     */
    int nRequired(boolean includeMember);

    /**
     * Tells if a node is required in the sequence.
     *
     * @param node the node whose state needs to be known.
     * @return true if the node is required the sequence.
     */
    boolean isRequired(int node);

    /**
     * Sets all possible nodes as required
     * After this operation, excluding a possible node results in an {@link InconsistencyException}
     */
    void requireAllPossible();

    /**
     * Tells if all nodes from the sequence are required
     * If this is the case, excluding a single node would result in an {@link InconsistencyException}
     *
     * @return true if all nodes from the sequence are required
     */
    boolean areAllPossibleRequired();

    /**
     * Gives the {@link InsertVar} related to a node in the sequence.
     *
     * @param i id of the InsertionVar
     * @return {@link InsertVar} with id i
     */
    InsertVar getInsertionVar(int i);

    /**
     * Asks that the closure is called whenever
     * a new node is required into the sequence.
     *
     * @param f the closure
     */
    void whenRequire(Procedure f);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever
     * a new node is required into the sequence
     * In such a state the variable is required and we say that a <i>require</i> event occurs.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on require events of this variable.
     */
    void propagateOnRequire(Constraint c);

}
