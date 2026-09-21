package games.cuckoo;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.cuckoo.actions.KeepCard;
import games.cuckoo.actions.SwapCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.CoreConstants.GameResult.*;
import static core.CoreConstants.VisibilityMode.*;

/**
 * <p>The forward model contains all the game rules and logic for Cuckoo.</p>
 */
public class CuckooForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        CuckooGameState state = (CuckooGameState) firstState;
        CuckooParameters params = (CuckooParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();

        state.playerCards = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            state.playerCards.add(new Deck<>("Card " + p, p, VISIBLE_TO_OWNER));
        state.drawDeck = FrenchCard.generateDeck("DrawDeck", HIDDEN_TO_ALL);
        state.lives = new int[nPlayers];
        Arrays.fill(state.lives, params.nLives);
        state.roundEliminated = new int[nPlayers];
        Arrays.fill(state.roundEliminated, -1);

        // the last player deals first, so player 0 (on the dealer's left) decides first
        state.dealer = nPlayers - 1;
        deal(state);
    }

    /**
     * Gathers all the cards into the draw deck, shuffles it and deals one card to each player still in the game,
     * starting on the dealer's left. The player on the dealer's left decides first.
     */
    void deal(CuckooGameState state) {
        int nPlayers = state.getNPlayers();
        for (Deck<FrenchCard> deck : state.playerCards) {
            state.drawDeck.add(deck);
            deck.clear();
        }
        state.drawDeck.shuffle(state.getRnd());
        for (int i = 1; i <= nPlayers; i++) {
            int p = (state.dealer + i) % nPlayers;
            if (state.isInGame(p))
                state.playerCards.get(p).add(state.drawDeck.draw());
        }
        state.knowledge = new CuckooKnowledge(nPlayers);
        state.setFirstPlayer(state.nextPlayerInGame(state.dealer));
    }

    /**
     * Every player may keep their card or try to swap it.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return new ArrayList<>(List.of(new KeepCard(), new SwapCard()));
    }

    /**
     * Passes the turn to the next player in the game, or once the dealer has decided, ends the round.
     */
    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        CuckooGameState state = (CuckooGameState) currentState;
        int player = state.getCurrentPlayer();
        if (player != state.dealer) {
            endPlayerTurn(state, state.nextPlayerInGame(player));
            return;
        }
        loseLives(state);
        if (state.getNPlayersInGame() <= 1) {
            endGame(state);
            return;
        }
        // the deal passes to the left, and the player on the new dealer's left decides first
        state.dealer = state.nextPlayerInGame(state.dealer);
        endRound(state, state.nextPlayerInGame(state.dealer));
        if (state.isNotTerminal())  // endRound ends the game at the framework's maxRounds
            deal(state);
    }

    /**
     * Every player in the game holding the lowest card loses a life. A player with no lives left is out.
     */
    private void loseLives(CuckooGameState state) {
        int lowest = Integer.MAX_VALUE;
        for (int p = 0; p < state.getNPlayers(); p++) {
            if (state.isInGame(p))
                lowest = Math.min(lowest, CuckooUtils.rank(state.getPlayerCard(p)));
        }
        for (int p = 0; p < state.getNPlayers(); p++) {
            if (state.isInGame(p) && CuckooUtils.rank(state.getPlayerCard(p)) == lowest) {
                state.lives[p]--;
                if (state.lives[p] == 0)
                    state.roundEliminated[p] = state.getRoundCounter();
            }
        }
    }

    /**
     * The last player left in the game wins and everyone else loses. If nobody is left, the players who went out in
     * the final round all win and everyone else loses.
     */
    @Override
    protected void endGame(AbstractGameState gs) {
        CuckooGameState state = (CuckooGameState) gs;
        int nLeft = state.getNPlayersInGame();
        // more than one player is left only when the framework's maxRounds ends the game; they are ranked by lives
        if (nLeft > 1) {
            super.endGame(gs);
            return;
        }
        state.setGameStatus(GAME_END);
        int finalRound = Arrays.stream(state.roundEliminated).max().orElse(-1);
        for (int p = 0; p < state.getNPlayers(); p++) {
            boolean wins = nLeft == 1 ? state.isInGame(p) : state.roundEliminated[p] == finalRound;
            state.setPlayerResult(wins ? WIN_GAME : LOSE_GAME, p);
        }
    }
}
