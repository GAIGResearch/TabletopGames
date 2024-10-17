package games.hanabi;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.List;
import java.util.Random;

public class HanabiPlayer extends AbstractPlayer{
    private final Random rnd;
    public HanabiPlayer() {
        super(null, "HanabiPlayer");
        this.rnd = new Random();
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        HanabiGameState hbgs = (HanabiGameState) gameState;
        for (AbstractAction action : actions) {
            if (hbgs.hintCounter.getValue() != hbgs.hintCounter.getMinimum()){

            }
        }
        int randomAction = rnd.nextInt(actions.size());
        return actions.get(randomAction);
    }

    @Override
    public AbstractPlayer copy() {
        return this;
    }
}
