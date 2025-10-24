package games.gofish;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.gofish.actions.GoFishAsk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.VisibilityMode.*;

public class GoFishForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        GoFishGameState state = (GoFishGameState) firstState;
        GoFishParameters params = (GoFishParameters) state.getGameParameters();

        // Create and shuffle deck
        state.drawDeck = FrenchCard.generateDeck("DrawDeck", HIDDEN_TO_ALL);
        state.drawDeck.shuffle(state.getRnd());

        // Init hands and books
        state.playerHands = new ArrayList<>();
        state.playerBooks = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            state.playerHands.add(new PartialObservableDeck<>("hand_" + i, i, state.getNPlayers(), VISIBLE_TO_OWNER));
            state.playerBooks.add(new Deck<>("books_" + i, i, VISIBLE_TO_ALL));
        }

        // Deal
        for (int i = 0; i < state.getNPlayers(); i++) {
            for (int j = 0; j < params.startingHandSize && state.drawDeck.getSize() > 0; j++) {
                state.playerHands.get(i).add(state.drawDeck.draw());
            }
            state.checkAndCollectBooks(i);
        }
        state.setFirstPlayer(0);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        GoFishGameState state = (GoFishGameState) gameState;

        int currentPlayer = state.getCurrentPlayer();
        List<AbstractAction> actions = new ArrayList<>();

        Deck<FrenchCard> hand = state.getPlayerHands().get(currentPlayer);

        // Ask actions: each unique rank in hand × each opponent with at least 1 card
        Set<Integer> ranks = new HashSet<>();
        for (FrenchCard c : hand.getComponents()) ranks.add(c.number);

        for (int target = 0; target < state.getNPlayers(); target++) {
            if (target == currentPlayer) continue;
            if (state.getPlayerHands().get(target).getSize() > 0) {
                for (int r : ranks) actions.add(new GoFishAsk(target, r));
            }
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        GoFishGameState state = (GoFishGameState) gameState;
        GoFishParameters  params = (GoFishParameters) state.getGameParameters();
        int current = state.getCurrentPlayer();

        // Interpret action outcome
        boolean continuePlayerTurn = false;
        if (action instanceof GoFishAsk ask) {
            if (!ask.receivedCards) {
                // Failed ask -> must draw if deck not empty
                if (state.drawDeck.getSize() > 0) {
                    FrenchCard card = state.drawDeck.draw();
                    state.getPlayerHands().get(current).add(card);
                    if (params.continueOnDrawingSameRank && card.number == ask.rankAsked) {
                        continuePlayerTurn = true;
                    }
                }
            } else {
                if (params.continueFishingOnSuccess) {
                    continuePlayerTurn = true;
                }
            }
        }

        // Books after any action
        state.checkAndCollectBooks(current);

        if (state.playerHands.get(current).getSize() == 0 && state.drawDeck.getSize() == 0) {
            continuePlayerTurn = false;  // special case
        }

        // End if terminal
        if (isGameEnd(state)) {
            endGame(state);
        } else {
            if (!continuePlayerTurn) {
                boolean nextPlayerFound = false;
                do {
                    endPlayerTurn(state);
                    // then end round if we are back to the first player
                    if (state.getCurrentPlayer() == state.getFirstPlayer()) {
                        endRound(state);
                    }
                    current =  state.getCurrentPlayer();
                    if (state.playerHands.get(current).getSize() == 0) {
                        // draw card
                        if (state.drawDeck.getSize() > 0) {
                            state.playerHands.get(current).add(state.drawDeck.draw());
                        }
                    } else {
                        nextPlayerFound = true;
                    }
                } while (!nextPlayerFound);
            }
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
