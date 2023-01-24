package evaluation.metrics;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import utilities.Group;
import utilities.Pair;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static utilities.Utils.generateCombinations;

/**
 * All metric collections should implement this interface. It gives access to the method which returns a list of
 * all metrics in the class, one instance per class, or one instance per parameter combination for parameterized metrics.
 * Metrics within the class that implements this interface should be <b>static classes</b> which extend
 * either {@link AbstractMetric} or {@link AbstractParameterizedMetric} (if the metric has any parameters, such as
 * recording data per type of card - the 'card type' can be a parameter).
 */
public interface IMetricsCollection
{
    default AbstractMetric[] getAllMetrics()
    {
        ArrayList<AbstractMetric> metrics = new ArrayList<>();
        for(Class<?> clazz : this.getClass().getDeclaredClasses())
        {
            try {
                Constructor<?> defaultCtor = ConstructorUtils.getMatchingAccessibleConstructor(clazz);
                AbstractMetric met = (AbstractMetric) defaultCtor.newInstance();

                if (clazz.getSuperclass() == AbstractParameterizedMetric.class) {
                    // A parameterized class, create all combinations
                    List<Group<String, List<?>, ?>> paramValues = (List<Group<String, List<?>, ?>>) clazz.getSuperclass().getDeclaredMethod("getAllowedParameters").invoke(met);

                    Class<?>[] argClasses = new Class<?>[paramValues.size()];
                    List<Object[]> allParams = new ArrayList<>();
                    for (int i = 0; i < paramValues.size(); i++) {
                        Object[] pArray = paramValues.get(i).b.toArray();
                        allParams.add(pArray);
                        argClasses[i] = Object.class; //paramValues.get(i).c.getClass();
                    }

                    //Get all the combinations for all values for all parameters
                    List<Object[]> combinations = generateCombinations(allParams);

                    //Build each one of the possible metrics.
                    Constructor<?> constructor = ConstructorUtils.getMatchingAccessibleConstructor(clazz, argClasses);
                    for(Object[] combParams : combinations)
                    {
                        AbstractParameterizedMetric retValue;
                        if (combParams.length == 1) {
                            retValue = (AbstractParameterizedMetric) constructor.newInstance(combParams[0]);
                        } else {
                            retValue = (AbstractParameterizedMetric) constructor.newInstance(combParams);
                        }
                        metrics.add(retValue);
                    }

                } else {
                    // Non-parameterized metric
                    metrics.add(met);
                }
            }catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        return metrics.toArray(new AbstractMetric[0]);
    }
}
