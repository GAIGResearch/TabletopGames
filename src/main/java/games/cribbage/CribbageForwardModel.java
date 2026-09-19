package games.cribbage;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.cribbage.actions.DiscardToCrib;
import games.cribbage.actions.PlayCard;

import java.util.ArrayList;
import java.util.List;

import static core.CoreConstants.VisibilityMode.*;
import static games.cribbage.CribbageGameState.CribbageGamePhase.Discard;
import static games.cribbage.CribbageGameState.CribbageGamePhase.Play;

/**
 * <p>The forward model contains all the game rules and logic for Cribbage.</p>
 */
public class CribbageForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        CribbageGameState state = (CribbageGameState) firstState;
        int nPlayers = state.getNPlayers();

        state.drawDeck = FrenchCard.generateDeck("DrawDeck", HIDDEN_TO_ALL);
        state.playerHands = new ArrayList<>();
        state.playedCards = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++) {
            state.playerHands.add(new Deck<>("Hand " + p, p, VISIBLE_TO_OWNER));
            state.playedCards.add(new Deck<>("Played " + p, p, VISIBLE_TO_ALL));
        }
        state.crib = new PartialObservableDeck<>("Crib", -1, nPlayers, HIDDEN_TO_ALL);
        state.playSequence = new ArrayList<>();
        state.scores = new int[nPlayers];
        deal(state);
    }

    /**
     * Shuffles the draw deck and deals each player CribbageParameters.nCardsDealt cards, one at a time starting
     * with the non-dealer. The non-dealer then discards to the crib first.
     */
    void deal(CribbageGameState state) {
        CribbageParameters params = (CribbageParameters) state.getGameParameters();
        state.drawDeck.shuffle(state.getRnd());
        for (int i = 0; i < params.nCardsDealt; i++)
            for (int p = 0; p < state.getNPlayers(); p++)
                state.playerHands.get((state.getNonDealer() + p) % state.getNPlayers()).add(state.drawDeck.draw());
        state.setGamePhase(Discard);
        state.setFirstPlayer(state.getNonDealer());
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        CribbageGameState state = (CribbageGameState) gameState;
        List<FrenchCard> hand = state.getPlayerHand(state.getCurrentPlayer()).getComponents();
        List<AbstractAction> actions = new ArrayList<>();
        if (state.getGamePhase() == Discard) {
            for (int i = 0; i < hand.size(); i++)
                for (int j = i + 1; j < hand.size(); j++)
                    actions.add(new DiscardToCrib(hand.get(i), hand.get(j)));
        } else {
            for (FrenchCard card : hand)
                if (state.canPlay(card))
                    actions.add(new PlayCard(card));
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        CribbageGameState state = (CribbageGameState) currentState;
        if (actionTaken instanceof DiscardToCrib)
            afterDiscard(state);
        else if (actionTaken instanceof PlayCard)
            afterPlay(state);
    }

    /**
     * Once the non-dealer has discarded, the dealer discards. Then the starter is turned up
     * (the dealer scores heels for a Jack) and the non-dealer leads the play.
     */
    private void afterDiscard(CribbageGameState state) {
        CribbageParameters params = (CribbageParameters) state.getGameParameters();
        if (state.crib.getSize() < params.nCardsToCrib * state.getNPlayers()) {
            endPlayerTurn(state, state.getDealer());
            return;
        }
        state.starter = state.drawDeck.draw();
        if (state.starter.type == FrenchCard.FrenchCardType.Jack)
            state.scores[state.getDealer()] += params.hisHeelsPoints;
        if (state.targetReached()) {
            endGame(state);
            return;
        }
        state.setGamePhase(Play);
        endPlayerTurn(state, state.getNonDealer());
    }

    /**
     * Scores the card just played, then decides who plays next. The opponent plays if they can; if not, the
     * player carries on alone (the opponent has said "go"). When neither can play, or the total is exactly
     * CribbageParameters.maxCount, the count ends and the opponent of the last player leads the next one
     * (or the last player, if the opponent has no cards). When both hands are empty the round ends.
     */
    private void afterPlay(CribbageGameState state) {
        CribbageParameters params = (CribbageParameters) state.getGameParameters();
        int player = state.getCurrentPlayer();
        int opponent = (player + 1) % state.getNPlayers();
        int total = state.getRunningTotal();

        if (total == 15)
            state.scores[player] += params.playFifteenPoints;
        state.scores[player] += CribbageUtils.playPairPoints(state.playSequence, params)
                + CribbageUtils.playRunPoints(state.playSequence);

        boolean countEnds;
        if (total == params.maxCount) {
            state.scores[player] += params.thirtyOnePoints;
            countEnds = true;
        } else if (!state.canPlay(opponent) && !state.canPlay(player)) {
            state.scores[player] += params.lastCardPoints;
            countEnds = true;
        } else {
            countEnds = false;
        }
        if (state.targetReached()) {
            endGame(state);
            return;
        }

        if (!countEnds) {
            endPlayerTurn(state, state.canPlay(opponent) ? opponent : player);
            return;
        }
        state.playSequence.clear();
        if (state.getPlayerHand(opponent).getSize() > 0)
            endPlayerTurn(state, opponent);
        else if (state.getPlayerHand(player).getSize() > 0)
            endPlayerTurn(state, player);
        else
            finishRound(state);
    }

    /**
     * The show: the non-dealer scores their played cards with the starter, then the dealer scores theirs, then
     * the dealer scores the crib. Counting stops as soon as a player reaches CribbageParameters.targetScore.
     */
    void scoreShow(CribbageGameState state) {
        int nonDealer = state.getNonDealer(), dealer = state.getDealer();
        scoreShowPart(state, nonDealer, state.playedCards.get(nonDealer).getComponents(), false);
        if (state.targetReached()) return;
        scoreShowPart(state, dealer, state.playedCards.get(dealer).getComponents(), false);
        if (state.targetReached()) return;
        scoreShowPart(state, dealer, state.crib.getComponents(), true);
    }

    /**
     * Scores one part of the show: adds CribbageUtils.showScore(cards, starter, isCrib, params) to the
     * player's score. scoreShow calls this once per part, in scoring order (tests override it to record the order).
     */
    void scoreShowPart(CribbageGameState state, int player, List<FrenchCard> cards, boolean isCrib) {
        CribbageParameters params = (CribbageParameters) state.getGameParameters();
        state.scores[player] += CribbageUtils.showScore(cards, state.starter, isCrib, params);
    }

    /**
     * Scores the show, then returns every card to the draw deck and passes the deal to the other player. The game
     * ends if a player reached the target score in the show, or after CribbageParameters.nRounds rounds;
     * otherwise the next round is dealt.
     */
    private void finishRound(CribbageGameState state) {
        CribbageParameters params = (CribbageParameters) state.getGameParameters();
        scoreShow(state);
        if (state.targetReached()) {
            endGame(state);
            return;
        }
        for (int p = 0; p < state.getNPlayers(); p++) {
            state.drawDeck.add(state.playerHands.get(p));
            state.playerHands.get(p).clear();
            state.drawDeck.add(state.playedCards.get(p));
            state.playedCards.get(p).clear();
        }
        state.drawDeck.add(state.crib);
        state.crib.clear();
        state.drawDeck.add(state.starter);
        state.starter = null;
        state.playSequence.clear();

        // the dealer of this round is the non-dealer of the next one and will act first

        endRound(state, state.getDealer());
        if (state.getRoundCounter() >= params.nRounds)
            endGame(state);
        else
            deal(state);
    }
}
