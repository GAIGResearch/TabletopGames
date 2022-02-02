package players.human;

import core.actions.AbstractAction;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

public class ActionController {

    private BlockingQueue<AbstractAction> actionsQueue;
    private AbstractAction lastActionPlayed;

    public ActionController() {
        actionsQueue = new ArrayBlockingQueue<>(1);
    }

    public void addAction(AbstractAction candidate) {
        if (candidate != null && actionsQueue.isEmpty()) {
            actionsQueue.add(candidate);
        }
    }

    public AbstractAction getAction() throws InterruptedException {
        lastActionPlayed = actionsQueue.take();
        return lastActionPlayed;
    }

    private ActionController(BlockingQueue<AbstractAction> otherQueue) {
        actionsQueue = new ArrayBlockingQueue<>(1);
        if (otherQueue.size() > 0)
            actionsQueue.add(otherQueue.peek());
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