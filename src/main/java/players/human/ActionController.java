package players.human;

import core.actions.AbstractAction;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

public class ActionController {

    private boolean debug = false;
    private BlockingQueue<AbstractAction> actionsQueue;
    private AbstractAction lastActionPlayed;

    public ActionController() {
        actionsQueue = new ArrayBlockingQueue<>(1);
    }

    public void addAction(AbstractAction candidate) {
        if (candidate != null && actionsQueue.remainingCapacity() > 0) {
            actionsQueue.add(candidate);
            if (debug) System.out.printf("Action %s added to ActionController%n", candidate);
        }
    }

    public AbstractAction getAction() throws InterruptedException {
        lastActionPlayed = actionsQueue.take();
        if (debug) System.out.printf("Action %s taken via getAction()%n", lastActionPlayed);
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
        if (debug && actionsQueue.size() > 0)
            System.out.printf("Action Queue being cleared with %d actions%n", actionsQueue.size());
        actionsQueue.clear();
    }

    public AbstractAction getLastActionPlayed() {
        return lastActionPlayed;
    }

    public Queue<AbstractAction> getActionsQueue() {
        return actionsQueue;
    }

    public void setLastActionPlayed(AbstractAction a) {
        lastActionPlayed = a;
    }

    public boolean hasAction() {
        return !actionsQueue.isEmpty();
    }
}