package games.gofish.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.gofish.GoFishGameState;

import java.util.Objects;

/**
 * Player draws a card from the deck.
 * Used when player must "Go Fish" after unsuccessful ask, or as fallback.
 */
public class GoFishDrawAction extends AbstractAction {

    public final int playerID;

    // Outcome flag: set during execute(), read by ForwardModel._afterAction
    public boolean drewRequestedRank = false;

    // Rank that was asked for (if any), to check the 'lucky draw' rule
    private final int requestedRank;

    public GoFishDrawAction(int playerID) {
        this(playerID, -1);
    }

    public GoFishDrawAction(int playerID, int requestedRank) {
        this.playerID = playerID;
        this.requestedRank = requestedRank;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        GoFishGameState state = (GoFishGameState) gameState;

        if (state.getDrawDeck().getSize() > 0) {
            FrenchCard drawn = state.getDrawDeck().draw();
            state.getPlayerHands().get(playerID).add(drawn);

            if (requestedRank != -1 && drawn.number == requestedRank) {
                drewRequestedRank = true;
            }
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        // Important: do NOT carry over runtime outcome flags into the copy.
        return new GoFishDrawAction(playerID, requestedRank);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GoFishDrawAction)) return false;
        GoFishDrawAction other = (GoFishDrawAction) obj;
        // Equality based on intent only
        return playerID == other.playerID && requestedRank == other.requestedRank;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, requestedRank);
    }

    @Override
    public String toString() {
        if (requestedRank != -1) {
            String rankName = switch (requestedRank) {
                case 1 -> "Aces";
                case 11 -> "Jacks";
                case 12 -> "Queens";
                case 13 -> "Kings";
                default -> requestedRank + "s";
            };
            return "P" + playerID + " goes fish (asked for " + rankName + ")";
        }
        return "P" + playerID + " draws from deck";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
