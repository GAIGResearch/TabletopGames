package games.loveletter.stats;
import evaluation.GameListener;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;
public class LLGameListener extends GameListener {


    public LLGameListener(IStatisticLogger logger) {
        super(logger, null);
    }

    @Override
    public void onEvent(Event event) {
        super.onEvent(event);
    }

}
