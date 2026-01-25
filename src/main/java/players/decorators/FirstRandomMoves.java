package players.decorators;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPlayerDecorator;
import players.mcts.MCTSEnums;

import java.util.List;
import java.util.Random;

public class FirstRandomMoves implements IPlayerDecorator {

    MCTSEnums.RolloutIncrement type;
    int randomUntil;
    Random rnd;

    public FirstRandomMoves(int n, String type) {
        this(n, type, System.currentTimeMillis());
    }

    public FirstRandomMoves(int n, String type, long seed) {
        this.randomUntil = n;
        this.type = MCTSEnums.RolloutIncrement.valueOf(type.toUpperCase());
        rnd = new Random(seed);
    }

    @Override
    public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
        // We play a completely random move until after the timelimit has expired
        boolean playRandom = switch (type) {
            case TICK -> state.getGameTick() < randomUntil;
            case TURN -> state.getTurnCounter() < randomUntil;
            case ROUND -> state.getRoundCounter() < randomUntil;
        };
        if (playRandom) {
            return List.of(possibleActions.get(rnd.nextInt(possibleActions.size())));
        } else {
            return possibleActions;
        }
    }

    @Override
    public boolean decisionPlayerOnly() {
        return true;
    }
}
