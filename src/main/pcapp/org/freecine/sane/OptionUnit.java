package org.freecine.sane;


public enum OptionUnit {

    SANE_UNIT_NONE, 
    SANE_UNIT_PIXEL, 
    SANE_UNIT_BIT, 
    SANE_UNIT_MM, 
    SANE_UNIT_DPI, 
    SANE_UNIT_PERCENT, 
    SANE_UNIT_MICROSECOND;
    
    public static OptionUnit get( int n ) {
        return values()[n];
    }
}
