package games.dicemonastery;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.*;
import games.dicemonastery.actions.Pass;

import java.util.*;

public class DontPassPolicy extends AbstractPlayer {

    Random rnd = new Random(System.currentTimeMillis());

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {

        AbstractAction firstAction = possibleActions.get(0);

        possibleActions.remove(new Pass());
        possibleActions.remove(new DoNothing());

        if (possibleActions.isEmpty())

            return firstAction;

        return possibleActions.get(rnd.nextInt(possibleActions.size()));
    }
}
