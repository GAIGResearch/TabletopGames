package evaluation.optimisation.ntbea;

public interface SearchSpace {
    /**
     * Created by sml on 16/08/2016.
     *
     * This models a search space where there is a fixed number of dimensions
     * but each dimension may have a different cardinality (i.e. a different number of possible values)
     *
     */
        // number of dimensions
        int nDims();

        // number of possible values in the ith dimension
        int nValues(int i);

        String name(int i);

        Object value(int dim, int i);
}
