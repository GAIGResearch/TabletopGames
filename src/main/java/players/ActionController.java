package players;

import core.actions.IAction;

import java.util.ArrayDeque;
import java.util.Queue;

public class ActionController {

    private Queue<IAction> actionsQueue;
    private IAction lastActionPlayed;

    public ActionController()
    {
        actionsQueue = new ArrayDeque<>();
    }

    public void addAction(IAction candidate) {
        if (candidate != null) {
            actionsQueue.add(candidate);
        }
    }

    public IAction getAction() {
        lastActionPlayed = actionsQueue.poll();
        return lastActionPlayed;
    }

    private ActionController(Queue<IAction> otherQueue) {
        actionsQueue = new ArrayDeque<>(otherQueue);
    }

    public ActionController copy() {
        return new ActionController(actionsQueue);
    }

    public void reset() {
        actionsQueue.clear();
    }

    public IAction getLastActionPlayed() {
        return lastActionPlayed;
    }

    public void setLastActionPlayed(IAction a) {
        lastActionPlayed = a;
    }
}