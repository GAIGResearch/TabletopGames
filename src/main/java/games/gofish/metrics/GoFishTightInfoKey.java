package games.gofish.metrics;

import core.AbstractGameState;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import core.interfaces.IStateKey;
import games.gofish.GoFishGameState;

import java.util.*;

/**
 * The information state of a Go Fish player: everything they know, where a card is known if its identity is known
 * exactly. Cards are identified by suit and rank.
 */
public class GoFishTightInfoKey implements IStateKey {

    /**
     * @param knownHands for each player, the cards in their hand that the player whose key this is can see (all of
     *                   their own hand)
     * @param books      for each player, the ranks of the books they have laid down
     * @param knownVoids for each player, the ranks they are known not to hold
     */
    public record Key(List<List<Integer>> knownHands, List<Integer> handSizes, List<List<Integer>> books,
                      int drawDeckSize, List<List<Integer>> knownVoids, int currentPlayer, boolean extraTurn) {
    }

    @Override
    public Key getKey(AbstractGameState gameState, int playerId) {
        GoFishGameState state = (GoFishGameState) gameState;
        List<List<Integer>> knownHands = new ArrayList<>();
        List<Integer> handSizes = new ArrayList<>();
        List<List<Integer>> books = new ArrayList<>();
        List<List<Integer>> knownVoids = new ArrayList<>();
        for (int p = 0; p < state.getNPlayers(); p++) {
            PartialObservableDeck<FrenchCard> hand = state.getPlayerHands().get(p);
            knownHands.add(hand.getVisibleComponents(playerId).stream().filter(Objects::nonNull)
                    .map(c -> c.suite.ordinal() * 100 + c.number).sorted().toList());
            handSizes.add(hand.getSize());
            books.add(state.getPlayerBooks().get(p).stream().map(c -> c.number).distinct().sorted().toList());
            knownVoids.add(List.copyOf(state.getKnownVoids().get(p)));  // a sorted set
        }
        return new Key(knownHands, handSizes, books, state.getDrawDeck().getSize(), knownVoids,
                state.getCurrentPlayer(), state.isExtraTurn());
    }
}
