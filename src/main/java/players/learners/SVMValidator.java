package players.learners;

import utilities.Pair;

public class SVMValidator {

    // We need to train an SVM model on the training data, and then record its value
    // from performance on the validation data. However - this is not stochastic, so a grid search is probably at least as good?
    // iterate over values; set parameters, then learnFrom, then validate
    // ....it is this last that is not currently supported...SVMStateHeuristic itself won;t work, as that takes a state as an input
    // and here we are given phi directly in the validation data


    SVMLearner learner;
    String trainingData, validationData;

    public SVMValidator(String training, String validation) {
        this.trainingData = training;
        this.validationData = validation;
    }

    public static void main(String[] args) {
        SVMValidator evaluator = new SVMValidator(args[0], args[1]);
        double[] cValues = new double[]{0.03, 0.1, 0.3, 1.0, 3.0};
        double[] gammaValues = new double[]{1000.0, 3000.0, 10000.0, 30000.0, 100000.0, 300000.0, 1000000.0};

        double[] bestMedian = new double[2];
        double[] bestSquare = new double[2];
        double lowMedian = Double.POSITIVE_INFINITY;
        double lowSquare = Double.POSITIVE_INFINITY;
        for (double C : cValues) {
            for (double gamma : gammaValues) {
                Pair<Double, Double> result = evaluator.evaluate(C, gamma);
                if (result.a < lowMedian) {
                    bestMedian[0] = C;
                    bestMedian[1] = gamma;
                    lowMedian = result.a;
                }
                if (result.b < lowSquare) {
                    bestSquare[0] = C;
                    bestSquare[1] = gamma;
                    lowSquare = result.b;
                }
            }
        }

        System.out.printf("Best parameters by Median Average Deviation: C: %.2g, Gamma: %.2g%n", bestMedian[0], bestMedian[1]);
        System.out.printf("Best parameters by Mean Square Error: C: %.2g, Gamma: %.2g%n", bestSquare[0], bestSquare[1]);


    }

    public Pair<Double, Double> evaluate(double C, double gamma) {
        learner = new SVMLearner();
        learner.params.C = C;
        learner.params.gamma = gamma;
        learner.learnFrom(trainingData);

        Pair<Double, Double> accuracy = learner.validate(validationData);

        System.out.printf("C: %.3g,\tGamma: %.3g,\tMedianDiff: %.4g,\tSquareDiff: %.4g%n", C, gamma, accuracy.a, accuracy.b);
        return accuracy;
    }

}

