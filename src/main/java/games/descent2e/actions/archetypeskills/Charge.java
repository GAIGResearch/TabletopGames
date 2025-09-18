package games.descent2e.actions.archetypeskills;

import com.google.common.collect.Iterables;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.components.GridBoard;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyInt;
import core.properties.PropertyVector2D;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Move;
import games.descent2e.actions.StopMove;
import games.descent2e.actions.Triggers;
import games.descent2e.actions.attack.EndCurrentPhase;
import games.descent2e.actions.attack.FreeAttack;
import games.descent2e.actions.attack.MeleeAttack;
import games.descent2e.actions.attack.RangedAttack;
import games.descent2e.actions.monsterfeats.CryHavoc;
import games.descent2e.actions.monsterfeats.CryHavocAttack;
import games.descent2e.actions.monsterfeats.Land;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import utilities.Pair;
import utilities.Utils;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static core.CoreConstants.playersHash;
import static games.descent2e.DescentHelper.*;
import static games.descent2e.actions.archetypeskills.PrayerOfPeace.canAttackPrayer;

public class Charge extends CryHavoc {

    boolean moved = false;

    public Charge(int attackingFigure, int range) {
        super(attackingFigure, range);
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Figure f = (Figure) gs.getComponentById(attackingFigure);
        f.getNActionsExecuted().increment();
        f.addActionTaken(toString());
        f.incrementAttribute(Figure.Attribute.Fatigue, 2);
        gs.setActionInProgress(this);
        oldMovePoints = f.getAttribute(Figure.Attribute.MovePoints).getValue();
        f.setAttributeToMax(Figure.Attribute.MovePoints);

        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        DescentGameState dgs = (DescentGameState) state;
        Figure f = (Figure) state.getComponentById(attackingFigure);

        List<AbstractAction> retVal = new ArrayList<>();

        boolean reach = checkReach(dgs, f);

        if (!f.getAttribute(Figure.Attribute.MovePoints).isMinimum()) {
            List<AbstractAction> movement = DescentHelper.moveActions(dgs, f);
            for (AbstractAction move : movement) {
                List<Vector2D> positions = ((Move) move).getPositionsTraveled();
                for (int target : targets) {
                    if (retVal.contains(move)) break;
                    Figure monster = (Figure) dgs.getComponentById(target);
                    int distance = f.getAttributeValue(Figure.Attribute.MovePoints) + 1
                            - positions.size()
                            + (reach ? 1 : 0);

                    BoardNode destination = dgs.getMasterBoard().getElement(Iterables.getLast(positions));

                    List<BoardNode> targetTiles = new ArrayList<>();
                    BoardNode anchorTile = dgs.getMasterBoard().getElement(monster.getPosition());
                    Pair<Integer, Integer> size = monster.getSize();
                    if (size.a > 1 || size.b > 1) {
                        targetTiles.addAll(getAttackingTiles(target, anchorTile, targetTiles));
                    } else {
                        targetTiles.add(anchorTile);
                    }

                    for (BoardNode tile : targetTiles)
                    {
                        Set<BoardNode> possible = getNeighboursInRange(dgs, monster.getPosition(), distance);
                        if (possible.contains(destination)) {
                            if (hasLineOfSight(dgs, ((PropertyVector2D) destination.getProperty("coordinates")).values, ((PropertyVector2D) tile.getProperty("coordinates")).values)) {
                                retVal.add(move);
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (moved) {
            List<Integer> targets = getMeleeTargets(dgs, f, reach);
            if (!targets.isEmpty()) {
                for (int target : targets) {
                    ChargeAttack attack = new ChargeAttack(attackingFigure, target, reach);
                    if (attack.canExecute(dgs)) {
                        retVal.add(attack);
                    }
                }
            }
        }

        return retVal;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {

        if (action instanceof MeleeAttack)
            complete = true;

        if (action instanceof Move)
            moved = true;

        Figure f = (Figure) state.getComponentById(attackingFigure);

        if (f.getAttribute(Figure.Attribute.Health).isMinimum()) {
            // Somehow, the Berserker ended their movement in a pit or lava and defeated themselves prematurely.
            // Idiot.
            complete = true;
            return;
        }

        if (complete) {
            f.setAttribute(Figure.Attribute.MovePoints, oldMovePoints); // Restore original Move Points
        }
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (f.getNActionsExecuted().isMaximum()) return false;
        if (f.getAttributeValue(Figure.Attribute.Fatigue) + 2 > f.getAttribute(Figure.Attribute.Fatigue).getMaximum())
            return false; // Not enough Fatigue to perform Charge
        if (f.hasCondition(DescentTypes.DescentCondition.Immobilize)) return false; // Immobilized figures cannot move

        // Only Heroes can use Charge
        if (!(f instanceof Hero)) return false;

        List<List<Monster>> monsters = dgs.getMonsters();
        if (monsters.isEmpty()) return false; // No valid Monsters, somehow?

        for (List<Monster> monster : monsters)
        {
            for (Monster m : monster)
            {
                if (checkValid(dgs, f, m))
                    return true;
            }
        }
        return false;
    }

    public void setTargets(DescentGameState dgs, Figure f)
    {
        List<List<Monster>> monsters = dgs.getMonsters();
        for (List<Monster> monster : monsters)
        {
            for (Monster m : monster)
            {
                if (checkValid(dgs, f, m))
                    targets.add(m.getComponentID());
            }
        }
    }

    public boolean checkValid(DescentGameState dgs, Figure f, Monster m) {
        int distance = getRangeAllSpaces(dgs, m, f);
        if (distance > range) return false; // Skip Monsters that are too far away

        Pair<Integer, Integer> size = m.getSize();
        List<BoardNode> targetTiles = new ArrayList<>();

        Vector2D currentLocation = m.getPosition();
        GridBoard board = dgs.getMasterBoard();
        BoardNode anchorTile = board.getElement(currentLocation);

        if (size.a > 1 || size.b > 1) {
            targetTiles.addAll(getAttackingTiles(m.getComponentID(), anchorTile, targetTiles));
        } else {
            targetTiles.add(anchorTile);
        }

        for (BoardNode tile : targetTiles)
        {
            Set<BoardNode> neighbours = tile.getNeighbours().keySet();
            for (BoardNode node : neighbours)
            {
                int id = ((PropertyInt) node.getProperty(playersHash)).value;
                // We want a valid space to stop on, so it must be empty
                // If we already occupy the space, then we are moving to nowhere
                // In which case - why are you wasting Fatigue on Charge? Just make a regular attack
                if (id == -1)
                {
                    DescentTypes.TerrainType terrain = Utils.searchEnum(DescentTypes.TerrainType.class, node.getComponentName());
                    if (terrain != null) {
                        // We really should not be ending this action in hazardous terrain, if we can avoid it
                        if (terrain == DescentTypes.TerrainType.Pit ||
                                terrain == DescentTypes.TerrainType.Lava ||
                                terrain == DescentTypes.TerrainType.Hazard ||
                                terrain == DescentTypes.TerrainType.Block) {
                            continue;
                        }
                        if (inRange(f.getPosition(), ((PropertyVector2D) node.getProperty("coordinates")).values, range))
                            if (hasLineOfSight(dgs, ((PropertyVector2D) node.getProperty("coordinates")).values, ((PropertyVector2D) tile.getProperty("coordinates")).values))
                                return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean hasMoved() {
        return moved;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(attackingFigure);
        String testingName = f.getName().replace("Hero: ", "");

        return "Charge: " + testingName + " Moves, then makes a Melee Attack";
    }

    @Override
    public String toString() {
        return "Charge: " + attackingFigure + " Move and Melee Attack";
    }

    @Override
    public Charge copy(){
        Charge retValue = new Charge(attackingFigure, range);
        copyComponentsTo(retValue);
        return retValue;
    }

    public void copyComponentsTo(Charge target){
        super.copyComponentsTo(target);
        target.moved = moved;
    }
}