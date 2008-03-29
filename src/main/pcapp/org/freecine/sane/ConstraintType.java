package org.freecine.sane;


public enum ConstraintType {

    SANE_CONSTRAINT_NONE, 
    SANE_CONSTRAINT_RANGE, 
    SANE_CONSTRAINT_WORD_LIST, 
    SANE_CONSTRAINT_STRING_LIST;
    
    public static ConstraintType get( int n ) {
        return values()[n];
    }
}

