package com.pasan.soilresistivity;

import org.json.JSONException;
import org.json.JSONObject;

public final class SurveyPoint {
    public final double mnHalf;
    public final double abHalf;
    public final double resistance;

    public SurveyPoint(double mnHalf, double abHalf, double resistance) {
        if (mnHalf <= 0 || abHalf <= mnHalf || resistance <= 0) {
            throw new IllegalArgumentException("Distances and resistance must be positive; AB/2 must exceed MN/2.");
        }
        this.mnHalf = mnHalf;
        this.abHalf = abHalf;
        this.resistance = resistance;
    }

    public double geometricFactor() {
        return Math.PI * (abHalf * abHalf - mnHalf * mnHalf) / (2.0 * mnHalf);
    }

    /** The imported/entered third column is apparent resistivity, not resistance. */
    public double apparentResistivity() { return resistance; }

    public double wennerSpacing() { return (2.0 * abHalf) / 3.0; }

    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("mnHalf", mnHalf).put("abHalf", abHalf).put("resistance", resistance);
    }

    public static SurveyPoint fromJson(JSONObject object) throws JSONException {
        return new SurveyPoint(object.getDouble("mnHalf"), object.getDouble("abHalf"), object.getDouble("resistance"));
    }
}
