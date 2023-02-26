package evaluation.summarisers;

import utilities.StatSummary;
import utilities.TimeStamp;

import java.util.ArrayList;

public class TimeStampSummary extends TimeStamp {

    public StatSummary values;

    public TimeStampSummary(int x, ArrayList<Double> values)
    {
        super(x, Double.NaN);
        this.values = new StatSummary();
        for(Double d : values) this.values.add(d);
    }

    public String toString()
    {
        return "[x: " + x + ", y: (" + values + ")]";
    }
}
