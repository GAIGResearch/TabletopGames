package evaluation.metrics;

import org.apache.commons.lang3.reflect.ConstructorUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import static utilities.Utils.generateCombinations;

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
                Object[] paramValues = (Object[]) clazz.getSuperclass().getDeclaredMethod("getAllowedParameters").invoke(met);
                if(paramValues.length == 0)
                {
                    metrics.add(met);
                }else{

                    //More than one parameter in the constructor of the metric.
                    //paramValues contains several arrays, each with all possible value for each param.
                    if(paramValues[0] instanceof Object[])
                    {
                        Class<?>[] argClass = new Class<?>[paramValues.length];
                        List<Object[]> allParams = new ArrayList<>();
                        for (int i = 0; i < paramValues.length; i++) {
                            Object[] pArray = (Object[]) paramValues[i];
                            allParams.add(pArray);
                            argClass[i] = String.class; //TODO: This assumes the arguments are always a string. Hell yeah.
                        }

                        //Get all the combinations for all values for all parameters
                        List<Object[]> combinations = generateCombinations(allParams);

                        //Build each one of the possible metrics.
                        Constructor<?> constructor = ConstructorUtils.getMatchingAccessibleConstructor(clazz, argClass);
                        for(Object[] combParams : combinations)
                        {
                            String[] paramsStr = new String[combParams.length];
                            for(int i = 0; i < paramsStr.length; i++)
                                paramsStr[i] = combParams[i].toString();

                            Object retValue = constructor.newInstance(paramsStr);
                            metrics.add((AbstractMetric) retValue);
                        }

                    }else {

                       //One parameter only in the constructor of the metric. paramValues contains all possible values for that param.
                       for (Object p : paramValues) {

                            //TODO: This assumes the arguments are always a string. Hell yeah.
                            Constructor<?> constructor = ConstructorUtils.getMatchingAccessibleConstructor(clazz, String.class);
                            Object retValue = constructor.newInstance(p.toString());
                            metrics.add((AbstractMetric) retValue);
                        }
                    }
                }
            }catch(Exception e) {
                throw new RuntimeException(e);
            }
        }


        return metrics.toArray(new AbstractMetric[0]);
    }
}
