package games.golfsix;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.golfsix.actions.DiscardCard;
import games.golfsix.actions.DrawCard;
import games.golfsix.actions.ReplaceCard;
import games.golfsix.actions.TurnUp;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static games.golfsix.GolfSixParameters.GRID_SIZE;

/**
 * <p>The forward model contains all the game rules and logic for Six-card Golf
 * (https://www.pagat.com/draw/golf.html).</p>
 */
public class GolfSixForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        GolfSixGameState state = (GolfSixGameState) firstState;
        int nPlayers = state.getNPlayers();
        state.grids = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            state.grids.add(new PartialObservableDeck<>("Grid " + p, p, new boolean[nPlayers]));
        state.drawDeck = FrenchCard.generateDeck("DrawDeck", HIDDEN_TO_ALL);
        state.discardPile = new Deck<>("DiscardPile", VISIBLE_TO_ALL);
        state.drawnCard = new PartialObservableDeck<>("DrawnCard", -1, new boolean[nPlayers]);
        state.scores = new int[nPlayers];
        deal(state);
    }

    /**
     * Gathers all the cards into the draw deck, shuffles it, deals each player a face-down grid one card at a time
     * starting on the dealer's left, and turns up the top card of the draw deck to start the discard pile.
     */
    void deal(GolfSixGameState state) {
        int nPlayers = state.getNPlayers();
        for (PartialObservableDeck<FrenchCard> grid : state.grids) {
            state.drawDeck.add(grid);
            grid.clear();
        }
        state.drawDeck.add(state.discardPile);
        state.discardPile.clear();
        state.drawDeck.add(state.drawnCard);
        state.drawnCard.clear();
        state.drawDeck.shuffle(state.getRnd());

        int first = (state.getDealer() + 1) % nPlayers;
        // grid positions are filled in order, so each card is added at the end of the grid
        for (int pos = 0; pos < GRID_SIZE; pos++)
            for (int i = 0; i < nPlayers; i++)
                state.grids.get((first + i) % nPlayers).addToBottom(state.drawDeck.draw());
        state.discardPile.add(state.drawDeck.draw());
        state.drawnFromDiscard = false;
        state.finisher = -1;
        state.setFirstPlayer(first);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        GolfSixGameState state = (GolfSixGameState) gameState;
        GolfSixParameters params = (GolfSixParameters) state.getGameParameters();
        int player = state.getCurrentPlayer();
        List<AbstractAction> actions = new ArrayList<>();
        if (state.faceUpCount(player) < params.initialFaceUp) {
            // before play, a player turns up GolfSixParameters.initialFaceUp cards, one at a time
            for (int pos = 0; pos < GRID_SIZE; pos++)
                if (!state.isFaceUp(player, pos))
                    actions.add(new TurnUp(pos));
        } else if (state.getDrawnCard() == null) {
            // a turn starts with a draw from either pile
            if (state.drawDeck.getSize() > 0)
                actions.add(new DrawCard(false));
            if (state.discardPile.getSize() > 0)
                actions.add(new DrawCard(true));
        } else {
            // then the drawn card replaces any grid card
            for (int pos = 0; pos < GRID_SIZE; pos++)
                actions.add(new ReplaceCard(pos));
            // a card taken from the discard pile may not be discarded again
            if (!state.drawnFromDiscard)
                actions.add(new DiscardCard());
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        GolfSixGameState state = (GolfSixGameState) currentState;
        GolfSixParameters params = (GolfSixParameters) state.getGameParameters();
        int player = state.getCurrentPlayer();
        if (actionTaken instanceof DrawCard)
            return;
        if (actionTaken instanceof TurnUp) {
            if (state.faceUpCount(player) < params.initialFaceUp)
                return;
        } else {
            // the card has been placed
            if (state.drawDeck.getSize() == 0)
                refillDrawDeck(state);
            if (state.finisher < 0 && state.allFaceUp(player)) {
                if (!params.finalTurns) {
                    endDeal(state);
                    return;
                }
                // each other player has one more turn
                state.finisher = player;
            }
            if (state.finisher == (player + 1) % state.getNPlayers()) {
                endDeal(state);
                return;
            }
        }
        endPlayerTurn(state);
        if (state.getTurnCounter() >= state.getNPlayers() * params.maxTurnsPerPlayer)
            endDeal(state);
    }

    /**
     * Shuffles every card of the discard pile except its top card to form a new draw deck.
     */
    private void refillDrawDeck(GolfSixGameState state) {
        FrenchCard top = state.discardPile.draw();
        state.drawDeck.add(state.discardPile);
        state.discardPile.clear();
        state.discardPile.add(top);
        state.drawDeck.shuffle(state.getRnd());
    }

    /**
     * Turns every grid card face-up and adds each grid's score to its owner's total. Then the next deal starts, or
     * after GolfSixParameters.nDeals deals the game ends.
     */
    private void endDeal(GolfSixGameState state) {
        GolfSixParameters params = (GolfSixParameters) state.getGameParameters();
        for (int p = 0; p < state.getNPlayers(); p++) {
            PartialObservableDeck<FrenchCard> grid = state.grids.get(p);
            for (int pos = 0; pos < GRID_SIZE; pos++)
                grid.setVisibilityOfComponent(pos, GolfSixUtils.visibleToAll(state.getNPlayers()));
            state.scores[p] += GolfSixUtils.gridScore(grid.getComponents(), params);
        }
        int dealsPlayed = state.getRoundCounter() + 1;
        if (dealsPlayed >= params.nDeals) {
            endGame(state);
            return;
        }
        // the deal passes to the next player, so the player after them plays first
        endRound(state, dealsPlayed % state.getNPlayers());
        deal(state);
    }
}
