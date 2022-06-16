package games.descent2e.concepts;

import games.descent2e.DescentGameState;
import org.json.simple.JSONObject;
import utilities.Utils;

public abstract class GameOverCondition {
    // Tests if game is over, sets the correct result, and returns game result
    public abstract Utils.GameResult test(DescentGameState gs);
    public abstract void parse(JSONObject jsonObject);
    public abstract String toString();  // Print condition info
    public abstract String getString(DescentGameState gs);  // Print condition info + progress so far
}
