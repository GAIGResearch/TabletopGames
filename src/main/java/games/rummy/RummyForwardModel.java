package games.rummy;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.rummy.actions.Discard;
import games.rummy.actions.DrawCard;
import games.rummy.actions.LayOff;
import games.rummy.actions.Meld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RummyForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        RummyGameState state = (RummyGameState) firstState;
        int nPlayers = state.getNPlayers();
        state.playerHands = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            state.playerHands.add(new PartialObservableDeck<>("Player" + p + "Hand", p, nPlayers,
                    CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
        state.discardPile = new Deck<>("DiscardPile", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        state.drawDeck = new Deck<>("DrawDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        state.playerScores = new int[nPlayers];
        // the dealer is the last player, so player 0 is dealt to first and plays first
        deal(state, 0);
        state.setFirstPlayer(0);
    }

    /**
     * Deals a new hand from all 52 cards, starting with the given player, who will play first.
     */
    void deal(RummyGameState state, int first) {
        RummyParameters params = (RummyParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        for (Deck<FrenchCard> hand : state.playerHands)
            hand.clear();
        state.discardPile.clear();
        state.melds = new ArrayList<>();
        state.takenCard = null;
        state.meldedThisTurn = false;
        state.drawDeck.clear();
        state.drawDeck.add(FrenchCard.generateDeck("DrawDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL));
        state.drawDeck.shuffle(state.getRnd());
        // one card at a time, then the next card starts the discard pile
        for (int i = 0; i < params.handSize(nPlayers); i++)
            for (int p = 0; p < nPlayers; p++)
                state.playerHands.get((first + p) % nPlayers).add(state.drawDeck.draw());
        state.discardPile.add(state.drawDeck.draw());
        state.setGamePhase(RummyGameState.Phase.DRAW);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        RummyGameState state = (RummyGameState) gameState;
        List<AbstractAction> actions = new ArrayList<>();
        if (state.getGamePhase() == RummyGameState.Phase.DRAW) {
            // the game ends when the draw deck empties, so it always has a card here
            actions.add(new DrawCard(false));
            if (state.getDiscardPile().getSize() > 0)
                actions.add(new DrawCard(true));
            return actions;
        }
        Deck<FrenchCard> hand = state.getPlayerHand(state.getCurrentPlayer());
        if (!state.meldedThisTurn)
            addMelds(hand, actions);
        for (FrenchCard card : hand.getComponents())
            for (LayOff.Position position : LayOff.Position.values())
                if (state.meldFor(card, position) != null)
                    actions.add(new LayOff(card, position));
        for (FrenchCard card : hand.getComponents()) {
            // the card taken from the discard pile this turn may be discarded only as the last card
            if (!card.equals(state.getTakenCard()) || hand.getSize() == 1)
                actions.add(new Discard(card));
        }
        return actions;
    }

    /**
     * Scores the deal when playing to RummyParameters.targetScore, then ends the game or deals again.
     */
    void endDeal(RummyGameState state) {
        RummyParameters params = (RummyParameters) state.getGameParameters();
        if (params.targetScore == 0) {
            endGame(state);
            return;
        }
        // the winner is the player who went out, or else the single player with the fewest points in hand
        int nPlayers = state.getNPlayers();
        int winner = -1, fewest = Integer.MAX_VALUE, total = 0;
        for (int p = 0; p < nPlayers; p++) {
            int points = state.handPoints(p);
            total += points;
            if (points < fewest) {
                fewest = points;
                winner = p;
            } else if (points == fewest) {
                winner = -1;
            }
        }
        // the winner scores the points in the other hands; nobody scores when the fewest points are tied
        if (winner >= 0)
            state.playerScores[winner] += total - fewest;
        if (Arrays.stream(state.playerScores).anyMatch(s -> s >= params.targetScore)) {
            endGame(state);
            return;
        }
        int first = (state.getRoundCounter() + 1) % nPlayers;
        endRound(state, first);
        // endRound ends the game itself after the maximum number of deals
        if (state.isNotTerminal())
            deal(state, first);
    }

    /**
     * Adds a Meld for every set and run that can be made from the hand, including the parts of larger ones.
     */
    private void addMelds(Deck<FrenchCard> hand, List<AbstractAction> actions) {
        // the cards held, by suit and rank (Aces low)
        FrenchCard[][] held = new FrenchCard[FrenchCard.Suite.values().length][14];
        for (FrenchCard card : hand.getComponents())
            held[card.suite.ordinal()][RummyUtils.rank(card)] = card;
        for (int rank = 1; rank <= 13; rank++) {
            List<FrenchCard> ofRank = new ArrayList<>();
            for (FrenchCard[] suit : held)
                if (suit[rank] != null) ofRank.add(suit[rank]);
            if (ofRank.size() >= 3)
                actions.add(new Meld(ofRank));
            if (ofRank.size() == 4) {
                // and each set of three
                for (FrenchCard left : ofRank) {
                    List<FrenchCard> three = new ArrayList<>(ofRank);
                    three.remove(left);
                    actions.add(new Meld(three));
                }
            }
        }
        for (FrenchCard[] suit : held)
            for (int low = 1; low <= 11; low++)
                for (int high = low; high <= 13 && suit[high] != null; high++)
                    if (high - low >= 2)
                        actions.add(new Meld(Arrays.asList(suit).subList(low, high + 1)));
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken) {
        RummyGameState state = (RummyGameState) currentState;
        RummyParameters params = (RummyParameters) state.getGameParameters();
        boolean handEmpty = state.getPlayerHand(state.getCurrentPlayer()).getSize() == 0;
        // a discard ends the turn, as does a meld or lay-off that empties the hand
        if (actionTaken instanceof DrawCard || (!(actionTaken instanceof Discard) && !handEmpty)) return;
        state.takenCard = null;
        state.meldedThisTurn = false;
        state.setGamePhase(RummyGameState.Phase.DRAW);
        endPlayerTurn(state);
        if (handEmpty || state.getDrawDeck().getSize() == 0 || state.getTurnCounter() >= params.maxTurnsPerDeal)
            endDeal(state);
    }
}
