package players.observers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import org.jetbrains.annotations.NotNull;

public class GameAdviser implements IGameListener {

    protected Game game;
    protected AbstractPlayer player;
    private final IAdviceFilter filter;

    /**
     * A Game Adviser is responsible for providing advice to one or more players during a game.
     * This may be one or all players in the game.
     *
     * The GameAdviser is composed of two elements:
     *      The AbstractPlayer that decides on what the Adviser thinks is best
     *      The IAdviceFilter that decides on which players in a game are advised, and other conditions
     *      about *when* the GameAdviser actually intervenes
     *
     * @param player the agent to be used by the adviser
     * @param filter the filter to (may be null)
     */
    public GameAdviser(@NotNull AbstractPlayer player, @NotNull IAdviceFilter filter) {
        this.player = player;
        this.filter = filter;
    }

    @Override
    public void onEvent(Event event) {
        int actingPlayerID = event.state.getCurrentPlayer();
        AbstractPlayer actingPlayer = game.getPlayers().get(actingPlayerID);
        if (event.type == Event.GameEvent.ACTION_CHOSEN && filter.advise(event.state, event.action, actingPlayer)) {
            AbstractAction action = player.getAction(event.state, event.actions);
            if (provideAdvice(event.state, event.action, action, actingPlayer)) {
                game.setOverrideAction(action);
            }
        }
    }

    /**
     * This is designed to be overridden in sub-classes (which have access to the player)
     * @param state the current game state
     * @param action the action chosen by the advisee
     * @param advice the action the adviser would recommend
     * @param advisee the advisee
     * @return   true if we should recommend the action
     */
    public boolean provideAdvice(AbstractGameState state, AbstractAction action, AbstractAction advice, AbstractPlayer advisee) {
        return true;
    }

    @Override
    public void report() {
        // nothing to do in this case
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public Game getGame() {
        return game;
    }
}
