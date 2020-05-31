package players;

import core.actions.AbstractAction;

import java.util.ArrayDeque;
import java.util.Queue;

public class ActionController {

    private Queue<AbstractAction> actionsQueue;
    private AbstractAction lastActionPlayed;

    public ActionController()
    {
        actionsQueue = new ArrayDeque<>();
    }

    public void addAction(AbstractAction candidate) {
        if (candidate != null) {
            actionsQueue.add(candidate);
        }
    }

    public AbstractAction getAction() {
        lastActionPlayed = actionsQueue.poll();
        return lastActionPlayed;
    }

    private ActionController(Queue<AbstractAction> otherQueue) {
        actionsQueue = new ArrayDeque<>(otherQueue);
    }

    public ActionController copy() {
        return new ActionController(actionsQueue);
    }

    public void reset() {
        actionsQueue.clear();
    }

    public AbstractAction getLastActionPlayed() {
        return lastActionPlayed;
    }

    public void setLastActionPlayed(AbstractAction a) {
        lastActionPlayed = a;
    }
}