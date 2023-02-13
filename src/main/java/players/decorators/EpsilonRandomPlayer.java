package players.decorators;

import core.*;
import core.actions.AbstractAction;

import java.util.*;

public class EpsilonRandomPlayer extends AbstractPlayerDecorator {

    private double epsilon;
    private final Random rnd;


    public EpsilonRandomPlayer(AbstractPlayer player, long seed) {
        super(player);
        rnd = new Random(seed);
    }

    public EpsilonRandomPlayer(AbstractPlayer player, long seed, double epsilon) {
        this(player, seed);
        this.epsilon = epsilon;
    }

    public EpsilonRandomPlayer(AbstractPlayer player, double epsilon) {
        this(player, System.currentTimeMillis(), epsilon);
    }
    public EpsilonRandomPlayer(AbstractPlayer player) {
        this(player, System.currentTimeMillis());
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }
    @Override
    public EpsilonRandomPlayer copy() {
        return new EpsilonRandomPlayer(player.copy(), rnd.nextInt(), epsilon);
    }

    @Override
    public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
        if (rnd.nextDouble() > epsilon) {
            return possibleActions;
        } else {
            return Collections.singletonList(possibleActions.get(rnd.nextInt(possibleActions.size())));
        }
    }
}
