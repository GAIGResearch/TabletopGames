package games.toads;

import core.*;
import core.actions.AbstractAction;
import core.components.Deck;
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

        state.battlesWon = new int[state.getNPlayers()];
        state.fieldCards = new ToadCard[state.getNPlayers()];
        state.hiddenFlankCards = new ToadCard[state.getNPlayers()];
        state.tieBreakers = new ToadCard[state.getNPlayers()];
        state.playerDiscards = new ArrayList<>();
        state.playerHands = new ArrayList<>();
        state.playerDecks = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            state.playerDecks.add(new Deck<>("Player " + i + " Deck", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            state.playerHands.add(new Deck<>("Player " + i + " Hand", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
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

        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState gameState, AbstractAction action) {
        // Player 0 takes two turns (field and flank) [well, turnowner to be more precise]
        // then Player 1 does the same
        // then we reveal the hidden cards and resolve the two battles
        int currentPlayer = gameState.getCurrentPlayer();
        ToadGameState state = (ToadGameState) gameState;
        if (state.hiddenFlankCards[currentPlayer] == null) {
            // continue with the same player
        } else if (state.fieldCards[1 - currentPlayer] == null) {
            // next player
            endPlayerTurn(gameState);
        } else {
            // we reveal cards and resolve
            // not the most elegant solution, but with 2 cards each no need to generalise yet
            int p0Field = state.fieldCards[0].value;
            int p0Flank = state.hiddenFlankCards[0].value;
            int p1Field = state.fieldCards[1].value;
            int p1Flank = state.hiddenFlankCards[1].value;
            if (state.fieldCards[0].ability != null) {
                p0Field = state.fieldCards[0].ability.updatedValue(p0Field, p1Field, state.getTurnOwner() == 0);
            }
            if (state.hiddenFlankCards[0].ability != null) {
                p0Flank = state.hiddenFlankCards[0].ability.updatedValue(p0Flank, p1Field, state.getTurnOwner() == 0);
            }
            if (state.fieldCards[1].ability != null) {
                p1Field = state.fieldCards[1].ability.updatedValue(p1Field, p0Field, state.getTurnOwner() == 1);
            }
            if (state.hiddenFlankCards[1].ability != null) {
                p1Flank = state.hiddenFlankCards[1].ability.updatedValue(p1Flank, p0Field, state.getTurnOwner() == 1);
            }
            if (p0Field + p0Flank > p1Field + p1Flank) {
                state.battlesWon[0]++;
            } else if (p0Field + p0Flank < p1Field + p1Flank) {
                state.battlesWon[1]++;
            }
            // move cards to discard
            state.playerDiscards.get(0).add(state.fieldCards[0]);
            state.playerDiscards.get(0).add(state.hiddenFlankCards[0]);
            state.playerDiscards.get(1).add(state.fieldCards[1]);
            state.playerDiscards.get(1).add(state.hiddenFlankCards[1]);
            // reset field and flank cards
            state.fieldCards = new ToadCard[state.getNPlayers()];
            state.hiddenFlankCards = new ToadCard[state.getNPlayers()];


            // TODO: At end of first round we store the tiebreaker
            // TODO: At end of second round we compare the tiebreakers if needed
            // TODO: Implement Overcommit rule
            // Then check for end of round
            if (state.playerHands.get(0).getSize() == 1) {
                // one card left in hand each
                if (state.playerDecks.get(0).getSize() != 0 || state.playerDecks.get(1).getSize() != 0) {
                    throw new AssertionError("Should have no cards left in either deck at this point");
                }
                // no more cards to draw

            } else {
                // TODO: Implement local version that draws cards
                endPlayerTurn(gameState);
            }
        }
    }
}
