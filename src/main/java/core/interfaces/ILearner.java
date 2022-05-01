package core.interfaces;

import java.util.List;

public interface ILearner {

    void learnFrom(IStateFeatureVector[] stateVectors, double[] values);

}
