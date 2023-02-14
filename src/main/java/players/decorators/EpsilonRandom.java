package players.decorators;

import core.*;
import core.actions.AbstractAction;

import java.util.*;

public class EpsilonRandom implements AbstractPlayerDecorator {

    private double epsilon;
    private final Random rnd;

    public EpsilonRandom(long seed) {
        rnd = new Random(seed);
    }

    public EpsilonRandom(long seed, double epsilon) {
        this(seed);
        this.epsilon = epsilon;
    }

    public EpsilonRandom(double epsilon) {
        this(System.currentTimeMillis(), epsilon);
    }
    public EpsilonRandom() {
        this(System.currentTimeMillis());
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }


    public EpsilonRandom copy() {
        return new EpsilonRandom(rnd.nextInt(), epsilon);
    }

    @Override
    public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
        if (rnd.nextDouble() > epsilon) {
            return possibleActions;
        } else {
            return Collections.singletonList(possibleActions.get(rnd.nextInt(possibleActions.size())));
        }
    }

    @Override
    public void recordDecision(AbstractGameState state, AbstractAction action) {
        // Do nothing
    }
}
