package evaluation.optimisation.ntbea;

import core.LLMTest;

import java.util.*;

public abstract class AgentSearchSpace<T> implements SearchSpace {

    protected List<String> searchDimensions;
    protected List<Class<?>> dimensionTypes;
    protected List<List<Object>> values;

    public void initialise(List<String> names, List<List<Object>> possibleSettings) {
        searchDimensions = names;
        dimensionTypes = new ArrayList<>();
        values = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            Class<?> expectedClass = possibleSettings.get(i).get(0).getClass();
            List<Object> s = new ArrayList<>();
            for (int j = 0; j < possibleSettings.get(i).size(); j++) {
                Object setting = possibleSettings.get(i).get(j);
                if (!(setting.getClass().isAssignableFrom(expectedClass))) {
                    throw new IllegalArgumentException("Unable to assign " + setting +
                            " to class " + expectedClass + " for " + names.get(i));
                }
                s.add(setting);
            }
            values.add(s);
        }
    }

    public abstract T instantiate(int[] settings);

    @Override
    public int nDims() {
        return searchDimensions.size();
    }

    @Override
    public int nValues(int i) {
        return values.get(i).size();
    }

    @Override
    public String name(int i) {
        return searchDimensions.get(i);
    }

    public List<String> getDimensions() {
        return searchDimensions;
    }

    @Override
    public Object value(int dim, int i) {
        return values.get(dim).get(i);
    }

    public List<Object> allValues(int dim) {
        return values.get(dim);
    }

    /**
     * converts the name of a parameter to its index position
     */
    public int getIndexOf(String parameter) {
        return searchDimensions.indexOf(parameter);
    }

}
