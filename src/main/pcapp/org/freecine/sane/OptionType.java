package org.freecine.sane;


public enum OptionType {

    SANE_TYPE_BOOL,
    SANE_TYPE_INT,
    SANE_TYPE_FIXED,
    SANE_TYPE_STRING,
    SANE_TYPE_BUTTON, 
    SANE_TYPE_GROUP;

    public static OptionType get( int n ) {
        return values()[n];
    }
}
