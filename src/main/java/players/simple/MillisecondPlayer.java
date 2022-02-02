package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class MillisecondPlayer extends AbstractPlayer {
    Random rnd;

    public MillisecondPlayer(long seed) {
        rnd = new Random(seed);
    }

    public MillisecondPlayer() {
        this(System.currentTimeMillis());
    }

    @Override
    public AbstractAction getAction(AbstractGameState observation, List<AbstractAction> actions) {
        int milliAction = Calendar.getInstance().get(Calendar.MILLISECOND) % actions.size();
        return actions.get(milliAction);
    }

    @Override
    public String toString() {
        return "Millisecond";
    }

    @Override
    public MillisecondPlayer copy() {
        return new MillisecondPlayer();
    }
}
