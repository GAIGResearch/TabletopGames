package evaluation.optimisation.ntbea;

import java.util.List;

/**
 * Created by James Goodman on 03/04/2021.
 *
 */
public interface MultiSolutionEvaluator {
    // call reset before running
    public void reset();

    /** Evaluates a given set of parameter settings.
     *
     * @param solution The settings to evaluate. This is a List of length equal to the number of competing solutions.
     *                 Each element in the List is an array of indices that reference the corresponding
     *                 parameter dimensions. So [0, 1, 0] means using the 1st values of the 1st and 3rd parameters,
     *                 and the second value of the second parameter. (The length of the array must equal the number of
     *                 dimensions of the search space.
     * @return
     */
    double[] evaluate(List<int[]> solution);

    /**
     * @return The search space being used
     */
    SearchSpace searchSpace();


}
