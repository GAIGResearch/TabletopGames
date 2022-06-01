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
import java.util.List;
import java.util.Objects;

import static core.CoreConstants.playersHash;
import static utilities.Utils.getNeighbourhood;

/**
 * 2-step action to give acolyte token to an adjacent player. Player currently carrying the token chooses between adjacent players
 */
public class TradeAcolyteAction extends TokenAction implements IExtendedSequence {
    int receivingHeroComponentID;

    public TradeAcolyteAction() {
        super(-1, Triggers.ACTION_POINT_SPEND);
        receivingHeroComponentID = -1;
    }
    public TradeAcolyteAction(int acolyteComponentID) {
        super(acolyteComponentID, Triggers.ACTION_POINT_SPEND);
        this.receivingHeroComponentID = -1;
    }
    public TradeAcolyteAction(int acolyteComponentID, int receivingHeroComponentID) {
        super(acolyteComponentID, Triggers.ACTION_POINT_SPEND);
        this.receivingHeroComponentID = receivingHeroComponentID;
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
                            actions.add(new TradeAcolyteAction(tokenID, figureAtNode.value));
                        }
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
            this.receivingHeroComponentID = ((TradeAcolyteAction) action).receivingHeroComponentID;
        } else this.receivingHeroComponentID = state.getComponentById(tokenID).getOwnerId();  // did nothing, ownership not changing
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return receivingHeroComponentID != -1;
    }

    @Override
    public TradeAcolyteAction copy() {
        return new TradeAcolyteAction(tokenID, receivingHeroComponentID);
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
        return receivingHeroComponentID == that.receivingHeroComponentID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), receivingHeroComponentID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Give acolyte to " + receivingHeroComponentID;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        if (receivingHeroComponentID != -1) {
            DToken acolyte = (DToken) gs.getComponentById(tokenID);
            if (receivingHeroComponentID != acolyte.getOwnerId()) {
                acolyte.setOwnerId(receivingHeroComponentID, gs);
            }
        } else {
            gs.setActionInProgress(this);
        }
        return true;
    }
}
