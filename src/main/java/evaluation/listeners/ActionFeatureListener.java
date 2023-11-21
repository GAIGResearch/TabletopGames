package evaluation.listeners;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import evaluation.metrics.Event;

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
    private Map<AbstractAction, Double> actionValues = new HashMap<>();


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
    public double[] extractFeatureVector(AbstractAction action, AbstractGameState state, int perspectivePlayer) {
        // We put phi in first, and then psi
        double[] retValue = new double[psiFn.names().length + phiFn.names().length];
        double[] phi = cachedPhi == null ?
                phiFn != null ? phiFn.featureVector(state, perspectivePlayer) : new double[0]
                : cachedPhi;
        System.arraycopy(phi, 0, retValue, 0, phi.length);
        double[] psi = psiFn.featureVector(action, state, perspectivePlayer);
        System.arraycopy(psi, 0, retValue, phi.length, psi.length);
        return retValue;
    }

    protected void processStateWithTargets(AbstractGameState state, AbstractAction action, Map<AbstractAction, Double> targets) {
        actionValues = targets;
        processState(state, action);
    }

    @Override
    public void processState(AbstractGameState state, AbstractAction action) {
        // we override this from FeatureListener, because we want to record the feature vector for each action
        if (action == null) return; // we do not record data for the GAME_OVER event
        cachedPhi = null;
        List<AbstractAction> availableActions = game.getForwardModel().computeAvailableActions(state);
        if (availableActions.size() == 1) {
            // only one action available, so no decision to take
            return;
        }
        if (actionValues.isEmpty()) {
            // the default if not provided
            actionValues = availableActions.stream().collect(toMap(a -> a, a -> 0.0));
            actionValues.put(action, 1.0);
        }
        int p = state.getCurrentPlayer();
        double[] phi = extractFeatureVector(action, state, p);
        currentData.add(new StateFeatureListener.LocalDataWrapper(p, phi, state, getActionScore(action)));  // chosen
        if (includeActionsNotTaken) {
            for (AbstractAction alternativeAction : availableActions) {
                if (alternativeAction.equals(action)) continue;
                phi = extractFeatureVector(alternativeAction, state, p);
                currentData.add(new StateFeatureListener.LocalDataWrapper(p, phi, state, getActionScore(alternativeAction))); // not chosen
            }
        }
        actionValues.clear();
    }

    private double getActionScore(AbstractAction action) {
        if (actionValues.containsKey(action))
            return actionValues.get(action);
        throw new AssertionError("Action " + action.toString() + " not found in action values map");
    }


    @Override
    public String injectAgentAttributes(String raw) {
        return raw.replaceAll(Pattern.quote("*PSI*"), psiFn.getClass().getCanonicalName())
                .replaceAll(Pattern.quote("*PHI*"), phiFn != null ? phiFn.getClass().getCanonicalName() : "NONE");
    }

}
