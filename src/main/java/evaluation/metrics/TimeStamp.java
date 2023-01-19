package evaluation.metrics;

import utilities.StatSummary;

import java.util.ArrayList;

public class TimeStamp {

    public int x;
    public double v;

    public TimeStamp(int x, double value)
    {
        this.x = x;
        this.v = value;
    }


    public String toString()
    {
        return "[x: " + x + ", y: " + v + "]";
    }

}
