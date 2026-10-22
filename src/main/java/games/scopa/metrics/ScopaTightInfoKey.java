package games.scopa.metrics;

import core.AbstractGameState;
import core.components.TarotCard;
import core.interfaces.IStateKey;
import games.scopa.ScopaGameState;

import java.util.*;

/**
 * The information state of a Scopa player: everything they know, where a card is known if its identity is known
 * exactly. Cards are identified by suit and rank.
 */
public class ScopaTightInfoKey implements IStateKey {

    /**
     * @param captured for each player, the cards they have captured this deal
     */
    public record Key(List<Integer> hand, List<Integer> table, List<List<Integer>> captured, List<Integer> scopas,
                      int lastCapturer, int drawDeckSize, List<Integer> handSizes, List<Integer> bankedScores,
                      int currentPlayer) {
    }

    @Override
    public Key getKey(AbstractGameState gameState, int playerId) {
        ScopaGameState state = (ScopaGameState) gameState;
        List<List<Integer>> captured = new ArrayList<>();
        List<Integer> scopas = new ArrayList<>();
        List<Integer> handSizes = new ArrayList<>();
        List<Integer> bankedScores = new ArrayList<>();
        for (int p = 0; p < state.getNPlayers(); p++) {
            captured.add(sortedCodes(state.getCapturedCards(p).getComponents()));
            scopas.add(state.getScopas(p));
            handSizes.add(state.getPlayerHand(p).getSize());
            bankedScores.add(state.getBankedScore(p));
        }
        return new Key(sortedCodes(state.getPlayerHand(playerId).getComponents()),
                sortedCodes(state.getTable().getComponents()), captured, scopas, state.getLastCapturer(),
                state.getDrawDeck().getSize(), handSizes, bankedScores, state.getCurrentPlayer());
    }

    static List<Integer> sortedCodes(List<TarotCard> cards) {
        return cards.stream().map(c -> c.suit.ordinal() * 100 + c.number).sorted().toList();
    }
}
