package evaluation.optimisation.ntbea;

import evaluation.optimisation.ITPSearchSpace;
import utilities.JSONUtils;

import java.io.File;
import java.util.*;

public abstract class AgentSearchSpace<T> implements SearchSpace {

    protected List<String> searchDimensions;
    protected List<? extends Class<?>> dimensionTypes;
    protected List<List<Object>> values;

    public void initialise(List<ITPSearchSpace.ParameterSettings> parameterDetails) {
        searchDimensions = parameterDetails.stream().map(ITPSearchSpace.ParameterSettings::name).toList();
        dimensionTypes = parameterDetails.stream().map(ITPSearchSpace.ParameterSettings::clazz).toList();
        values = new ArrayList<>();
        for (int i = 0; i < searchDimensions.size(); i++) {
            Class<?> clazz = dimensionTypes.get(i);
            List<Object> s = new ArrayList<>();
            for (int j = 0; j < parameterDetails.get(i).values().size(); j++) {
                Object setting = parameterDetails.get(i).values().get(j);
                // account for special case that this is a json file
                if (setting instanceof String str) {
                    if (str.endsWith(".json")) {
                        // we load from the file
                        setting = JSONUtils.loadClassFromFile(str);
                    }
                }
                if (setting.getClass() == Long.class) {
                    setting = ((Number) setting).intValue();
                }
                if (clazz == Double.class || clazz == double.class) {
                    setting = ((Number) setting).doubleValue();
                }
                if (!(clazz.isAssignableFrom(setting.getClass()))) {
                    throw new IllegalArgumentException("Unable to assign " + setting +
                            " to class " + clazz + " for " + searchDimensions.get(i));
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
