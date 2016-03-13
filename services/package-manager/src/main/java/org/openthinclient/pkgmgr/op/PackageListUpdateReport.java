package org.openthinclient.pkgmgr.op;

public class PackageListUpdateReport {

    /**
     * The number of packages that have been updated.
     */
    private int updated;
    /**
     * The number of packages that have been added.
     */
    private int added;

    /**
     * The number of packages that have been removed from the remote repository.
     */
    private int removed;

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public int getAdded() {
        return added;
    }

    public void setAdded(int added) {
        this.added = added;
    }

    public int getRemoved() {
        return removed;
    }

    public void setRemoved(int removed) {
        this.removed = removed;
    }

    public void incAdded() {
        added++;
    }

    public void incRemoved() {
        removed++;
    }

    public void intUpdated() {
        updated++;
    }
}
