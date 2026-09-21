package com.pasan.soilresistivity;

public final class LayerModel {
    public final double[] resistivity, thickness, calculated;
    public final double errorPercent;
    public LayerModel(double[] resistivity, double[] thickness, double[] calculated, double errorPercent) {
        this.resistivity=resistivity; this.thickness=thickness; this.calculated=calculated; this.errorPercent=errorPercent;
    }
    public double depthToLayer(int layer) { double d=0; for(int i=0;i<layer&&i<thickness.length;i++) d+=thickness[i]; return d; }
}
