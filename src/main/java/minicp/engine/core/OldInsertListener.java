package minicp.engine.core;

public interface OldInsertListener {

    /**
     * Called whenever the related {@link OldInsertVar} has been inserted to one point
     */
    void insert();

    /**
     * Called whenever the related {@link OldInsertVar} has been removed from its corresponding {@link OldSeqVar}
     */
    void exclude();

    /**
     * Called whenever the number of insertion related to this {@link OldInsertVar} has changed
     */
    void predChange();

}
