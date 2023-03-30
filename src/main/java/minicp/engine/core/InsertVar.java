package minicp.engine.core;

import minicp.util.Procedure;

public interface InsertVar extends OldInsertVar {

    /**
     * Tells if the successor insertion belongs to the set of insertions points
     *
     * @param i successor candidate for the beginning of the request.
     * @return True if the successor candidate is a valid insertion point
     */
    boolean isSucc(int i);

    /**
     * Removes an insertion point from the set of candidate insertions points.
     *
     * @param i successor candidate for the end of the request.
     */
    void removeSucc(int i);

    /**
     * Copies the values of the insertions points into an array.
     * each entry of the array contains a valid successor
     *
     * @param dest an array large enough {@code dest.length >= nSucc()}
     * @return the size of the domain and {@code dest[0,...,size-1]} contains
     *         the values in the domain in an arbitrary order
     */
    int fillSucc(int[] dest);

    /**
     * @return number of successor insertions points for the node
     */
    int nSucc();

    /**
     * Asks that the closure is called whenever the successor domain
     * of this variable changes
     *
     * @param f the closure
     */
    void whenSuccChange(Procedure f);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever the successor domain
     * of this variable changes.
     * We say that a <i>change</i> event occurs.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on change events of this variable.
     */
    void propagateOnSuccChange(Constraint c);

    /**
     * Asks that the closure is called whenever the variable becomes required
     *
     * @param f the closure
     */
    void whenRequire(Procedure f);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever the variable becomes required
     * We say that a <i>require</i> event occurs.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on require events of this variable.
     */
    void propagateOnRequire(Constraint c);
}
