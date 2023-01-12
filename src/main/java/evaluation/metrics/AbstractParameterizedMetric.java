package evaluation.metrics;
import java.util.HashSet;

public abstract class AbstractParameterizedMetric extends AbstractMetric
{
    public abstract String name();
    public abstract Object[] getAllowedParameters();
}
