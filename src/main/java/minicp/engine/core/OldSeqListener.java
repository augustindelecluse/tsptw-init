package minicp.engine.core;

public interface OldSeqListener {

    /**
     * Called whenever no possible node remains
     */
    void fix();

    /**
     * Called whenever a possible node has been inserted into the sequence
     */
    void insert();

    /**
     * Called whenever a possible node has been removed from the sequence
     */
    void exclude();
}
