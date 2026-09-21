package games.whist;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.tricktaking.KnownVoids;
import games.tricktaking.PlayCard;
import games.tricktaking.PlayRule;
import games.tricktaking.Trick;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.GameResult.*;
import static core.CoreConstants.VisibilityMode.*;

/**
 * <p>The forward model contains all the game rules and logic for Whist.</p>
 */
public class WhistForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        WhistGameState state = (WhistGameState) firstState;
        int nPlayers = state.getNPlayers();

        state.playerHands = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            state.playerHands.add(new Deck<>("Hand " + p, p, VISIBLE_TO_OWNER));
        // all 52 cards start in the discard pile, and are gathered from there for the deal
        state.discardPile = FrenchCard.generateDeck("DiscardPile", VISIBLE_TO_ALL);
        state.tricksTaken = new int[nPlayers];
        state.teamPoints = new int[state.getNTeams()];
        state.knownVoids = new KnownVoids(nPlayers);

        // the last player deals first, so player 0 (on the dealer's left) leads the first trick
        state.currentTrick = new Trick("CurrentTrick", nPlayers, 0);
        deal(state);
        state.setFirstPlayer(0);
    }

    /**
     * Gathers all the cards, shuffles them and deals them out one at a time, starting on the dealer's left, so the
     * last card goes to the dealer. Sets the trump suit, and clears the tricks taken and known voids.
     */
    void deal(WhistGameState state) {
        int nPlayers = state.getNPlayers();
        Deck<FrenchCard> deck = new Deck<>("Deck", HIDDEN_TO_ALL);
        for (Deck<FrenchCard> hand : state.playerHands) {
            deck.add(hand);
            hand.clear();
        }
        deck.add(state.currentTrick);
        state.currentTrick = new Trick("CurrentTrick", nPlayers, (state.getDealer() + 1) % nPlayers);
        deck.add(state.discardPile);
        state.discardPile.clear();
        deck.shuffle(state.getRnd());

        FrenchCard card = null;
        for (int i = 0; deck.getSize() > 0; i++) {
            card = deck.draw();
            state.playerHands.get((state.getDealer() + 1 + i) % nPlayers).add(card);
        }
        setTrumps(state, card);

        state.tricksTaken = new int[nPlayers];
        state.knownVoids.clear();
    }

    /**
     * Sets the trump suit for the deal, as WhistParameters.trumpMode chooses it.
     *
     * @param lastCard the last card dealt, which the dealer holds
     */
    private void setTrumps(WhistGameState state, FrenchCard lastCard) {
        WhistParameters params = (WhistParameters) state.getGameParameters();
        switch (params.trumpMode) {
            case TURN_UP -> {
                state.trumpCard = lastCard;
                state.trumpSuit = lastCard.suite;
            }
            case ROTATION -> {
                state.trumpCard = null;
                state.trumpSuit = params.rotationTrumps(state.getRoundCounter());
            }
        }
    }

    /**
     * One PlayCard for each card the current player may play: any card to lead, and then a card of the suit led if
     * they hold one.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        WhistGameState state = (WhistGameState) gameState;
        List<FrenchCard> hand = state.getPlayerHand(state.getCurrentPlayer()).getComponents();
        List<AbstractAction> actions = new ArrayList<>();
        for (FrenchCard card : PlayRule.FOLLOW_SUIT.legalPlays(hand, state.currentTrick))
            actions.add(new PlayCard(card));
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        WhistGameState state = (WhistGameState) currentState;
        Trick trick = state.currentTrick;
        if (!trick.isComplete()) {
            endPlayerTurn(state);
            return;
        }
        // the winner of the trick takes it and leads the next one
        int winner = trick.winner(state.trumpSuit);
        state.tricksTaken[winner]++;
        state.discardPile.add(trick);
        state.currentTrick = new Trick("CurrentTrick", state.getNPlayers(), winner);
        if (state.playerHands.stream().anyMatch(h -> h.getSize() > 0)) {
            endPlayerTurn(state, winner);
            return;
        }
        scoreDeal(state);
        WhistParameters params = (WhistParameters) state.getGameParameters();
        if (state.getRoundCounter() + 1 >= params.nDeals) {
            endGame(state);
        } else {
            // the deal passes to the left, and the player on the new dealer's left leads
            endRound(state, (state.getDealer() + 2) % state.getNPlayers());
            if (state.isNotTerminal())  // endRound ends the game at the framework's maxRounds
                deal(state);
        }
    }

    /**
     * The side that won more tricks scores one point for each trick over six.
     */
    private void scoreDeal(WhistGameState state) {
        for (int team = 0; team < state.getNTeams(); team++) {
            int tricks = state.getTeamTricks(team);
            if (tricks > 6)
                state.teamPoints[team] += tricks - 6;
        }
    }

    /**
     * Both partners of the side with more points win, and the other side loses; equal points is a draw.
     */
    @Override
    protected void endGame(AbstractGameState gs) {
        WhistGameState state = (WhistGameState) gs;
        state.setGameStatus(GAME_END);
        int difference = state.getTeamPoints(0) - state.getTeamPoints(1);
        for (int p = 0; p < state.getNPlayers(); p++) {
            int lead = state.getTeam(p) == 0 ? difference : -difference;
            state.setPlayerResult(lead > 0 ? WIN_GAME : lead < 0 ? LOSE_GAME : DRAW_GAME, p);
        }
    }
}
