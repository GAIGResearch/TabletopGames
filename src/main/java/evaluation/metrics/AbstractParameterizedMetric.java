package evaluation.metrics;

import utilities.Group;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Subclasses should implement matching constructors depending on how many parameters they have (1 or 2+).
// Always include the default constructor. Always call appropriate super constructor.
public abstract class AbstractParameterizedMetric extends AbstractMetric
{
    // Mapping from parameter name to parameter value
    private final Map<String, Object> parameterValues = new HashMap<>();

    public AbstractParameterizedMetric() {
        super();
        List<Group<String, List<?>, ?>> parameters = getAllowedParameters();
        IntStream.range(0, parameters.size()).filter(i -> !parameters.get(i).b.contains(parameters.get(i).c)).forEach(i -> {
            throw new RuntimeException("Declared default parameter " + i + " value (" + parameters.get(i).c + " is not one of the possible values for this parameter: " + parameters.get(i).b);
        });
        parameters.forEach(p -> parameterValues.put(p.a, p.c));  // Use default value for this parameter
    }
    public AbstractParameterizedMetric(Object arg) {
        super();
        List<Group<String, List<?>, ?>> parameters = getAllowedParameters();
        IntStream.range(0, parameters.size()).filter(i -> !parameters.get(i).b.contains(parameters.get(i).c)).forEach(i -> {
            throw new RuntimeException("Declared default parameter " + i + " value (" + parameters.get(i).c + " is not one of the possible values for this parameter: " + parameters.get(i).b);
        });
        if (parameters.size() != 1) throw new RuntimeException("Number of parameters supplied doesn't match number of parameters recorded");
        if (!parameters.get(0).b.contains(arg)) throw new RuntimeException("Given argument (" + arg + ") is not one of the possible values: " + parameters.get(0).b.toString());
        parameterValues.put(parameters.get(0).a, arg);
    }
    public AbstractParameterizedMetric(Object... args) {
        super();
        List<Group<String, List<?>, ?>> parameters = getAllowedParameters();
        IntStream.range(0, parameters.size()).filter(i -> !parameters.get(i).b.contains(parameters.get(i).c)).forEach(i -> {
            throw new RuntimeException("Declared default parameter " + i + " value (" + parameters.get(i).c + " is not one of the possible values for this parameter: " + parameters.get(i).b);
        });
        if (args.length != parameters.size()) throw new RuntimeException("Number of parameters supplied doesn't match number of parameters recorded");
        IntStream.range(0, args.length).forEach(i -> {
            if (!parameters.get(i).b.contains(args[i]) || parameters.get(i).c.getClass() != args[i].getClass()) throw new RuntimeException("Given argument " + i + " (" + args[i] + ") is not one of the possible values: " + parameters.get(i).b.toString());
            parameterValues.put(parameters.get(i).a, args[i]);});
    }

    /**
     * To be implemeted in subclasses.
     * @return a list of groups (3-tuples), one per parameter, including:
     * - group.a = parameter name
     * - group.b = all possible values for parameter
     * - group.c = default value for parameter
     */
    public abstract List<Group<String, List<?>, ?>> getAllowedParameters();

    /* Final methods */

    /**
     * @return name of metric, following format: "METRIC_NAME (parameter1_value, parameter2_value ...)"
     */
    protected final String name() {
        List<Group<String, List<?>, ?>> parameters = getAllowedParameters();
        String name = parameters.stream().map(p -> getParameterValue(p.a) + ", ").collect(Collectors.joining("", getClass().getSimpleName() + " (", ")"));
        return name.replace(", )", ")");
    }

    /**
     * Retrieve the values of one parameter.
     * @param parameterName - ID of parameter queried
     */
    public final Object getParameterValue(String parameterName) {
        return parameterValues.get(parameterName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractParameterizedMetric)) return false;
        AbstractParameterizedMetric that = (AbstractParameterizedMetric) o;
        return Objects.equals(parameterValues, that.parameterValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterValues);
    }
}
