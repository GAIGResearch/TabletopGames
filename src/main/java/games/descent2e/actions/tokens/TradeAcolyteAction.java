package games.descent2e.actions.tokens;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.BoardNode;
import core.components.GridBoard;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyInt;
import games.descent2e.DescentGameState;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.tokens.DToken;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static core.CoreConstants.playersHash;
import static utilities.Utils.getNeighbourhood;

/**
 * 2-step action to give acolyte token to an adjacent player. Player currently carrying the token chooses between adjacent players
 */
public class TradeAcolyteAction extends TokenAction implements IExtendedSequence {
    int receivingHeroIdx;

    public TradeAcolyteAction() {
        super(-1, Triggers.ACTION_POINT_SPEND);
        receivingHeroIdx = -1;
    }
    public TradeAcolyteAction(int acolyteComponentID) {
        super(acolyteComponentID, Triggers.ACTION_POINT_SPEND);
        this.receivingHeroIdx = -1;
    }
    public TradeAcolyteAction(int acolyteComponentID, int receivingHeroIdx) {
        super(acolyteComponentID, Triggers.ACTION_POINT_SPEND);
        this.receivingHeroIdx = receivingHeroIdx;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        // Trade with adjacent players
        List<AbstractAction> actions = new ArrayList<>();
        DescentGameState dgs = (DescentGameState) state;
        DToken acolyte = (DToken) dgs.getComponentById(tokenID);
        int heroIdx = acolyte.getOwnerId();
        Hero hero = dgs.getHeroes().get(heroIdx);
        if (hero.getOwnerId() == ((DescentGameState) state).getActingFigure().getOwnerId()) {
            HashSet<Hero> adjacentHeroes = new HashSet<>();
            Vector2D loc = hero.getPosition();
            GridBoard board = dgs.getMasterBoard();
            List<Vector2D> neighbours = getNeighbourhood(loc.getX(), loc.getY(), board.getWidth(), board.getHeight(), true);
            for (Vector2D n : neighbours) {
                BoardNode bn = board.getElement(n.getX(), n.getY());
                if (bn != null) {
                    PropertyInt figureAtNode = ((PropertyInt) bn.getProperty(playersHash));
                    if (figureAtNode != null && figureAtNode.value != -1) {
                        Figure f = (Figure) dgs.getComponentById(figureAtNode.value);
                        if (f instanceof Hero) {
                            adjacentHeroes.add((Hero) f);
                        }
                    }
                }
            }
            if (adjacentHeroes.size() > 0) {
                for (int i = 0; i < dgs.getHeroes().size(); i++) {
                    if (i != heroIdx && adjacentHeroes.contains(dgs.getHeroes().get(i))) {
                        // Check if adjacent
                        actions.add(new TradeAcolyteAction(tokenID, i));
                    }
                }
            }
        }
        if (actions.size() == 0) actions.add(new DoNothing());
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return state.getComponentById(tokenID).getOwnerId();
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        if (action instanceof TradeAcolyteAction) {
            this.receivingHeroIdx = ((TradeAcolyteAction) action).receivingHeroIdx;
        } else this.receivingHeroIdx = state.getComponentById(tokenID).getOwnerId();  // did nothing, ownership not changing
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return receivingHeroIdx != -1;
    }

    @Override
    public TradeAcolyteAction copy() {
        return new TradeAcolyteAction(tokenID, receivingHeroIdx);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        // Can only execute if player adjacent to another hero
        DToken acolyte = (DToken) dgs.getComponentById(tokenID);
        int heroIdx = acolyte.getOwnerId();
        Hero hero = dgs.getHeroes().get(heroIdx);
        if (hero.getOwnerId() == dgs.getActingFigure().getOwnerId()) {
            Vector2D loc = hero.getPosition();
            GridBoard board = dgs.getMasterBoard();
            List<Vector2D> neighbours = getNeighbourhood(loc.getX(), loc.getY(), board.getWidth(), board.getHeight(), true);
            for (Vector2D n : neighbours) {
                BoardNode bn = board.getElement(n.getX(), n.getY());
                if (bn != null) {
                    PropertyInt figureAtNode = ((PropertyInt) bn.getProperty(playersHash));
                    if (figureAtNode != null && figureAtNode.value != -1) {
                        Figure f = (Figure) dgs.getComponentById(figureAtNode.value);
                        if (f instanceof Hero) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TradeAcolyteAction)) return false;
        TradeAcolyteAction that = (TradeAcolyteAction) o;
        return receivingHeroIdx == that.receivingHeroIdx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), receivingHeroIdx);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Give acolyte to " + receivingHeroIdx;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        if (receivingHeroIdx != -1) {
            DToken acolyte = (DToken) gs.getComponentById(tokenID);
            if (receivingHeroIdx != acolyte.getOwnerId()) {
                acolyte.setOwnerId(receivingHeroIdx, gs);
            }
            gs.getActingFigure().getNActionsExecuted().increment();
        } else {
            gs.setActionInProgress(this);
        }
        return true;
    }
}
