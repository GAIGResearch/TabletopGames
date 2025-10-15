package games.gofish;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.CoreConstants;
import games.gofish.actions.GoFishAsk;
import games.gofish.actions.GoFishDrawAction;
import games.gofish.actions.GoFishEndTurnAction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GoFishForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        GoFishGameState state = (GoFishGameState) firstState;
        GoFishParameters params = (GoFishParameters) state.getGameParameters();

        // Create and shuffle deck
        state.drawDeck = FrenchCard.generateDeck("DrawDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        state.drawDeck.shuffle(state.getRnd());

        // Init hands and books
        state.playerHands = new ArrayList<>();
        state.playerBooks = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            state.playerHands.add(new Deck<>("hand_" + i, i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            state.playerBooks.add(new Deck<>("books_" + i, i, CoreConstants.VisibilityMode.VISIBLE_TO_ALL));
        }

        // Deal
        for (int i = 0; i < state.getNPlayers(); i++) {
            for (int j = 0; j < params.startingHandSize && state.drawDeck.getSize() > 0; j++) {
                state.playerHands.get(i).add(state.drawDeck.draw());
            }
            state.checkAndCollectBooks(i);
        }

        state.continuePlayerTurn = false;
        state.mustDraw = false;
        state.lastRequestedRank = -1;
        state.setFirstPlayer(0);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        GoFishGameState state = (GoFishGameState) gameState;

        int currentPlayer = state.getCurrentPlayer();
        List<AbstractAction> actions = new ArrayList<>();

        Deck<FrenchCard> hand = state.getPlayerHands().get(currentPlayer);
        boolean deckEmpty = state.getDrawDeck().getSize() == 0;
        boolean handEmpty = hand.getSize() == 0;

        // Empty hand: draw if possible, else pass
        if (handEmpty) {
            if (!deckEmpty) actions.add(new GoFishDrawAction(currentPlayer));
            else actions.add(new GoFishEndTurnAction(currentPlayer));
            return actions;
        }

        // Must draw due to failed ask
        if (state.mustDraw) {
            if (!deckEmpty) {
                if (state.lastRequestedRank != -1)
                    actions.add(new GoFishDrawAction(currentPlayer, state.lastRequestedRank));
                else
                    actions.add(new GoFishDrawAction(currentPlayer));
            } else {
                actions.add(new GoFishEndTurnAction(currentPlayer));
            }
            return actions;
        }

        // Ask actions: each unique rank in hand × each opponent with at least 1 card
        Set<Integer> ranks = new HashSet<>();
        for (FrenchCard c : hand.getComponents()) ranks.add(c.number);

        boolean hasValidTargets = false;
        for (int target = 0; target < state.getNPlayers(); target++) {
            if (target == currentPlayer) continue;
            if (state.getPlayerHands().get(target).getSize() > 0) {
                hasValidTargets = true;
                for (int r : ranks) actions.add(new GoFishAsk(currentPlayer, target, r));
            }
        }

        // If no asks possible: draw if can, else pass
        if (!hasValidTargets || ranks.isEmpty()) {
            if (!deckEmpty) actions.add(new GoFishDrawAction(currentPlayer));
            else actions.add(new GoFishEndTurnAction(currentPlayer));
        }

        // Guard: never return empty in a non-terminal state
        if (actions.isEmpty()) {
            throw new IllegalStateException("No actions available to player " + currentPlayer +
                    " in Go Fish, but game is not over!");
        }

        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        GoFishGameState state = (GoFishGameState) gameState;
        int current = state.getCurrentPlayer();

        // Interpret action outcome
        if (action instanceof GoFishAsk ask) {
            state.lastRequestedRank = ask.rankAsked;

            if (!ask.receivedCards) {
                // Failed ask -> must draw (or pass if deck empty, handled in _computeAvailableActions)
                state.mustDraw = true;
                return; // stay on same player to draw/pass next
            } else {
                // Successful ask -> same player continues
                state.continuePlayerTurn = true;
                state.lastRequestedRank = -1;
            }
        } else if (action instanceof GoFishDrawAction draw) {
            state.mustDraw = false;
            // Continue if we drew the requested rank (optional rule enabled here)
            state.continuePlayerTurn = draw.drewRequestedRank;
            state.lastRequestedRank = -1;
        }

        // Books after any action
        state.checkAndCollectBooks(current);

        // End if terminal
        if (isGameEnd(state)) {
            endGame(state);
        } else {
            if (!state.continuePlayerTurn) {
                endPlayerTurn(state);
                // then end round if we are back to the first player
                if (state.getCurrentPlayer() == state.getFirstPlayer()) {
                    endRound(state);
                }
            }
            state.continuePlayerTurn = false; // consume the repeat
        }
    }

    private boolean isGameEnd(GoFishGameState state) {
        // All 13 ranks booked?
        int totalBooks = 0;
        for (Deck<FrenchCard> b : state.getPlayerBooks())
            totalBooks += b.getSize() / 4;
        if (totalBooks >= 13) return true;

        // Deck empty AND all hands empty?
        if (state.getDrawDeck().getSize() > 0) return false;
        for (int i = 0; i < state.getNPlayers(); i++)
            if (state.getPlayerHands().get(i).getSize() > 0) return false;
        return true;
    }

}
