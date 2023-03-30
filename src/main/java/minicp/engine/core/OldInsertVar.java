package minicp.engine.core;

import minicp.util.Procedure;

/**
 * This variable represents the set of nodes after which
 * this node can be inserted.
 *
 * An insertion point is defined by an integer i
 * and is supposed to be valid if this node can be inserted after node i.
 * Invalid insertion points are assumed to be removed by the constraints.
 * A {@link OldInsertVar} can be empty domain and still be valid.
 * However, a constraint can be added to throw an inconsistency whenever an InsertionVar is empty.
 */
public interface OldInsertVar {

    /**
     * Returns the solver in which this variable was created.
     *
     * @return the solver in which this variable was created
     */
    Solver getSolver();

    /**
     * Removes an insertion point from the set of candidate insertions points.
     *
     * @param i predecessor candidate for the beginning of the request.
     */
    void removePred(int i);

    /**
     * Tells if the predecessor insertion belongs to the set of insertions points
     *
     * @param i predecessor candidate for the beginning of the request.
     * @return True if the predecessor candidate is a valid insertion point
     */
    boolean isPred(int i);

    /**
     * Returns the id of the node
     *
     * @return id of the node
     */
    int node();

    /**
     * Copies the values of the insertions points into an array.
     * each entry of the array contains a valid predecessor
     *
     * @param dest an array large enough {@code dest.length >= nPred()}
     * @return the size of the domain and {@code dest[0,...,size-1]} contains
     *         the values in the domain in an arbitrary order
     */
    int fillPred(int[] dest);

    /**
     * Returns the number of predecessor insertions points for the node
     *
     * @return number of predecessor insertions points for the node
     */
    int nPred();

    /**
     * Asks that the closure is called whenever the domain
     * of this variable is reduced to a single setValue
     *
     * @param f the closure
     */
    void whenInsert(Procedure f);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever the domain
     * of this variable is reduced to a singleton.
     * In such a state the variable is fixed.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on insert events of this variable.
     */
    void propagateOnInsert(Constraint c);

    /**
     * Asks that the closure is called whenever the predecessor domain
     * of this variable changes
     *
     * @param f the closure
     */
    void whenPredChange(Procedure f);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever the predecessor domain
     * of this variable changes.
     * We say that a <i>change</i> event occurs.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on change events of this variable.
     */
    void propagateOnPredChange(Constraint c);

    /**
     * Asks that the closure is called whenever the insertion point is excluded
     *
     * @param f the closure
     */
    void whenExclude(Procedure f);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever the insertion point is excluded
     * We say that a <i>exclude</i> event occurs.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on exclude events of this variable.
     */
    void propagateOnExclude(Constraint c);

    /**
     * Asks that the closure is called whenever no inserted point remained for this variable
     * this occurs when the insertion variable is either inserted or excluded
     *
     * @param f the closure
     */
    void whenFixed(Procedure f);

    /**
     * Asks that {@link Constraint#propagate()} to be called when
     * no inserted point remains for this variable.
     * This occurs when the insertion variable is either inserted or excluded.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on fix of this variable.
     */
    void propagateOnFix(Constraint c);

}
