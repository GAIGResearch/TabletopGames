package evaluation.optimisation.ntbea;

public interface SolutionEvaluator {
    /**
     * Created by simonmarklucas on 06/08/2016.
     * <p>
     * Evaluates solutions and logs fitness improvement
     */
    // call reset before running
    public void reset();

    /**
     * Evaluates a given set of parameter settings.
     *
     * @param solution The settings to evaluate. This is an array of indices that reference the corresponding
     *                 parameter dimensions. So [0, 1, 0] means using the 1st values of the 1st and 3rd parameters,
     *                 and the second value of the second parameter. (The length of the array must equal the number of
     *                 dimensions of the search space.
     * @return
     */
    double evaluate(int[] solution);
    // has the algorithm found the optimal solution?

    /**
     * @return TThe search space being used
     */
    SearchSpace searchSpace();

    /**
     * @return The number of evaluations the optimiser has made so far
     */
    int nEvals();

}

