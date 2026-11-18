package games.gofish.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.gofish.GoFishForwardModel;
import games.gofish.GoFishGameState;
import games.gofish.GoFishKnowledge;
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
        showAskersCard(state, askerHand, allVisible);

        knownVoids.record(targetPlayer, rank);
        if (state.playerHasRank(targetPlayer, rank)) {
            for (FrenchCard c : GoFishForwardModel.removeCardsOfRank(targetHand, rank).getComponents()) {
                state.forgetShown(c);
                askerHand.add(c, allVisible);
                state.recordShown(c, GoFishKnowledge.CARDS_FISHED_FOR);
            }
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
            if (shown) {
                askerHand.add(drawn, allVisible);
                state.recordShown(drawn, GoFishKnowledge.CARDS_FISHED_FOR);
            } else
                askerHand.add(drawn);
            knownVoids.drew(asker);
            state.setExtraTurn(shown);
        }
        return true;
    }

    /**
     * Shows one of the asker's cards of the rank, recorded as {@link GoFishKnowledge#ASKERS_CARD}, unless one already
     * is. A card of the rank already shown for another reason is preferred, so the table sees nothing new.
     */
    private void showAskersCard(GoFishGameState state, PartialObservableDeck<FrenchCard> hand, boolean[] allVisible) {
        int chosen = -1;
        boolean chosenVisible = false;
        for (int i = 0; i < hand.getSize(); i++) {
            FrenchCard c = hand.get(i);
            if (c.number != rank) continue;
            if (state.isShownBecause(c, GoFishKnowledge.ASKERS_CARD))
                return;
            boolean visible = visibleToAll(hand, i);
            if (chosen < 0 || (visible && !chosenVisible)) {
                chosen = i;
                chosenVisible = visible;
            }
        }
        if (chosen < 0) return;
        hand.setVisibilityOfComponent(chosen, allVisible);
        state.recordShown(hand.get(chosen), GoFishKnowledge.ASKERS_CARD);
    }

    private static boolean visibleToAll(PartialObservableDeck<FrenchCard> hand, int i) {
        for (boolean v : hand.getVisibilityOfComponent(i))
            if (!v) return false;
        return true;
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
