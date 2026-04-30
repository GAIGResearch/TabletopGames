package players.observers;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;

public class GameAdviser implements IGameListener {

    Game game;
    AbstractPlayer player;
    IAdviceFilter filter;

    /**
     * A Game Adviser is responsible for providing advice to one or more players during a game.
     * This may be one or all players in the game.
     *
     * @param player
     */
    public GameAdviser(AbstractPlayer player, IAdviceFilter filter) {
        this.player = player;
        this.filter = filter;
    }

    @Override
    public void onEvent(Event event) {
        int actingPlayer = event.state.getCurrentPlayer();
        if (event.type == Event.GameEvent.ACTION_CHOSEN && filter.advise(event.state, game.getPlayers().get(actingPlayer))) {
            AbstractAction action = player.getAction(event.state, event.actions);
            game.setOverrideAction(action);
        }
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
