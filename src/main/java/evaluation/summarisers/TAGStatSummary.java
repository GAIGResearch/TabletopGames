package evaluation.summarisers;

import java.util.Map;

public abstract class TAGStatSummary {
    public enum StatType {
        Numeric,
        Occurrence,
        Time
    }

    public String name; // defaults to ""
    public StatType type;
    protected int n;

    public TAGStatSummary(StatType type) {
        this("", type);
    }

    public TAGStatSummary(String name, StatType type) {
        this.name = name;
        this.type = type;
        reset();
    }

    public void reset() {
        n = 0;
    }

    public int n() {
        return n;
    }

    public void add(TAGStatSummary ss) {
        this.n += ss.n;
    }

    public abstract Object getElements();
    public abstract TAGStatSummary copy();
    public abstract Map<String, Object> getSummary();

    public static TAGStatSummary construct(StatType type) {
        switch(type) {
            case Numeric: return new TAGNumericStatSummary();
            case Occurrence: return new TAGOccurrenceStatSummary();
        }
        return null;
    }

    public static TAGStatSummary construct(String name, StatType type) {
        switch(type) {
            case Numeric: return new TAGNumericStatSummary(name);
            case Occurrence: return new TAGOccurrenceStatSummary(name);
        }
        return null;
    }
}
