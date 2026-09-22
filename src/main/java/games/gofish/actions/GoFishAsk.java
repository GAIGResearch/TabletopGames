package games.gofish.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.gofish.GoFishForwardModel;
import games.gofish.GoFishGameState;
import games.gofish.GoFishKnownVoids;
import games.gofish.GoFishParameters;

import java.util.Arrays;

/**
 * The current player asks another player for all their cards of a rank the asker holds. If the target has none, the
 * asker draws a card from the draw deck ("Go fish").
 */
public class GoFishAsk extends AbstractAction {

    public final int targetPlayer;
    public final int rank;

    public GoFishAsk(int targetPlayer, int rank) {
        this.targetPlayer = targetPlayer;
        this.rank = rank;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        GoFishGameState state = (GoFishGameState) gameState;
        GoFishParameters params = (GoFishParameters) state.getGameParameters();
        GoFishKnownVoids knownVoids = state.getKnownVoids();
        int asker = state.getCurrentPlayer();
        PartialObservableDeck<FrenchCard> askerHand = state.getPlayerHands().get(asker);
        PartialObservableDeck<FrenchCard> targetHand = state.getPlayerHands().get(targetPlayer);
        boolean[] allVisible = new boolean[state.getNPlayers()];
        Arrays.fill(allVisible, true);

        // asking shows the table that the asker holds the rank
        if (!holdsVisibleToAll(askerHand, rank))
            for (int i = 0; i < askerHand.getSize(); i++)
                if (askerHand.get(i).number == rank) {
                    askerHand.setVisibilityOfComponent(i, allVisible);
                    break;
                }

        knownVoids.record(targetPlayer, rank);
        if (state.playerHasRank(targetPlayer, rank)) {
            for (FrenchCard c : GoFishForwardModel.removeCardsOfRank(targetHand, rank).getComponents())
                askerHand.add(c, allVisible);
            knownVoids.received(asker, rank);
            state.setExtraTurn(params.continueOnSuccess);
        } else {
            // Go fish: the drawn card is private, unless it is the rank asked for and is shown to take another turn
            FrenchCard drawn = state.getDrawDeck().draw();
            if (drawn == null) {
                // the draw deck is empty (GoFishParameters.playUntilAllBooks)
                state.setExtraTurn(false);
                return true;
            }
            boolean shown = params.continueOnDrawingSameRank && drawn.number == rank;
            if (shown)
                askerHand.add(drawn, allVisible);
            else
                askerHand.add(drawn);
            knownVoids.drew(asker);
            state.setExtraTurn(shown);
        }
        return true;
    }

    private static boolean holdsVisibleToAll(PartialObservableDeck<FrenchCard> hand, int rank) {
        for (int i = 0; i < hand.getSize(); i++) {
            if (hand.get(i).number != rank) continue;
            boolean visibleToAll = true;
            for (boolean v : hand.getVisibilityOfComponent(i))
                visibleToAll &= v;
            if (visibleToAll)
                return true;
        }
        return false;
    }

    @Override
    public GoFishAsk copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GoFishAsk other)) return false;
        return targetPlayer == other.targetPlayer && rank == other.rank;
    }

    @Override
    public int hashCode() {
        return 31 * targetPlayer + rank + 294017;
    }

    @Override
    public String toString() {
        String rankName = switch (rank) {
            case 14 -> "Aces";
            case 11 -> "Jacks";
            case 12 -> "Queens";
            case 13 -> "Kings";
            default -> rank + "s";
        };
        return "Ask P" + targetPlayer + " for " + rankName;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
