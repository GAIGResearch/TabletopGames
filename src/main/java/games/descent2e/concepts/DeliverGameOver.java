package games.descent2e.concepts;

import core.CoreConstants;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import games.descent2e.components.tokens.DToken;
import org.json.simple.JSONObject;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DeliverGameOver extends GameOverCondition {
    // IMPORTANT: All values are effectively final and should not be changed after parsing initialisation
    String tokenType;
    String figureName;
    String destination;
    CoreConstants.GameResult resultOverlord, resultHeroes;

    @Override
    public CoreConstants.GameResult test(DescentGameState gs) {

        for (DToken token : gs.getTokens()) {
            if (!Objects.equals(token.getComponentName(), tokenType)) continue;
            if (token.getOwnerId() != -1) {
                Figure f = (Figure) gs.getComponentById(token.getOwnerId());
                if (figureName.toLowerCase().contains("hero")) {
                    if (f instanceof Hero) {
                        List<Vector2D> positions = new ArrayList<>(gs.getGridReferences().get(destination).keySet());
                        if (positions.contains(f.getPosition()))
                            return endGame(gs);
                    }
                } else if (f.getName().toLowerCase().contains(figureName.toLowerCase())) {
                    if (f instanceof Monster) {
                        List<Vector2D> positions = new ArrayList<>(gs.getGridReferences().get(destination).keySet());
                        if (positions.contains(f.getPosition()))
                            return endGame(gs);
                    }
                }
            }
        }
        return CoreConstants.GameResult.GAME_ONGOING;
    }

    @Override
    public void parse(JSONObject jsonObject) {
        tokenType = jsonObject.get("token").toString();
        figureName = jsonObject.get("figure").toString();
        destination = jsonObject.get("destination").toString();
        resultHeroes = CoreConstants.GameResult.valueOf((String) jsonObject.get("result-heroes"));
        resultOverlord = CoreConstants.GameResult.valueOf((String) jsonObject.get("result-overlord"));
    }

    @Override
    public String toString() {
        return figureName + " + deliver " + tokenType + " to " + destination +
                "? " + "Heroes: " + resultHeroes + "; Overlord: " + resultOverlord;
    }

    @Override
    public String getString(DescentGameState gs) {
        return figureName + " + deliver " + tokenType + " to " + destination +
                "? " + "Heroes: " + resultHeroes + "; Overlord: " + resultOverlord;
    }

    private CoreConstants.GameResult endGame(DescentGameState gs) {
        gs.setGameStatus(CoreConstants.GameResult.GAME_END);
        for (int i = 0; i < gs.getNPlayers(); i++) {
            if (gs.getOverlordPlayer() == i) gs.setPlayerResult(resultOverlord, i);
            else gs.setPlayerResult(resultHeroes, i);
        }
        return CoreConstants.GameResult.GAME_END;
    }

    @Override
    public DeliverGameOver copy() {
        DeliverGameOver cgo = new DeliverGameOver();
        cgo.tokenType = tokenType;
        cgo.figureName = figureName;
        cgo.destination = destination;
        cgo.resultHeroes = resultHeroes;
        cgo.resultOverlord = resultOverlord;
        return cgo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliverGameOver that = (DeliverGameOver) o;
        return tokenType == that.tokenType && Objects.equals(figureName, that.figureName) && Objects.equals(destination, that.destination) && resultOverlord == that.resultOverlord && resultHeroes == that.resultHeroes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenType, figureName, destination, resultOverlord, resultHeroes);
    }
}
