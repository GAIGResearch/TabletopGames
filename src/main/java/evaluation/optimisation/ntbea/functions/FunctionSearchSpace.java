package evaluation.optimisation.ntbea.functions;

import evaluation.optimisation.ntbea.SearchSpace;

/**
 * This avoids the need to set up a json definition file. It takes two parameters:
 * - The NTBEA Function to use
 * - the values per dimension
 */
public class FunctionSearchSpace implements SearchSpace {

    final int valuesPerDimension;
    final NTBEAFunction function;

    public FunctionSearchSpace(int valuesPerDimension, NTBEAFunction function) {
        this.valuesPerDimension = valuesPerDimension;
        this.function = function;
    }

    @Override
    public int nDims() {
        return function.dimension();
    }

    @Override
    public int nValues(int i) {
        return valuesPerDimension;
    }

    @Override
    public String name(int i) {
        return String.format("%d", i);
    }

    @Override
    public Double value(int dim, int i) {
        return ((double) i) / valuesPerDimension;
    }

}
