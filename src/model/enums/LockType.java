package model.enums;

/**
 * 4 co che dong bo hoa bat buoc de so sanh trong Simulator.
 */
public enum LockType {
    NO_LOCK,
    FILE_LOCK,
    SYNCHRONIZED,
    OPTIMISTIC_LOCK;

    public boolean protectsStock() {
        return this != NO_LOCK;
    }
}

