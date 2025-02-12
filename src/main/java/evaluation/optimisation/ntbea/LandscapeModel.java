package evaluation.optimisation.ntbea;

public interface LandscapeModel {
    // reset removes all data from the model
    void reset();

    // creates a copy
    LandscapeModel copy();

    SearchSpace getSearchSpace();

    void addPoint(int[] datapoint, double value);

    void recalculateModel();

    double getMeanEstimate(int[] datapoint);

    double getLowerBound(int[] datapoint);

    double getUpperBound(int[] datapoint);

    int[] getBestSampled();
}
