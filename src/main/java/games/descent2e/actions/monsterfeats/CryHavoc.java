package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyInt;
import core.properties.PropertyVector2D;
import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Move;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import utilities.Pair;
import utilities.Utils;
import utilities.Vector2D;

import java.util.*;

import static core.CoreConstants.coordinateHash;
import static core.CoreConstants.playersHash;
import static games.descent2e.DescentHelper.*;
import static games.descent2e.actions.archetypeskills.PrayerOfPeace.canAttackPrayer;

public class CryHavoc extends DescentAction implements IExtendedSequence {

    protected int attackingFigure;
    protected int range;
    protected int oldMovePoints = 0;
    protected boolean attacked = false;
    protected boolean complete = false;
    protected List<Integer> targets = new ArrayList<>();
    public CryHavoc(int attackingFigure, int range) {
        super(Triggers.ACTION_POINT_SPEND);
        this.attackingFigure = attackingFigure;
        this.range = range;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure belthir = (Figure) gameState.getComponentById(attackingFigure);
        String name;
        if (belthir == null) {
            // If Belthir is dead, we cannot get his name
            name = "Belthir";
        }
        else name = belthir.getName().replace("Hero ", "");
        return "Cry Havoc: " + name + " flies overhead and makes a Multi Attack against all figures in the path";
    }

    @Override
    public String toString() {
        return "Cry Havoc: " + attackingFigure + " Move and Multi Attack";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Figure belthir = (Figure) gs.getComponentById(attackingFigure);
        belthir.getNActionsExecuted().increment();
        belthir.addActionTaken(toString());
        gs.setActionInProgress(this);
        belthir.setOffMap(true); // Belthir is off the map until he finishes his movement
        oldMovePoints = belthir.getAttribute(Figure.Attribute.MovePoints).getValue();
        belthir.setAttributeToMax(Figure.Attribute.MovePoints);

        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {

        DescentGameState dgs = (DescentGameState) state;
        Figure belthir = (Figure) state.getComponentById(attackingFigure);

        List<AbstractAction> retVal = new ArrayList<>();

        if (attacked) {
            // After attack, land
            Land land = new Land();
            if (land.canExecute(dgs)) {
                retVal.add(land);
            }
            return retVal;
        }

        // If Belthir has Move Points, we need to move him first
        // We don't use the usual DescentHelper.moveActions() here
        // because we want to have multiple movements with the same destination but different paths
        // so that he can maximise the number of targets he flies over
        List<AbstractAction> movement = moveActions(dgs, belthir);
        retVal.addAll(movement);

        // If he has flown over at least one target, we can land to trigger the attack
        if (!targets.isEmpty())
        {
            // Enable the Multi Attack if we have targets

            CryHavocAttack attack = new CryHavocAttack(attackingFigure, targets, range);
            if (attack.canExecute(dgs)) {
                retVal.add(attack);
            }
        }

        return retVal;
    }

    protected List<AbstractAction> moveActions(DescentGameState dgs, Figure figure)
    {
        Vector2D figureLocation = figure.getPosition();
        BoardNode figureNode = dgs.getMasterBoard().getElement(figureLocation.getX(), figureLocation.getY());

        //<Board Node, Cost to get there>
        HashMap<BoardNode, Pair<Double,List<Vector2D>>> allAdjacentNodes = new HashMap<>();
        List<List<Vector2D>> paths = new ArrayList<>();
        List<List<Integer>> targets = new ArrayList<>();
        double chainCost = 0.0;

        getMoves(figure, figureNode, new ArrayList<>(), new ArrayList<>(), chainCost, paths, targets, 0);

        List<Move> actions = new ArrayList<>();

        for (List<Vector2D> move : paths)
        {
            Move myMoveAction = new Move(figure.getComponentID(), move, true);
            myMoveAction.updateDirectionID(dgs);
            if(myMoveAction.canExecute(dgs))
                actions.add(myMoveAction);
        }

        // Sorts the movement actions to always be in the same order (Clockwise NW to W, One Space then Multiple Spaces)
        actions.sort(Comparator.comparingInt(Move::getDirectionID));

        return new ArrayList<>(actions);
    }

    private void getMoves(Figure figure, BoardNode node, List<Vector2D> path, List<Integer> target, double currentCost, List<List<Vector2D>> paths, List<List<Integer>> targets, int distance) {
        int movement = figure.getAttributeValue(Figure.Attribute.MovePoints);
        if (distance > movement) return;

        int figureID = figure.getComponentID();

        for (BoardNode neighbour : node.getNeighbours().keySet()) {
            int neighbourID = ((PropertyInt) neighbour.getProperty(playersHash)).value;
            Vector2D pos = ((PropertyVector2D) neighbour.getProperty(coordinateHash)).values;
            double cost = node.getNeighbourCost(neighbour);
            double totalCost = currentCost + cost;

            if (totalCost > movement) continue;

            List<Vector2D> newPath = new ArrayList<>(path);
            List<Integer> newTarget = new ArrayList<>(target);
            newPath.add(pos);
            if (!newTarget.contains(neighbourID)) {
                newTarget.add(neighbourID);
            }
            newTarget.sort(Comparator.comparingInt(Integer::intValue));

            boolean skip = false;

            // Only add the path if it ends on an empty tile or Belthir himself
            if (neighbourID == -1 || neighbourID == figureID) {
                List<Integer> prune = new ArrayList<>();
                for (int i = 0; i < targets.size(); i++) {
                    if (skip) break;

                    // Check if the new target list is already in the list of targets
                    if (newTarget.equals(targets.get(i)))
                    {
                        List<Vector2D> oldPath = paths.get(i);
                        if (oldPath.get(oldPath.size() - 1).equals(newPath.get(newPath.size() - 1))) {
                            // If the last position is the same, and the old path is shorter or equal, do not add this path
                            if (oldPath.size() <= newPath.size()) skip = true;
                            else prune.add(i); // Otherwise, prune the old path
                        }
                    }
                }
                if (skip) continue;
                if (!prune.isEmpty())
                {
                    // Remove the old paths that are longer than the new one
                    // and have the same targets and destination
                    for (int i = prune.size() - 1; i >= 0; i--) {
                        int index = prune.get(i);
                        paths.remove(index);
                        targets.remove(index);
                    }
                }

                paths.add(newPath);
                targets.add(newTarget);
            }

            // Only continue deeper if the tile contains an enemy
            else {
                getMoves(figure, neighbour, newPath, newTarget, totalCost, paths, targets, distance + 1);
            }
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        Figure belthir = (Figure) state.getComponentById(attackingFigure);
        if (belthir == null) return ((DescentGameState) state).getOverlordPlayer();     // Only if Belthir got himself killed. Idiot.
        return belthir.getOwnerId();
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (f.hasAttacked()) return false; // Monsters can only attack once per turn
        if (f.hasCondition(DescentTypes.DescentCondition.Immobilize)) return false; // Immobilized figures cannot move
        if (f.getAttributeMax(Figure.Attribute.MovePoints) < 2) return false; // If he can't even fly over someone, don't bother

        if (!(f instanceof Monster) || (!((Monster) f).hasAction(MonsterAbilities.MonsterAbility.CRYHAVOC))) return false;

        List<Hero> heroes = dgs.getHeroes();
        if (heroes.isEmpty()) return false; // No valid Heroes, somehow?
        for (Hero hero : heroes) {
            if (hero.isOffMap()) continue; // Skip Heroes that are off the map
            Vector2D heroPosition = hero.getPosition();

            int distance = getRangeAllSpaces(dgs, f, hero);
            if (distance > range) continue; // Skip Heroes that are too far away

            // Check if Belthir can actually move to a Hero's position and then to an empty space
            Set<BoardNode> neighbours = getNeighboursInRange(dgs, heroPosition, range - distance + 1);
            for (BoardNode node : neighbours)
            {
                int id = ((PropertyInt) node.getProperty(playersHash)).value;
                // We want a valid space to land on, so it must be empty or occupied already by Belthir
                if (id == -1 || id == attackingFigure)
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
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {

        if (action instanceof Land) {
            complete = true;
        }

        if (action instanceof Move)
        {
            DescentGameState dgs = (DescentGameState) state;
            List<Vector2D> positions = ((Move) action).getPositionsTraveled();
            for (Vector2D pos : positions)
            {
                BoardNode node = dgs.getMasterBoard().getElement(pos);
                int id = ((PropertyInt) node.getProperty(playersHash)).value;
                if (id != -1 && id != attackingFigure) {

                    // If we have not already added this target, add it now
                    if (!targets.contains(id)) {
                        // Defeated Heroes should not be targeted
                        Figure f = (Figure) state.getComponentById(id);
                        if (!f.isOffMap())
                            targets.add(id);
                    }
                }

            }
        }

        Figure belthir = (Figure) state.getComponentById(attackingFigure);

        if (belthir.getAttribute(Figure.Attribute.Health).isMinimum()) {
            // Somehow, Belthir ended his movement in a pit or lava and defeated himself prematurely.
            // Idiot.
            complete = true;
            attacked = false;
            return;
        }

        if (!complete)
        {
            // If Belthir wants to stop moving to attack, do so now
            if (action instanceof CryHavocAttack)
            {
                // Start the attack
                belthir.setAttributeToMin(Figure.Attribute.MovePoints);
                attacked = true;
            }

            // Check if Belthir has, somehow, run out of Move Points without flying over any targets.
            // Or worse, ended his movement next to a Disciple with Prayer of Peace and blocked his attack.
            // Idiot.
            if (belthir.getAttribute(Figure.Attribute.MovePoints).isMinimum()) {
                if (targets.isEmpty()) attacked = true;
                if (!canAttackPrayer((DescentGameState) state, belthir)) attacked = true;
            }
        }
        if (complete) {
            attacked = false;
            belthir.setOffMap(false);
            belthir.setAttribute(Figure.Attribute.MovePoints, oldMovePoints); // Restore his original Move Points
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public CryHavoc copy() {
        CryHavoc retVal = new CryHavoc(attackingFigure, range);
        copyComponentsTo(retVal);
        return retVal;
    }

    public void copyComponentsTo(CryHavoc retVal)
    {
        retVal.targets = new ArrayList<>();
        retVal.targets.addAll(targets);
        retVal.oldMovePoints = oldMovePoints;
        retVal.attacked = attacked;
        retVal.complete = complete;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CryHavoc cryHavoc) {
            return super.equals(cryHavoc) &&
                    this.attackingFigure == cryHavoc.attackingFigure &&
                    this.range == cryHavoc.range &&
                    this.oldMovePoints == cryHavoc.oldMovePoints &&
                    this.attacked == cryHavoc.attacked &&
                    this.complete == cryHavoc.complete &&
                    this.targets.equals(cryHavoc.targets);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attackingFigure, range, oldMovePoints, attacked, complete, targets);
    }
}
