package players.heuristics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import utilities.Utils;


public class LinearActionHeuristic extends GLMActionHeuristic {

    protected double minValue = Double.NEGATIVE_INFINITY;
    protected double maxValue = Double.POSITIVE_INFINITY;

    public LinearActionHeuristic(IStateFeatureVector featureVector, IActionFeatureVector actionFeatureVector, String coefficientsFile) {
        super(featureVector, actionFeatureVector, coefficientsFile);
    }

    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state) {
        if (coefficients == null)
            throw new AssertionError("No coefficients found");
     //   double[] phi = features.featureVector(state, state.getCurrentPlayer());
   //     double retValue = coefficients[0]; // the bias term
   //     for (int i = 0; i < phi.length; i++) {
  //          retValue += phi[i] * coefficients[i + 1];
  //      }
        // We don't need to calculate the state features, since they are the same, by definition, for all actions
        // and this is a linear approximator (if this were non-linear, then this would not be possible)
        // we also ignore the BIAS term (the first coefficient) for the same reason
        // (these are still potentially useful to learn if they reduce bias/variance in the psi coefficients)
        double retValue = 0;
        int phiLength = features.names().length;
        double[] psi = actionFeatures.featureVector(action, state, state.getCurrentPlayer());
        for (int i = 0; i < psi.length; i++) {
            retValue += psi[i] * coefficients[i + 1 + phiLength];
        }
        return Utils.clamp(retValue, minValue, maxValue);
    }

}
