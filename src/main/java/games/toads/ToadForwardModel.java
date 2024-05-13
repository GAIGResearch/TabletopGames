package games.toads;

import core.*;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.toads.actions.PlayFieldCard;
import games.toads.actions.PlayFlankCard;
import gametemplate.actions.GTAction;

import java.util.ArrayList;
import java.util.List;


public class ToadForwardModel extends StandardForwardModel {


    @Override
    protected void _setup(AbstractGameState firstState) {
        ToadGameState state = (ToadGameState) firstState;
        ToadParameters params = (ToadParameters) state.getGameParameters();

        state.battlesWon = new int[2][2];
        state.fieldCards = new ToadCard[state.getNPlayers()];
        state.hiddenFlankCards = new ToadCard[state.getNPlayers()];
        state.tieBreakers = new ToadCard[state.getNPlayers()];
        state.playerDiscards = new ArrayList<>();
        state.playerHands = new ArrayList<>();
        state.playerDecks = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            state.playerDecks.add(new Deck<>("Player " + i + " Deck", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            state.playerHands.add(new PartialObservableDeck<>("Player " + i + " Hand", i, 2,  CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            state.playerDiscards.add(new Deck<>("Player " + i + " Discard", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            List<ToadCard> cards = params.getCardDeck();
            state.playerDecks.get(i).add(cards);
            state.playerDecks.get(i).shuffle(state.getRnd());
            for (int j = 0; j < params.handSize; j++) {
                state.playerHands.get(i).add(state.playerDecks.get(i).draw());
            }
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        int player = gameState.getCurrentPlayer();
        ToadGameState state = (ToadGameState) gameState;
        if (state.fieldCards[player] == null) {
            for (ToadCard card : state.playerHands.get(player)) {
                actions.add(new PlayFieldCard(card));
            }
        } else if (state.hiddenFlankCards[player] == null) {
            for (ToadCard card : state.playerHands.get(player)) {
                actions.add(new PlayFlankCard(card));
            }
        } else {
            throw new AssertionError("Player already has Field and Flank cards in play");
        }

        return actions.stream().distinct().toList();
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        // Player 0 takes two turns (field and flank) [well, turn-owner to be more precise]
        // then Player 1 does the same
        // then we reveal the hidden cards and resolve the two battles
        int currentPlayer = gameState.getCurrentPlayer();
        ToadGameState state = (ToadGameState) gameState;
        ToadParameters params = (ToadParameters) state.getGameParameters();
        if (state.hiddenFlankCards[currentPlayer] == null) {
            // continue with the same player
        } else if (state.fieldCards[1 - currentPlayer] == null) {
            // next player
            endTurn(gameState, 1 - currentPlayer);
        } else {
            // we reveal cards and resolve
            // not the most elegant solution, but with 2 cards each no need to generalise yet
            int attacker = 1 - currentPlayer; // defender always goes second, so is the current player
            BattleResult battle = new BattleResult(attacker, state.fieldCards[attacker], state.fieldCards[1-attacker],
                    state.hiddenFlankCards[attacker], state.hiddenFlankCards[1-attacker]);

            int[] scoreDiff = battle.calculate(state);

            // convert back to scores for each player
            int round = state.getRoundCounter();
            // Overcommit rule (only counts as one victory if you win by 2 and are currently ahead)
            if (scoreDiff[0] == 2 && state.battlesWon[round][0] >= state.battlesWon[round][1]) {
                scoreDiff[0]--;
            } else if (scoreDiff[1] == 2 && state.battlesWon[round][1] >= state.battlesWon[round][0]) {
                scoreDiff[1]--;
            }
            // and increment scores
            state.battlesWon[round][0] += scoreDiff[0];
            state.battlesWon[round][1] += scoreDiff[1];
            // move cards to discard
            state.playerDiscards.get(0).add(state.fieldCards[0]);
            state.playerDiscards.get(0).add(state.hiddenFlankCards[0]);
            state.playerDiscards.get(1).add(state.fieldCards[1]);
            state.playerDiscards.get(1).add(state.hiddenFlankCards[1]);
            // reset field and flank cards
            state.fieldCards = new ToadCard[state.getNPlayers()];
            state.hiddenFlankCards = new ToadCard[state.getNPlayers()];

            // if all cards played, then we keep the same player as the attacker for the next round
            // Then check for end of round
            if (state.playerHands.get(0).getSize() <= 1) {
                // one card left in hand each
                if (state.playerDecks.get(0).getSize() != 0 || state.playerDecks.get(1).getSize() != 0) {
                    throw new AssertionError("Should have no cards left in either deck at this point");
                }
                // no more cards to draw so end of round
                if (state.getRoundCounter() == 1) {
                    // end of game
                    // we need to modify the final scores due to the rules

                    endGame(gameState);
                } else {
                    // set tie breakers
                    state.tieBreakers[0] = state.playerHands.get(0).draw();
                    state.tieBreakers[1] = state.playerHands.get(1).draw();
                    // then discards become the other players decks
                    state.playerDecks.get(0).add(state.playerDiscards.get(1));
                    state.playerDecks.get(1).add(state.playerDiscards.get(0));
                    for (Deck<ToadCard> discard : state.playerDiscards) {
                        discard.clear();
                    }
                    // shuffle
                    state.playerDecks.get(0).shuffle(state.getRnd());
                    state.playerDecks.get(1).shuffle(state.getRnd());
                    // and draw new hands
                    for (int i = 0; i < params.handSize; i++) {
                        state.playerHands.get(0).add(state.playerDecks.get(0).draw());
                        state.playerHands.get(1).add(state.playerDecks.get(1).draw());
                    }
                    endRound(gameState, 0);
                }
            } else {
                // the
                endTurn(gameState, currentPlayer);
            }
        }
    }

    protected void endTurn(AbstractGameState gs, int nextPlayer) {
        ToadGameState state = (ToadGameState) gs;
        int player = state.getCurrentPlayer();
        // Draw 2 cards
        int cardsToDraw = Math.min(2, state.playerDecks.get(player).getSize());
        for (int i = 0; i < cardsToDraw; i++) {
            state.playerHands.get(player).add(state.playerDecks.get(player).draw());
        }
        endPlayerTurn(state, nextPlayer);
    }
}
