package evaluation.listeners;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import evaluation.loggers.FileStatsLogger;
import evaluation.metrics.Event;
import org.apache.spark.sql.catalyst.expressions.Abs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toMap;

/**
 * This provides a generic way of recording training data from games. After each move is made, it will record a feature
 * vector of the current state (for each player?) and the current score.
 * When a game is finished, and we know the final result, the records for the game can be updated with this (i.e.
 * win/loss, score, ordinal position), and all the records written to file.
 */
public class ActionFeatureListener extends FeatureListener {

    protected boolean includeActionsNotTaken;
    protected IActionFeatureVector psiFn;
    protected IStateFeatureVector phiFn;
    protected double[] cachedPhi;
    protected Object[] cachedObjectPhi;
    protected Map<String, Map<AbstractAction, Number>> actionValues = new HashMap<>();

    public ActionFeatureListener(IActionFeatureVector psi, IStateFeatureVector phi, Event.GameEvent frequency, boolean includeActionsNotTaken) {
        super(frequency, true);
        if (psi == null) throw new AssertionError("Action Features must be provided and cannot be null");
        this.psiFn = psi;
        this.phiFn = phi;
        this.includeActionsNotTaken = includeActionsNotTaken;
    }

    @Override
    public String[] names() {
        // return the concatenation of psi and phi names
        String[] psiNames = psiFn.names();
        String[] phiNames = phiFn != null ? phiFn.names() : new String[0];
        String[] retValue = new String[psiNames.length + phiNames.length];
        System.arraycopy(phiNames, 0, retValue, 0, phiNames.length);
        System.arraycopy(psiNames, 0, retValue, phiNames.length, psiNames.length);
        return retValue;
    }


    @Override
    public double[] extractDoubleVector(AbstractAction action, AbstractGameState state, int perspectivePlayer) {
        // We put phi in first, and then psi
        double[] retValue = new double[psiFn.names().length + (phiFn == null ? 0 : phiFn.names().length)];
        if (cachedPhi == null) {
            // we need to compute the phi vector
            if (phiFn != null) {
                cachedPhi = phiFn.doubleVector(state, perspectivePlayer);
            } else {
                cachedPhi = new double[0];
            }
        }
        System.arraycopy(cachedPhi, 0, retValue, 0, cachedPhi.length);
        double[] psi = psiFn.doubleVector(action, state, perspectivePlayer);
        System.arraycopy(psi, 0, retValue, cachedPhi.length, psi.length);
        return retValue;
    }

    @Override
    public Object[] extractFeatureVector(AbstractAction action, AbstractGameState state, int perspectivePlayer) {
        // We put phi in first, and then psi
        Object[] retValue = new Object[psiFn.names().length + (phiFn == null ? 0 : phiFn.names().length)];
        if (cachedObjectPhi == null) {
            // we need to compute the phi vector
            if (phiFn != null) {
                cachedObjectPhi = phiFn.featureVector(state, perspectivePlayer);
            } else {
                cachedObjectPhi = new Object[0];
            }
        }
        System.arraycopy(cachedObjectPhi, 0, retValue, 0, cachedObjectPhi.length);
        Object[] psi = psiFn.featureVector(action, state, perspectivePlayer);
        System.arraycopy(psi, 0, retValue, cachedObjectPhi.length, psi.length);
        return retValue;
    }

    @Override
    public void processState(AbstractGameState state, AbstractAction action) {
        // we override this from FeatureListener, because we want to record the feature vector for each action
        // we record this once, and cache the results for all actions
        if (action == null) return; // we do not record data for the GAME_OVER event
        cachedPhi = null;  // relevant phi cache will be recomputed on the first call to the relevant method
        cachedObjectPhi = null;
        List<AbstractAction> availableActions = game.getForwardModel().computeAvailableActions(state);
        if (availableActions.size() == 1) {
            // only one action available, so no decision to take
            return;
        }
        if (actionValues.isEmpty()) {
            // the default if not provided
            Map<AbstractAction, Number> av = availableActions.stream().collect(toMap(a -> a, a -> 0.0));
            av.put(action, 1.0);
            actionValues.put("CHOSEN", av);
        }
        int p = state.getCurrentPlayer();
        double[] doubleData = new double[0];
        Object[] objectData = new Object[0];
        try {
            doubleData = extractDoubleVector(action, state, p);
        } catch (UnsupportedOperationException e) {
            objectData = extractFeatureVector(action, state, p);
        }
        if (objectData.length == 0) {
            currentData.add(LocalDataWrapper.factory(p, doubleData, names(), state, getActionScores(action)));  // chosen
        } else {
            currentData.add(LocalDataWrapper.factory(p, objectData, names(), state, getActionScores(action)));  // chosen
        }
        if (includeActionsNotTaken) {
            // State data will not be recalculated for each other action
            for (AbstractAction alternativeAction : availableActions) {
                if (alternativeAction.equals(action)) continue;
                if (objectData.length == 0) {
                    double[] f = extractDoubleVector(alternativeAction, state, p);
                    currentData.add(LocalDataWrapper.factory(p, f, names(), state, getActionScores(alternativeAction))); // not chosen
                } else {
                    Object[] f = extractFeatureVector(alternativeAction, state, p);
                    currentData.add(LocalDataWrapper.factory(p, f, names(), state, getActionScores(alternativeAction))); // not chosen
                }
            }
        }
        actionValues.clear();
    }

    protected Map<String, Number> getActionScores(AbstractAction action) {
        Map<String, Number> retValue = new HashMap<>();
        for (String key : actionValues.keySet()) {
            retValue.put(key, actionValues.get(key).get(action));
        }
        if (retValue.isEmpty())
            throw new AssertionError("Action " + action.toString() + " not found in action values map");
        return retValue;
    }


    public IStateFeatureVector getPhiFn() {
        return phiFn;
    }

    public IActionFeatureVector getPsiFn() {
        return psiFn;
    }

}
