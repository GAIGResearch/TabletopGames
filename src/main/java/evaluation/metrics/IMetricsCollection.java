package evaluation.metrics;

import org.apache.commons.lang3.reflect.ConstructorUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

/**
 * All metric collections should implement this interface. It gives access to the method which returns a list of
 * all metrics in the class, one instance per class, or one instance per parameter combination for parameterized metrics.
 * Metrics within the class that implements this interface should be <b>static classes</b> which extend
 * either {@link AbstractMetric}.
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
                metrics.add(met);
            }catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        return metrics.toArray(new AbstractMetric[0]);
    }
}
