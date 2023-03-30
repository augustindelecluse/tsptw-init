package minicp.engine.core;

import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;

/**
 * Decision variable used to represent a sequence of nodes
 * As an invariant of the domain, the nodes are partitioned into 3 categories:
 *  - member ones, which are part of the sequence and ordered
 *  - possible ones, which could be part of the sequence
 *  - excluded ones, which cannot be part of the sequence
 *
 * The constraints pruning a {@link OldSeqVar}
 * does it by remove insertions points from
 * the {@link OldInsertVar} contained within the sequence or
 * by inserting or excluding them.
 */
public interface OldSeqVar {

    /**
     * Returns the solver in which this variable was created.
     *
     * @return the solver in which this variable was created
     */
    Solver getSolver();

    /**
     * Returns the first node of the sequence.
     *
     * @return first node of the sequence.
     */
    int begin();

    /**
     * Returns the last node of the sequence
     *
     * @return last node of the sequence
     */
    int end();

    /**
     * Returns the number of elements already in the partial sequence
     *
     * @return number of member nodes in the sequence, including the {@link #begin()} and {@link #end()} nodes
     */
    int nMember();

    /**
     * Returns the number of elements already in the partial sequence, possibly excluding
     * the count of {@link #begin()} and {@link #end()}.
     *
     * @param includeBounds whether to count the {@link #begin()} and {@link #end()} nodes or not
     * @return number of member nodes
     */
    int nMember(boolean includeBounds);

    /**
     * Returns the number of possible elements that can be added to the partial sequence.
     *
     * @return number of possible nodes in the sequence
     */
    int nPossible();

    /**
     * Returns the number of excluded nudes in the sequence.
     *
     * @return number of excluded nodes in the sequence
     */
    int nExcluded();

    /**
     * Returns the number of nodes in the sequence.
     *
     * @return number of nodes in the sequence,
     *         including the {@link #begin()} and {@link #end()} nodes
     */
    int nNode();

    /**
     * Tells if a node is a member of the sequence.
     *
     * @param node the node whose state needs to be known.
     * @return true if the node is a member of the sequence.
     */
    boolean isMember(int node);

    /**
     * Tells if a node is a possible one.
     *
     * @param node node whose state needs to be known.
     * @return true if the node is possible.
     */
    boolean isPossible(int node);

    /**
     * Tells if a node is excluded.
     *
     * @param node node whose state needs to be known.
     * @return true if the node is excluded.
     */
    boolean isExcluded(int node);

    /**
     * Tells if the variable is fixed.
     *
     * @return true when no more node belongs to the set of possible nodes
     */
    boolean isFixed();

    /**
     * Returns the next member node in the sequence just after the one given in parameter.
     *
     * @param node node member of the sequence.
     * @return index of the successor of the node. Irrelevant if the node is not in the sequence
     */
    int nextMember(int node);

    /**
     * Returns the predecessor member node in the sequence just before the one given in parameter.
     *
     * @param node node member of the sequence.
     * @return index of the predecessor of the node. Irrelevant if the node is not in the sequence
     */
    int predMember(int node);

    /**
     * Fills the current order of the sequence into an array
     * including {@link #begin()} and {@link #end()} node.
     *
     * @param dest array where to store the order of the sequence. The array should be large enough.
     * @return number of elements in the sequence, including beginning and ending node
     */
    int fillOrder(int[] dest);

    /**
     * Fills the current order of the sequence into an array.
     *
     * @param dest array where to store the order of the sequence. The array should be large enough.
     * @param includeBounds if true, includes the beginning and ending node into the order array
     * @return number of elements in the sequence
     */
    int fillOrder(int[] dest, boolean includeBounds);

    /**
     * Inserts the node into the sequence, right after the given predecessor.
     *
     * @param pred predecessor for the node, a member of the sequence
     * @param node node to insert, a possible node
     * @throws InconsistencyException if {@code pred} is not a member
     *         or if {@code node} is not a possible node.
     */
    void insert(int pred, int node);

    /**
     * Tells if a node can be inserted with a given predecessor.
     *
     * @param pred predecessor for the node.
     * @param node node trying to be inserted.
     * @return true if the node can be inserted.
     */
    boolean canInsert(int pred, int node);

    /**
     * Excludes the node from the set of possible nodes.
     *
     * @param node node to exclude.
     * @throws InconsistencyException if {@code node} is a member of the partial sequence.
     */
    void exclude(int node);

    /**
     * Excludes all possible nodes from the sequence, fixing the variable.
     */
    void excludeAllPossible();

    /* ============================== fill operations ============================== */

    /**
     * Copies the member nodes into an array.
     * The copied values always contain the {@link #begin()} and {@link #end()} nodes
     *
     * @param dest an array large enough {@code dest.length >= NMember(true)}
     * @return the size of the member domain and {@code dest[0,...,size-1]} contains
     *         the values in the member domain in an arbitrary order.
     */
    int fillMember(int[] dest);

    /**
     * Copies the possible values of the domain into an array.
     *
     * @param dest an array large enough {@code dest.length >= nPossible()}
     * @return the size of the possible domain and {@code dest[0,...,size-1]} contains
     *         the values in the possible domain in an arbitrary order.
     */
    int fillPossible(int[] dest);

    /**
     * Copies the excluded values of the domain into an array.
     *
     * @param dest an array large enough {@code dest.length >= nExcluded()}
     * @return the size of the excluded domain and {@code dest[0,...,size-1]} contains
     *         the values in the excluded domain in an arbitrary order.
     */
    int fillExcluded(int[] dest);

    /**
     * Copies the member predecessor insertions values of the domain into an array.
     *
     * @param node node whose member insertions (predecessor candidate) needs to be known
     * @param dest an array large enough {@code dest.length >= nMemberPredInsert(node)}
     * @return the size of the member insertion domain and {@code dest[0,...,size-1]} contains
     *         the values in the member insertion domain in an arbitrary order
     */
    int fillMemberPredInsert(int node, int[] dest);

    /**
     * Copies the possible predecessor insertions values of the domain into an array.
     *
     * @param node node whose possible insertions (predecessor candidate) needs to be known
     * @param dest an array large enough {@code dest.length >= nPossiblePredInsert(node)}
     * @return the size of the possible insertion domain and {@code dest[0,...,size-1]} contains
     *         the values in the possible insertion domain in an arbitrary order
     */
    int fillPossiblePredInsert(int node, int[] dest);

    /**
     * Copies the predecessor insertions values of the domain into an array.
     *
     * @param node node whose insertions (predecessor candidate) needs to be known
     * @param dest an array large enough {@code dest.length >= nPredInsert(node)}
     * @return the size of the insertion domain and {@code dest[0,...,size-1]} contains
     *         the values in the insertion domain in an arbitrary order
     */
    int fillPredInsert(int node, int[] dest);

    /**
     * Gives the number of predecessor insertions that are member nodes
     *
     * @param node node from which the member predecessor insertions are related to
     * @return number of predecessor insertions that are member nodes
     */
    int nMemberPredInsert(int node);

    /**
     * Gives the number of predecessor insertions that are possible nodes
     *
     * @param node node from which the possible predecessor insertions are related to
     * @return number of predecessor insertions that are possible nodes
     */
    int nPossiblePredInsert(int node);

    /**
     * Gives the number of predecessor insertions
     *
     * @param node node from which the predecessor insertions are related to
     * @return number of predecessor insertions
     */
    int nPredInsert(int node);

    /* ============================== insertion checks and removal ============================== */

    /**
     * Tells if an insertion exists
     *
     * @param pred predecessor for the insertion
     * @param node node for which we need to tell if the insertion exists
     * @return true if the insertion exists
     */
    boolean isPredInsert(int pred, int node);

    /**
     * Removes an insertion
     *
     * @param pred predecessor for the insertion
     * @param node node whose insertion will be removed
     */
    void removePredInsert(int pred, int node);

    /**
     * Removes all insertions having the specified node as predecessor
     *
     * @param node node after which no insertion can occur.
     */
    void removeAllPredInsertFrom(int node);

    /**
     * Gives the {@link OldInsertVar} related to a node in the sequence.
     *
     * @param i id of the InsertionVar
     * @return {@link OldInsertVar} with id i
     */
    OldInsertVar getInsertionVar(int i);

    /* ============================== propagations operations ============================== */

    /**
     * Asks that the closure is called whenever the domain
     * of this variable is reduced to a single setValue.
     *
     * @param f the closure
     */
    void whenFix(Procedure f);

    /**
     * Asks that the closure is called whenever
     * a new node is inserted into the sequence.
     *
     * @param f the closure
     */
    void whenInsert(Procedure f);

    /**
     * Asks that the closure is called whenever
     * a new node is excluded from the sequence.
     *
     * @param f the closure
     */
    void whenExclude(Procedure f);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever the domain
     * of this variable is reduced to a singleton.
     * In such a state the variable is fixed and we say that a <i>fix</i> event occurs.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on fix events of this variable.
     */
    void propagateOnFix(Constraint c);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever
     * a new node is inserted into the sequence
     * We say that an <i>insert</i> event occurs in this case.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on insert events of this variable.
     */
    void propagateOnInsert(Constraint c);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever
     * a new node is excluded from the sequence
     * We say that an <i>exclude change</i> event occurs in this case.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on exclude change events of this variable.
     */
    void propagateOnExclude(Constraint c);

    /* ============================== String representations ============================== */

    /**
     * Gives a string representation of the ordering of nodes with the {@link #begin()} and {@link #end()} nodes.
     *
     * @return ordering of the sequence, with " -> " between 2 consecutive nodes, incudling the {@link #begin()} and
     *  {@link #end()} nodes
     */
    String ordering();

    /**
     * Gives a string representation of the ordering of nodes with possibly the {@link #begin()} and {@link #end()} nodes.
     *
     * @param includeBounds if the bounds ({@link #begin()} and {@link #end()}) must be included or not
     * @return ordering of the sequence, with " -> " between 2 consecutive nodes
     */
    String ordering(boolean includeBounds);

    /**
     * Gives a string representation of ordering of nodes with the beginning and end nodes.
     *
     * @param includeBounds if the bounds ({@link #begin()} and {@link #end()}) must be included or not
     * @param join string that must be used to join two consecutive nodes
     * @return ordering of the sequence, nodes being joined on the specified string
     */
    String ordering(boolean includeBounds, String join);

}
