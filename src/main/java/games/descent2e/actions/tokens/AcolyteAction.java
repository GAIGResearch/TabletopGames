package games.descent2e.actions.tokens;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.GridBoard;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Hero;
import games.descent2e.components.tokens.DToken;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static games.descent2e.actions.Triggers.END_TURN;
import static utilities.Utils.getNeighbourhood;

/**
 * If the hero carrying the acolyte ends turn adjacent to 1 or more wounded clergy (villager tokens),
 * choose 1 adjacent wounded clergy to escort
 * TODO: effect does not cost action points and is automatically triggered at the end of the hero's turn
 * TODO: player choice which one to get if multiple options (currently first token found)
 */
public class AcolyteAction extends TokenAction<AcolyteAction> implements IExtendedSequence {

    int villagerID;
    boolean complete;

    public AcolyteAction() {
        super(-1, END_TURN);
    }

    public AcolyteAction(int acolyteComponentID) {
        super(acolyteComponentID, END_TURN);
        this.villagerID = 0;
    }

    public AcolyteAction(int acolyteComponentID, int villagerID) {
        super(acolyteComponentID, END_TURN);
        this.villagerID = villagerID;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> actions = new ArrayList<>();
        DescentGameState dgs = (DescentGameState) state;
        DToken acolyte = (DToken) dgs.getComponentById(tokenID);
        int heroIdx = acolyte.getOwnerId();
        if (heroIdx == dgs.getActingFigure().getComponentID()) {
            HashSet<DToken> adjacentVillagers = new HashSet<>();
            Vector2D loc = ((Hero) dgs.getComponentById(heroIdx)).getPosition();
            GridBoard board = dgs.getMasterBoard();
            List<Vector2D> neighbours = getNeighbourhood(loc.getX(), loc.getY(), board.getWidth(), board.getHeight(), true);
            neighbours.add(loc);
            for (DToken token : dgs.getTokens()) {
                if (token.getDescentTokenType() == DescentTypes.DescentToken.Villager && neighbours.contains(token.getPosition())) {
                    adjacentVillagers.add(token);
                }
            }
            if (!adjacentVillagers.isEmpty()) {
                for (DToken token : adjacentVillagers) {
                    AcolyteAction newAA = new AcolyteAction(tokenID, token.getComponentID());
                    if (newAA.canExecute(dgs)) actions.add(newAA);
                }
                AcolyteAction newAA = new AcolyteAction(tokenID, -1);
                if (newAA.canExecute(dgs)) actions.add(newAA);
            }
        }
        if (actions.isEmpty()) actions.add(new DoNothing());
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        int figureIdx = state.getComponentById(tokenID).getOwnerId();
        return state.getComponentById(figureIdx).getOwnerId();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        complete = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public AcolyteAction _copy() {
        AcolyteAction acolyteAction = new AcolyteAction(tokenID);
        acolyteAction.villagerID = villagerID;
        acolyteAction.complete = complete;
        return acolyteAction;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        // Can only execute if player adjacent to villager token
        DToken acolyte = (DToken) dgs.getComponentById(tokenID);
        int heroIdx = acolyte.getOwnerId();
        if (heroIdx == dgs.getActingFigure().getComponentID()) {
            if (villagerID == -1) return true; // Can always skip
            Hero hero = (Hero) dgs.getComponentById(heroIdx);
            String lastAction = hero.getActionsTaken().get(hero.getActionsTaken().size() - 1);
            if (lastAction.contains("Escort Wounded Clergy at")) return false;  // Already escorted one this turn
            if (lastAction.contains("Skipped Escorting Wounded Clergy")) return false;  // Already skipped this turn
            Vector2D loc = hero.getPosition();
            GridBoard board = dgs.getMasterBoard();
            List<Vector2D> neighbours = getNeighbourhood(loc.getX(), loc.getY(), board.getWidth(), board.getHeight(), true);
            neighbours.add(loc);
            if (villagerID != 0) {
                DToken token = (DToken) dgs.getComponentById(villagerID);
                return token.getDescentTokenType() == DescentTypes.DescentToken.Villager && neighbours.contains(token.getPosition());
            }
            else {
                for (DToken token: dgs.getTokens()) {
                    if (token.getDescentTokenType() == DescentTypes.DescentToken.Villager && neighbours.contains(token.getPosition())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof AcolyteAction && ((AcolyteAction) o).villagerID == villagerID && complete == ((AcolyteAction) o).complete;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), villagerID, complete);
    }


    @Override
    public String getString(AbstractGameState gameState) {
        if (villagerID == 0) return "Acolyte End Turn Effect";
        if (villagerID == -1) return "Skip Escorting Wounded Clergy";
        return "Escort Wounded Clergy at " + ((DToken) gameState.getComponentById(villagerID)).getPosition();
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        if (villagerID != 0) {
            DToken acolyte = (DToken) dgs.getComponentById(tokenID);
            int heroIdx = acolyte.getOwnerId();
            Hero hero = (Hero) dgs.getComponentById(heroIdx);
            if (villagerID == -1) {
                hero.addActionTaken("Skipped Escorting Wounded Clergy");
                return true;
            }
            Vector2D loc = hero.getPosition();
            GridBoard board = dgs.getMasterBoard();
            List<Vector2D> neighbours = getNeighbourhood(loc.getX(), loc.getY(), board.getWidth(), board.getHeight(), true);
            neighbours.add(loc);
            DToken token = (DToken) dgs.getComponentById(villagerID);
            if (token.getDescentTokenType() == DescentTypes.DescentToken.Villager && neighbours.contains(token.getPosition())) {
                token.setOwnerId(heroIdx, dgs); // Take this one
                token.setPosition(null);  // Take off the map
                hero.addActionTaken("Escort Wounded Clergy at " + token.getPosition());
            }
        }
        else {
            dgs.setActionInProgress(this);
        }
        return true;
    }
}
