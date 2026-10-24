package gui;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.List;

/**
 * Lets a human player plan a run of their own decisions (all of a power's orders, say) on a copy of the game, change
 * them in any order, and send them together, rather than making each one as the game asks for it. Nothing random or
 * hidden may be revealed between the decisions of one run, so that the plan cannot be overtaken by events.
 * <p>
 * When the plan is sent, each decision the game asks for in the run is answered with the planned action it offers
 * (the first in the plan's order), else with the {@link #fallback}, else the plan stops and the player is asked.
 * Every action sent is one the forward model offered at that decision.
 * <p>
 * A GUI offers a planner with {@link AbstractGUIManager#getPlanner()}. The web server uses it; the desktop GUI does
 * not.
 */
public interface IMovePlanner {

    /**
     * Whether the player may plan from this decision of theirs (the state they are asked to decide in).
     */
    boolean plans(AbstractGameState state, int player);

    /**
     * The actions the player may add to the plan in the planned state: those the forward model would offer next, or
     * more (orders for any unit, not only the one the game would ask about next). None when the plan can go no
     * further.
     */
    List<AbstractAction> options(AbstractGameState planned, int player);

    /**
     * Carries out a planned action on the planned state, as far as the plan needs (recording an order, say).
     */
    void apply(AbstractGameState planned, AbstractAction action);

    /**
     * Whether choosing the action replaces the planned one (a new order for a unit already given one).
     */
    default boolean replaces(AbstractAction planned, AbstractAction chosen) {
        return false;
    }

    /**
     * The action to take, once the plan is sent, at a decision of the run that the plan has no action for (a hold for
     * a unit given no order), or null to stop and ask the player.
     *
     * @param available the actions the forward model offers
     */
    default AbstractAction fallback(AbstractGameState state, List<AbstractAction> available) {
        return null;
    }

    /**
     * What the player should know before sending the plan (an order that does not fit with the others, say).
     */
    default List<String> warnings(AbstractGameState planned, int player) {
        return List.of();
    }

    /**
     * The label of the button that sends the plan.
     */
    default String sendLabel() {
        return "Send";
    }
}
