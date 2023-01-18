package evaluation.summarisers;

import utilities.TimeStamp;

import java.util.*;

public class TAGTimeSeriesSummary extends TAGStatSummary{

    private ArrayList<TimeStamp> series;

    public TAGTimeSeriesSummary() {
        this("");
    }

    public TAGTimeSeriesSummary(String name) {
        super(name, StatType.Time);
    }

    public void reset() {
        super.reset();
        series = new ArrayList<>();
    }

    public void append(TimeStamp ktp) {
        append(ktp.x, ktp.v);
    }

    public void append(int x, double v) {
        series.add(new TimeStamp(x, v));
        n++;
    }

    public void append(TimeStampSummary ktp) {
        append(ktp.x, ktp.values.getElements());
    }

    public void append(int x, ArrayList<Double> values) {
        series.add(new TimeStampSummary(x, values));
        n++;
    }

    @Override
    public String toString() {
        String s = (name == null) ? "" : (name + "\n");
        s += " n     = " + n + "\n";
        return s;
    }

    @Override
    public Object getElements() {
        return series;
    }

    @Override
    public TAGTimeSeriesSummary copy() {
        TAGTimeSeriesSummary ss = new TAGTimeSeriesSummary(this.name);
        ss.series.addAll(series);
        return ss;
    }

    @Override
    public HashMap<String, Object> getSummary() {
        HashMap<String, Object> data = new HashMap<>();
        data.put(name, processValues(series));
        return data;
    }

    private Object processValues(ArrayList<TimeStamp> values)
    {
        if(values.size() <= 1) return values;

        //Check if this needs aggregation. We need to assume uniformity from the first entry.
        TimeStamp k0 = values.get(0);
        TimeStamp k1 = values.get(1);
        if(k0.x != k1.x) return values;

        //We need to aggregate per x value
        ArrayList<TimeStampSummary> all = new ArrayList<>();
        ArrayList<Double> yData = new ArrayList<>();
        int currX = -1;
        for(int i = 0; i < values.size(); i++)
        {
            TimeStamp next = values.get(i);
            if(yData.size() == 0)
            {
                currX = next.x;
                yData.add(next.v);
            }else if(currX == next.x){
                yData.add(next.v);
                if(i == values.size()-1)
                    all.add(new TimeStampSummary(currX, yData));
            }else{
                //change of X
                all.add(new TimeStampSummary(currX, yData));
                currX = next.x;
                yData.clear();
                yData.add(next.v);
            }
        }
        return all;
    }

}
