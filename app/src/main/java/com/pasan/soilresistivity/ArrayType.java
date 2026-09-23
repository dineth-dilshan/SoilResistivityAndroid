package com.pasan.soilresistivity;

public enum ArrayType {
    WENNER("Wenner (alpha)", "a (m)", "a = AB/3 spacing (m)"),
    SCHLUMBERGER("Schlumberger", "AB/2 (m)", "AB/2 spacing (m)");

    public final String label, spacingHint, axisLabel;
    ArrayType(String label, String spacingHint, String axisLabel) {
        this.label=label; this.spacingHint=spacingHint; this.axisLabel=axisLabel;
    }
}
