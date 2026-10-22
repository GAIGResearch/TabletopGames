package games.whist.metrics;

import core.AbstractGameState;
import core.components.FrenchCard;
import core.interfaces.IStateKey;
import games.tricktaking.CardOrder;
import games.tricktaking.Trick;
import games.whist.WhistGameState;

import java.util.*;

/**
 * The information state of a Whist player: everything they know, where a card is known if its identity is known
 * exactly. Cards are identified by suit and rank.
 */
public class WhistTightInfoKey implements IStateKey {

    /**
     * @param currentTrick  [player, card] for each card in the trick in progress, in the order played
     * @param playedCards   for each player, the cards they played to the completed tricks of this deal
     * @param trumpCard     the dealer's turned-up card while it is still in their hand, otherwise -1
     */
    public record Key(List<Integer> hand, List<List<Integer>> currentTrick, List<List<Integer>> playedCards,
                      int trumpSuit, int trumpCard, List<Integer> tricksTaken, List<List<Integer>> knownVoids,
                      int currentPlayer, int deal) {
    }

    @Override
    public Key getKey(AbstractGameState gameState, int playerId) {
        WhistGameState state = (WhistGameState) gameState;
        int nPlayers = state.getNPlayers();
        Trick<FrenchCard, FrenchCard.Suite> trick = state.getCurrentTrick();

        List<List<Integer>> currentTrick = new ArrayList<>();
        for (int i = 0; i < trick.getSize(); i++)
            currentTrick.add(List.of(trick.playerOf(i), code(trick.get(i))));

        List<List<Integer>> playedCards = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            playedCards.add(new ArrayList<>());
        // The discard pile holds the completed tricks of this deal, the most recent first, each in the order its
        // cards were played. The first trick was led by the player on the dealer's left, and each later one by the
        // winner of the trick before.
        List<FrenchCard> discards = state.getDiscardPile().getComponents();
        int leader = (state.getDealer() + 1) % nPlayers;
        for (int start = discards.size() - nPlayers; start >= 0; start -= nPlayers) {
            Trick<FrenchCard, FrenchCard.Suite> completed = new Trick<>("Completed", nPlayers, leader, CardOrder.STANDARD);
            for (int i = 0; i < nPlayers; i++)
                completed.play(discards.get(start + i));
            for (int i = 0; i < nPlayers; i++)
                playedCards.get(completed.playerOf(i)).add(code(completed.get(i)));
            leader = completed.winner(state.getTrumpSuit());
        }
        playedCards.forEach(Collections::sort);

        FrenchCard trumpCard = state.getTrumpCard();
        boolean trumpCardHeld = trumpCard != null && !state.getDiscardPile().contains(trumpCard) && !trick.contains(trumpCard);

        List<Integer> tricksTaken = new ArrayList<>();
        List<List<Integer>> knownVoids = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++) {
            tricksTaken.add(state.getTricksTaken(p));
            knownVoids.add(state.getKnownVoids().get(p).stream().map(Enum::ordinal).sorted().toList());
        }

        return new Key(sortedCodes(state.getPlayerHand(playerId).getComponents()), currentTrick, playedCards,
                state.getTrumpSuit() == null ? -1 : state.getTrumpSuit().ordinal(),
                trumpCardHeld ? code(trumpCard) : -1,
                tricksTaken, knownVoids, state.getCurrentPlayer(), state.getRoundCounter());
    }

    static int code(FrenchCard card) {
        return card.suite.ordinal() * 100 + card.number;
    }

    static List<Integer> sortedCodes(List<FrenchCard> cards) {
        return cards.stream().map(WhistTightInfoKey::code).sorted().toList();
    }
}
