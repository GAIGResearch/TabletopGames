package games.gofish.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.gofish.GoFishGameState;

import java.util.List;
import java.util.Objects;

/**
 * Player asks another player for all cards of a specific rank.
 * If target has cards, asker gets them and continues turn (handled by FM).
 * If not, FM will require a draw ("Go Fish").
 */
public class GoFishAsk extends AbstractAction {

    public final int askingPlayer;
    public final int targetPlayer;
    public final int rankAsked;

    // Outcome flag set during execute(), read by ForwardModel._afterAction
    public boolean receivedCards = false;

    public GoFishAsk(int askingPlayer, int targetPlayer, int rankAsked) {
        this.askingPlayer = askingPlayer;
        this.targetPlayer = targetPlayer;
        this.rankAsked = rankAsked;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        GoFishGameState state = (GoFishGameState) gameState;

        // Store requested rank to support "draw the same rank" rule in FM
        state.lastRequestedRank = this.rankAsked;

        // Transfer all cards of that rank if target has them
        if (state.playerHasRank(targetPlayer, rankAsked)) {
            List<FrenchCard> cards = state.removeCardsOfRank(targetPlayer, rankAsked);
            for (FrenchCard c : cards) {
                state.getPlayerHands().get(askingPlayer).add(c);
            }
            receivedCards = true; // same player continues (FM decides)
        } else {
            receivedCards = false; // FM will force a draw or pass
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        // Important: do NOT carry over rollout outcome flags into the copy.
        return new GoFishAsk(askingPlayer, targetPlayer, rankAsked);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GoFishAsk)) return false;
        GoFishAsk other = (GoFishAsk) obj;
        // Equality is based on intent (who asks whom for what), not the outcome.
        return askingPlayer == other.askingPlayer
                && targetPlayer == other.targetPlayer
                && rankAsked == other.rankAsked;
    }

    @Override
    public int hashCode() {
        // Do NOT include receivedCards in hash (it changes after execute()).
        return Objects.hash(askingPlayer, targetPlayer, rankAsked);
    }

    @Override
    public String toString() {
        String rankName = switch (rankAsked) {
            case 1 -> "Aces";
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
