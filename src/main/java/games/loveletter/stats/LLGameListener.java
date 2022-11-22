package games.loveletter.stats;
import core.CoreConstants;
import core.Game;
import evaluation.GameListener;
import core.interfaces.IStatisticLogger;
public class LLGameListener extends GameListener {


    public LLGameListener(IStatisticLogger logger) {
        super(logger, null);
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        super.onGameEvent(type, game);
    }

}
