package web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.AbstractGameState;
import core.actions.AbstractAction;
import gui.IMovePlanner;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * The plan the browser player is making for a decision the game is waiting for (see {@link IMovePlanner}): the
 * actions planned so far, the state they lead to (on a copy of the player's view of the game), and the actions that
 * may be added there. The GUI shows the planned state, and offers those actions.
 * <p>
 * Swing thread only.
 */
class MovePlan {

    private final IMovePlanner planner;
    // describes a step, in the state it is planned in
    private final BiFunction<AbstractAction, AbstractGameState, String> describe;
    private final int player;
    // the decision planned from, as the player sees it; never changed (the preview is a copy)
    private final AbstractGameState decision;
    private List<AbstractAction> steps = new ArrayList<>();
    private AbstractGameState preview;
    private List<AbstractAction> options;

    MovePlan(IMovePlanner planner, AbstractGameState decision, int player) {
        this(planner, decision, player, AbstractAction::getString);
    }

    /**
     * @param describe describes a step in the state it is planned in (as the GUI describes the action on its button)
     */
    MovePlan(IMovePlanner planner, AbstractGameState decision, int player,
             BiFunction<AbstractAction, AbstractGameState, String> describe) {
        this.planner = planner;
        this.describe = describe;
        this.decision = decision;
        this.player = player;
        rebuild();
    }

    AbstractGameState decision() {
        return decision;
    }

    /**
     * The state the plan leads to.
     */
    AbstractGameState preview() {
        return preview;
    }

    /**
     * The actions that may be added to the plan.
     */
    List<AbstractAction> options() {
        return options;
    }

    List<AbstractAction> steps() {
        return List.copyOf(steps);
    }

    /**
     * Adds the action to the plan, in place of any planned action it replaces. Returns whether it was added, which it
     * is only if it is one of the options.
     */
    boolean add(AbstractAction action) {
        if (!options.contains(action)) return false;
        steps.removeIf(s -> planner.replaces(s, action));
        steps.add(action);
        rebuild();
        return true;
    }

    void remove(int i) {
        if (i < 0 || i >= steps.size()) return;
        steps.remove(i);
        rebuild();
    }

    void clear() {
        steps.clear();
        rebuild();
    }

    /**
     * Carries out the steps again on a fresh copy of the decision, keeping them only as far as each is still an
     * option (a step taken away may have been what made the next ones possible).
     */
    private void rebuild() {
        preview = decision.copy();
        options = planner.options(preview, player);
        List<AbstractAction> kept = new ArrayList<>();
        for (AbstractAction step : steps) {
            if (!options.contains(step)) break;
            planner.apply(preview, step);
            kept.add(step);
            options = planner.options(preview, player);
        }
        steps = kept;
    }

    /**
     * The plan for the page: its steps, what the player should know before sending it, and the label of the button
     * that sends it.
     */
    JsonObject toJson() {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "plan");
        JsonArray list = new JsonArray();
        // each step described in the state it was planned in
        AbstractGameState state = decision.copy();
        for (AbstractAction step : steps) {
            list.add(describe.apply(step, state));
            planner.apply(state, step);
        }
        msg.add("steps", list);
        JsonArray warnings = new JsonArray();
        planner.warnings(preview, player).forEach(warnings::add);
        msg.add("warnings", warnings);
        msg.addProperty("send", planner.sendLabel());
        return msg;
    }
}
