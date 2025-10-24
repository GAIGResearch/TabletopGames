package games.gofish.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.gofish.GoFishGameState;

import java.util.List;
import java.util.Objects;
import java.util.Arrays;

/**
 * Player asks another player for all cards of a specific rank.
 * If target has cards, asker gets them and continues turn (handled by FM).
 * If not, FM will require a draw ("Go Fish").
 */
public class GoFishAsk extends AbstractAction {

    public final int targetPlayer;
    public final int rankAsked;

    // Outcome flag set during execute(), read by ForwardModel._afterAction
    public boolean receivedCards = false;

    public GoFishAsk(int targetPlayer, int rankAsked) {
        this.targetPlayer = targetPlayer;
        this.rankAsked = rankAsked;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        GoFishGameState state = (GoFishGameState) gameState;
        // Transfer all cards of that rank if target has them
        if (state.playerHasRank(targetPlayer, rankAsked)) {
            List<FrenchCard> transferred = state.removeCardsOfRank(targetPlayer, rankAsked);
            int currentPlayer = state.getCurrentPlayer();
            PartialObservableDeck<FrenchCard> askerHand = state.getPlayerHands().get(currentPlayer);

            // Add transferred cards to asker's hand
            for (FrenchCard c : transferred) {
                askerHand.add(c);
            }
            receivedCards = true; // same player continues (FM decides)

            // Visibility update: preserve any pre-existing "visible to all" cards, then reveal transferred cards first,
            // and reveal one pre-existing card only if asker had pre-existing cards but none were visible-to-all before.
            boolean[] allVisible = new boolean[state.getNPlayers()];
            Arrays.fill(allVisible, true);
            boolean[] noneVisible = new boolean[state.getNPlayers()];
            Arrays.fill(noneVisible, false);
            noneVisible[currentPlayer] = true;

            // Collect indices of all cards of the asked rank in the asker's hand
            List<Integer> transferredIndices = new java.util.ArrayList<>();
            List<Integer> preexistingInvisibleIndices = new java.util.ArrayList<>();
            List<Integer> preexistingVisibleIndices = new java.util.ArrayList<>();

            for (int i = 0; i < askerHand.getSize(); i++) {
                FrenchCard fc = askerHand.get(i);
                if (fc.number != rankAsked) continue;
                // check if currently visible to all players
                boolean visibleToAll = true;
                for (int p = 0; p < state.getNPlayers(); p++) {
                    if (!askerHand.isComponentVisible(i, p)) { visibleToAll = false; break; }
                }
                if (visibleToAll) {
                    preexistingVisibleIndices.add(i);
                } else {
                    // check if this object is one of the transferred ones (identity)
                    boolean isTransferred = false;
                    for (FrenchCard tc : transferred) {
                        if (askerHand.get(i) == tc) { isTransferred = true; break; }
                    }
                    if (isTransferred) transferredIndices.add(i);
                    else preexistingInvisibleIndices.add(i);
                }
            }

            // compute whether asker had pre-existing cards (after transfer math)
            int totalRankAfter = transferredIndices.size() + preexistingInvisibleIndices.size() + preexistingVisibleIndices.size();
            int preExistingCount = Math.max(0, totalRankAfter - transferred.size());

            int needVisible = transferred.size() + (preExistingCount > 0 && preexistingVisibleIndices.isEmpty() ? 1 : 0);

            // Do NOT hide any indices that are already visible to all; hide only the non-visible ones for determinism
            for (int idx : transferredIndices) askerHand.setVisibilityOfComponent(idx, noneVisible);
            for (int idx : preexistingInvisibleIndices) askerHand.setVisibilityOfComponent(idx, noneVisible);

            // Reveal transferred indices first
            int revealed = 0;
            for (int idx : transferredIndices) {
                if (revealed >= needVisible) break;
                askerHand.setVisibilityOfComponent(idx, allVisible);
                revealed++;
            }
            // If still need more (i.e., transferred < needVisible), reveal some preexisting invisible indices
            for (int idx : preexistingInvisibleIndices) {
                if (revealed >= needVisible) break;
                askerHand.setVisibilityOfComponent(idx, allVisible);
                revealed++;
            }

        } else {
            receivedCards = false; // FM will force a draw or pass
        }
        return true;
    }

    @Override
    public GoFishAsk copy() {
        GoFishAsk retValue =  new GoFishAsk(targetPlayer, rankAsked);
        retValue.receivedCards = this.receivedCards;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GoFishAsk other)) return false;
        // Equality is based on intent (who asks whom for what), not the outcome.
        return targetPlayer == other.targetPlayer
                && receivedCards == other.receivedCards
                && rankAsked == other.rankAsked;
    }

    @Override
    public int hashCode() {
        // Do NOT include receivedCards in hash (it changes after execute()).
        return Objects.hash(targetPlayer, rankAsked, receivedCards);
    }

    @Override
    public String toString() {
        String rankName = switch (rankAsked) {
            case 14 -> "Aces";
            case 11 -> "Jacks";
            case 12 -> "Queens";
            case 13 -> "Kings";
            default -> rankAsked + "s";
        };
        return "Ask P" + targetPlayer + " for " + rankName;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
