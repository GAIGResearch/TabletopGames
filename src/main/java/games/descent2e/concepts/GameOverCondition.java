package games.descent2e.concepts;

import core.CoreConstants;
import games.descent2e.DescentGameState;
import org.json.simple.JSONObject;
import utilities.Utils;

import java.nio.charset.CoderResult;

public abstract class GameOverCondition {
    // Tests if game is over, sets the correct result, and returns game result
    public abstract CoreConstants.GameResult test(DescentGameState gs);
    public abstract void parse(JSONObject jsonObject);
    public abstract String toString();  // Print condition info
    public abstract String getString(DescentGameState gs);  // Print condition info + progress so far
    public abstract GameOverCondition copy();
    public abstract boolean equals(Object o);
    public abstract int hashCode();
}
