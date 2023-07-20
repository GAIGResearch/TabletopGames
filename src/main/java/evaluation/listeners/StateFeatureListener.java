package evaluation.listeners;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IStateFeatureVector;
import evaluation.metrics.Event;

import java.util.regex.Pattern;

/**
 * This provides a generic way of recording training data from games. After each move is made, it will record a feature
 * vector of the current state (for each player?) and the current score.
 * When a game is finished, and we know the final result, the records for the game can be updated with this (i.e.
 * win/loss, score, ordinal position), and all the records written to file.
 */
public class StateFeatureListener extends FeatureListener {

    IStateFeatureVector phiFn;

    public StateFeatureListener(IStateFeatureVector phi, Event.GameEvent frequency, boolean currentPlayerOnly) {
        super(frequency, currentPlayerOnly);
        this.phiFn = phi;
    }

    @Override
    public String[] names() {
        return phiFn.names();
    }

    @Override
    public double[] extractFeatureVector(AbstractAction action, AbstractGameState state, int perspectivePlayer) {
        return phiFn.featureVector(state, perspectivePlayer);
    }

    @Override
    public String injectAgentAttributes(String raw) {
        return raw.replaceAll(Pattern.quote("*PHI*"), phiFn.getClass().getCanonicalName());
    }
}
