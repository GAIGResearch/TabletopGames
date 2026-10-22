package games.poker.metrics;

import core.AbstractGameState;
import core.components.Deck;
import core.components.FrenchCard;
import core.interfaces.IStateHeuristic;
import games.poker.PokerGameState;
import utilities.Pair;

import java.util.*;

public class PokerHandHeuristic extends PokerHandFeatures implements IStateHeuristic {

    public PokerHandHeuristic() {
        super();
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        PokerGameState pgs = (PokerGameState) gs;
        List<FrenchCard> cards = new ArrayList<>(pgs.getPlayerDecks().get(playerId).getComponents());
        cards.addAll(pgs.getCommunityCards().getComponents());
        
        Deck<FrenchCard> tempDeck = new Deck<>("Temp", core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        for (FrenchCard card : cards) {
            tempDeck.add(new FrenchCard(card.type, card.suite, card.number));
        }
        
        Pair<PokerGameState.PokerHand, HashSet<Integer>> hand = PokerGameState.PokerHand.translateHand(tempDeck);
        if (hand == null) {
            return 0.0;
        }
        
        // The rank in PokerHand is 1 for RoyalFlush, 10 for HighCard (lower is better)
        // IStateHeuristic expects higher is better, ideally bounded between [-1, 1]
        // We can map rank 1..10 to 1.0..0.1
        
        return (11 - hand.a.rank) / 10.0;
    }

    // no settings (any fields are fixed, or worked out from the game): any two are the same heuristic, so that one
    // read back from JSON equals the one written
    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass();
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode();
    }
}
