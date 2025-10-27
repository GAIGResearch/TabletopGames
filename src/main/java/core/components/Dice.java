package core.components;

import java.util.*;

import core.CoreConstants;
import org.json.simple.JSONObject;
import utilities.JSONUtils;

import static core.components.Dice.Type.*;

public class Dice extends Component {
    public enum Type{
        d3(3),
        d4(4),
        d6(6),
        d8(8),
        d10(10),
        d12(12),
        d20(20),
        dCustom(-1);
        public final int nSides;
        Type(int nSides) {
            this.nSides = nSides;
        }
        static Type sidesToType(int nSides) {
            for (Type t: values()) if (t.nSides == nSides) return t;
            return dCustom;
        }
    }

    public final Type type;
    public final int nSides; // Number of sides
    protected int value;  // Current value after last roll
    private double[] pdf = null;

    public Dice() {
        this(d6);  // By default d6
    }

    public Dice(Type type) {
        super(CoreConstants.ComponentType.DICE);
        this.type = type;
        this.nSides = type.nSides;
    }
    public Dice(int nSides) {
        super(CoreConstants.ComponentType.DICE);
        this.nSides = nSides;
        this.type = Type.sidesToType(nSides);
    }
    public Dice(String json) {
        super(CoreConstants.ComponentType.DICE);
        JSONObject data = JSONUtils.loadJSONFile(json);
        this.nSides = ((Long) data.get("nSides")).intValue();
        this.type = Type.sidesToType(nSides);
        Object pdfObj = data.get("pdf");
        if (pdfObj != null) {
            org.json.simple.JSONArray arr = (org.json.simple.JSONArray) pdfObj;
            pdf = new double[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                pdf[i] = ((Number) arr.get(i)).doubleValue();
            }
            double total = Arrays.stream(pdf).sum();
            if (Math.abs(total - 1.0) > 0.000001)
                throw new IllegalArgumentException("Invalid PDF in Dice: " + Arrays.toString(pdf));
        }
    }

    private Dice(Type type, int nSides, int value, int ID) {
        super(CoreConstants.ComponentType.DICE, ID);
        this.type = type;
        this.nSides = nSides;
        this.value = value;
    }

    /**
     * @return current value shown by this die
     */
    public int getValue() {
        return value;
    }

    public void setValue(int v) {
        if (v > nSides || v < 1)
            throw new IllegalArgumentException("Invalid number for die : " + v);
        value = v;
    }

    /**
     * @return number of sides for this die.
     */
    public int getNumberOfSides() {
        return this.nSides;
    }

    /**
     * Rolls the die and returns result for roll in range [1, nSides].
     *
     * @param r - random generator.
     */
    public void roll(Random r) {
        if (pdf == null) {
            value = r.nextInt(this.nSides) + 1;
        } else {
            double p = r.nextDouble();
            double cumulative = 0.0;
            for (int i = 0; i < pdf.length; i++) {
                cumulative += pdf[i];
                if (p <= cumulative) {
                    value = i + 1;
                    return;
                }
            }
            // Should never get here
            throw new AssertionError("Error when rolling custom die, no value selected");
        }
    }

    @Override
    public Dice copy() {
        Dice copy = new Dice(type, nSides, value, componentID);
        if (pdf != null) copy.pdf = Arrays.copyOf(pdf, pdf.length);
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public final int hashCode() {
        return componentID;
    }
}
