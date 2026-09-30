package games.president;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.president.actions.GiveCard;
import games.president.actions.Pass;
import games.president.actions.PlayCards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PresidentForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        PresidentGameState state = (PresidentGameState) firstState;
        int nPlayers = state.getNPlayers();
        state.playerHands = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            state.playerHands.add(new Deck<>("Player" + p + "Hand", p, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
        state.playPile = new Deck<>("PlayPile", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        state.discardPile = new Deck<>("DiscardPile", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        Arrays.fill(state.playerScores, 0);
        state.scum = -1;
        state.cardsToGive = 0;
        // player 0 is dealt to first, and leads
        deal(state, 0);
        state.setGamePhase(PresidentGameState.Phase.PLAY);
        state.setFirstPlayer(0);
    }

    /**
     * Deals the whole pack one card at a time, starting with the given player.
     */
    void deal(PresidentGameState state, int first) {
        int nPlayers = state.getNPlayers();
        for (Deck<FrenchCard> hand : state.playerHands)
            hand.clear();
        state.playPile.clear();
        state.discardPile.clear();
        state.lastPlayer = -1;
        state.setSize = 0;
        state.passesInRow = 0;
        state.finishingOrder = new ArrayList<>();

        Deck<FrenchCard> pack = FrenchCard.generateDeck("Pack", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        pack.shuffle(state.getRnd());
        for (int i = 0; pack.getSize() > 0; i++)
            state.playerHands.get((first + i) % nPlayers).add(pack.draw());
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        PresidentGameState state = (PresidentGameState) gameState;
        // how many of each number the player holds
        int[] held = new int[15];
        for (FrenchCard card : state.getPlayerHand(state.getCurrentPlayer()).getComponents())
            held[card.number]++;
        List<AbstractAction> actions = new ArrayList<>();
        if (state.getGamePhase() == PresidentGameState.Phase.EXCHANGE) {
            // the President gives back any card
            for (FrenchCard card : state.getPlayerHand(state.getCurrentPlayer()).getComponents())
                actions.add(new GiveCard(card));
        } else if (state.getSetSize() == 0) {
            // the leader plays any set they hold, and may not pass
            for (int number = 2; number <= 14; number++)
                for (int count = 1; count <= held[number]; count++)
                    actions.add(new PlayCards(number, count));
        } else {
            // a follower passes, or plays a set of the same size and a higher rank
            actions.add(new Pass());
            int toBeat = PresidentUtils.rank(state.getPlayPile().peek());
            for (int number = 2; number <= 14; number++)
                if (held[number] >= state.getSetSize() && PresidentUtils.rank(number) > toBeat)
                    actions.add(new PlayCards(number, state.getSetSize()));
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        PresidentGameState state = (PresidentGameState) currentState;
        PresidentParameters params = (PresidentParameters) state.getGameParameters();
        int player = state.getCurrentPlayer();
        if (actionTaken instanceof GiveCard) {
            state.cardsToGive--;
            if (state.cardsToGive == 0) {
                // the President, still to act, leads the first trick
                state.scum = -1;
                state.setGamePhase(PresidentGameState.Phase.PLAY);
            }
            return;
        }
        if (actionTaken instanceof PlayCards && state.getPlayerHand(player).getSize() == 0) {
            // the player goes out
            int place = state.finishingOrder.size();
            state.finishingOrder.add(player);
            if (place == 0) state.playerScores[player] += params.presidentPoints;
            else if (place == 1) state.playerScores[player] += params.vicePresidentPoints;
        }
        int holders = holders(state);
        if (holders <= 1) {
            // the last player holding cards is the Scum
            for (int p = 0; p < state.getNPlayers(); p++)
                if (state.getPlayerHand(p).getSize() > 0) state.finishingOrder.add(p);
            endDeal(state);
            return;
        }
        // the last player to play, if still holding cards, is not among those who must pass
        int othersHolding = state.getPlayerHand(state.lastPlayer).getSize() > 0 ? holders - 1 : holders;
        if (state.passesInRow >= othersHolding) {
            // everyone else has passed, so the pile is cleared. The next holder is then the last player to play, who
            // leads - or, if they went out, the holder after them.
            state.discardPile.add(state.playPile);
            state.playPile.clear();
            state.lastPlayer = -1;
            state.setSize = 0;
            state.passesInRow = 0;
        }
        endPlayerTurn(state, nextHolder(state, player));
    }

    /**
     * Moves the Scum's highest cards to the President once a later deal is dealt, then lets the President give cards
     * back, or lead if PresidentParameters.exchangeCards is 0.
     */
    void startExchange(PresidentGameState state, int president, int scum) {
        PresidentParameters params = (PresidentParameters) state.getGameParameters();
        Deck<FrenchCard> scumHand = state.getPlayerHand(scum);
        for (int i = 0; i < params.exchangeCards && scumHand.getSize() > 0; i++) {
            FrenchCard best = null;
            for (FrenchCard card : scumHand.getComponents()) {
                // the highest rank; of equal ranks, the first in suit order
                if (best == null || PresidentUtils.rank(card) > PresidentUtils.rank(best)
                        || (card.number == best.number && card.suite.ordinal() < best.suite.ordinal()))
                    best = card;
            }
            scumHand.remove(best);
            state.getPlayerHand(president).add(best);
        }
        if (params.exchangeCards > 0) {
            state.setGamePhase(PresidentGameState.Phase.EXCHANGE);
            state.scum = scum;
            state.cardsToGive = params.exchangeCards;
        } else {
            state.setGamePhase(PresidentGameState.Phase.PLAY);
            state.scum = -1;
            state.cardsToGive = 0;
        }
        // the President gives cards back, or leads
        endPlayerTurn(state, president);
    }

    /**
     * Ends the game, or starts the next deal when PresidentParameters.targetScore has not been reached.
     */
    private void endDeal(PresidentGameState state) {
        PresidentParameters params = (PresidentParameters) state.getGameParameters();
        boolean targetReached = Arrays.stream(state.playerScores).anyMatch(s -> s >= params.targetScore);
        if (params.targetScore <= 1 || targetReached) {
            endGame(state);
            return;
        }
        int president = state.finishingOrder.get(0);
        int scum = state.finishingOrder.get(state.finishingOrder.size() - 1);
        endRound(state, president);
        // endRound ends the game itself after maxRounds deals
        if (state.isNotTerminal()) {
            deal(state, president);
            startExchange(state, president, scum);
        }
    }

    private int holders(PresidentGameState state) {
        int holders = 0;
        for (Deck<FrenchCard> hand : state.getPlayerHands())
            if (hand.getSize() > 0) holders++;
        return holders;
    }

    /**
     * @return the first player clockwise after the given one who still holds cards
     */
    private int nextHolder(PresidentGameState state, int player) {
        int next = (player + 1) % state.getNPlayers();
        while (state.getPlayerHand(next).getSize() == 0)
            next = (next + 1) % state.getNPlayers();
        return next;
    }
}
