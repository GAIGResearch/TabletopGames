package games.descent2e.actions.tokens;

import core.AbstractGameState;
import core.components.GridBoard;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Hero;
import games.descent2e.components.tokens.DToken;
import utilities.Vector2D;

import java.util.List;

import static games.descent2e.actions.Triggers.END_TURN;
import static utilities.Utils.getNeighbourhood;

/**
 * If the hero carrying the acolyte ends turn adjacent to 1 or more wounded clergy (villager tokens),
 * choose 1 adjacent wounded clergy to escort
 * TODO: effect does not cost action points and is automatically triggered at the end of the hero's turn
 * TODO: player choice which one to get if multiple options (currently first token found)
 */
public class AcolyteAction extends TokenAction {

    public AcolyteAction() {
        super(-1, END_TURN);
    }

    @Override
    public AcolyteAction copy() {
        AcolyteAction sa = new AcolyteAction();
        sa.tokenID = tokenID;
        return sa;
    }

    @Override
    public boolean canExecute(DescentGameState gs) {
        // Can only execute if player adjacent to villager token
        DToken acolyte = (DToken) gs.getComponentById(tokenID);
        Hero hero = gs.getHeroes().get(acolyte.getOwnerId());
        Vector2D loc = hero.getPosition();
        GridBoard board = gs.getMasterBoard();
        List<Vector2D> neighbours = getNeighbourhood(loc.getX(), loc.getY(), board.getWidth(), board.getHeight(), true);
        for (DToken token: gs.getTokens()) {
            if (token.getDescentTokenType() == DescentTypes.DescentToken.Villager && neighbours.contains(token.getPosition())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof AcolyteAction;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Acolyte end turn effect";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        DToken acolyte = (DToken) gs.getComponentById(tokenID);
        int heroIdx = acolyte.getOwnerId();
        Hero hero = gs.getHeroes().get(heroIdx);
        Vector2D loc = hero.getPosition();
        GridBoard board = gs.getMasterBoard();
        List<Vector2D> neighbours = getNeighbourhood(loc.getX(), loc.getY(), board.getWidth(), board.getHeight(), true);
        for (DToken token: gs.getTokens()) {
            if (token.getDescentTokenType() == DescentTypes.DescentToken.Villager && neighbours.contains(token.getPosition())) {
                token.setOwnerId(heroIdx, gs); // Take this one
                token.setPosition(null);  // Take off the map
                return true;
            }
        }
        return false;
    }
}
