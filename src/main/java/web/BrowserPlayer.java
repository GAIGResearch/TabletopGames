package web;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import gui.IMovePlanner;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * The browser player's seat: a {@link HumanGUIPlayer}, which may also be sent a plan of several decisions (see
 * {@link IMovePlanner}) and then answers the game's decisions from it without waiting for the page.
 * <p>
 * The plan answers the decisions of one run: those of the player's turn in which it was sent. Each is answered with
 * the first planned action the game offers, else with the planner's fallback; if neither is offered, the rest of the
 * plan is dropped and the player is asked as usual.
 */
class BrowserPlayer extends HumanGUIPlayer {

    // put in the action controller to wake the game thread when a plan is sent
    private static final AbstractAction WAKE = new DoNothing();

    private final ActionController ac;
    private final Consumer<String> onPlanStopped;
    // the plan being carried out, and the turn it is for (guarded by this)
    private final Deque<AbstractAction> planned = new ArrayDeque<>();
    private IMovePlanner planner;
    private boolean sending;
    private int turn, round;
    // the decision the player is being asked to make, while the game waits for the page
    private volatile AbstractGameState waitingOn;

    /**
     * @param onPlanStopped told (on the game thread) why a plan was stopped before its end
     */
    BrowserPlayer(ActionController ac, Consumer<String> onPlanStopped) {
        super(ac);
        this.ac = ac;
        this.onPlanStopped = onPlanStopped;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState observation, List<AbstractAction> actions) {
        while (true) {
            AbstractAction planned = fromPlan(observation, actions);
            if (planned != null) return planned;
            waitingOn = observation;
            AbstractAction chosen;
            try {
                chosen = ac.getAction();
            } catch (InterruptedException e) {
                // the game has been stopped
                return new DoNothing();
            } finally {
                waitingOn = null;
            }
            if (chosen != WAKE) return chosen;
        }
    }

    /**
     * The decision the game is waiting for the player to make (the state as the player sees it), or null when it is
     * not waiting for them.
     */
    AbstractGameState waitingOn() {
        return waitingOn;
    }

    /**
     * Whether a plan is being carried out.
     */
    synchronized boolean isSending() {
        return sending;
    }

    /**
     * Carries out the plan, made from the decision the game is waiting for. Call on the Swing thread, with the
     * action controller no longer diverted to the plan.
     */
    void send(IMovePlanner planner, AbstractGameState decision, List<AbstractAction> steps) {
        synchronized (this) {
            this.planner = planner;
            planned.clear();
            planned.addAll(steps);
            turn = decision.getTurnCounter();
            round = decision.getRoundCounter();
            sending = true;
        }
        ac.addAction(WAKE);
    }

    /**
     * The action the plan being carried out gives for the decision, or null if there is none (and the plan is over).
     */
    synchronized AbstractAction fromPlan(AbstractGameState observation, List<AbstractAction> actions) {
        if (!sending) return null;
        if (observation.getTurnCounter() != turn || observation.getRoundCounter() != round
                || observation.getCurrentPlayer() != getPlayerID()) {
            // the run is over
            stopSending();
            return null;
        }
        for (Iterator<AbstractAction> it = planned.iterator(); it.hasNext(); ) {
            AbstractAction a = it.next();
            if (actions.contains(a)) {
                it.remove();
                return a;
            }
        }
        AbstractAction fallback = planner.fallback(observation, actions);
        if (fallback != null && actions.contains(fallback)) return fallback;
        if (!planned.isEmpty())
            onPlanStopped.accept("The rest of your plan could not be carried out: "
                    + planned.peekFirst().getString(observation) + " is no longer possible.");
        stopSending();
        return null;
    }

    private void stopSending() {
        sending = false;
        planned.clear();
        planner = null;
    }
}
