package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.interfaces.IExtendedSequence;
import core.properties.PropertyInt;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Move;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import utilities.Utils;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static core.CoreConstants.playersHash;
import static games.descent2e.DescentHelper.*;

public class CryHavoc extends DescentAction implements IExtendedSequence {

    int attackingFigure;
    int range;
    int oldMovePoints = 0;
    boolean attacking = false;
    boolean complete = false;
    List<Integer> targets = new ArrayList<>();
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

        if (attacking) {
            // Enable the Multi Attack if he has landed

            CryHavocAttack attack = new CryHavocAttack(attackingFigure, targets, range);
            if (attack.canExecute(dgs)) {
                retVal.add(attack);
            }
            return retVal;
        }

        // If Belthir has Move Points, we need to move him first
        List<AbstractAction> movement = moveActions(dgs, belthir);
        retVal.addAll(movement);

        // If he has flown over at least one target, we can land to trigger the attack
        if (!targets.isEmpty())
        {
            Land land = new Land();
            if (land.canExecute(dgs)) {
                retVal.add(land);
            }
        }

        return retVal;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        Figure belthir = (Figure) state.getComponentById(attackingFigure);
        if (belthir == null) return ((DescentGameState) state).getOverlordPlayer();     // Only if Belthir got himself killed.
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

        if (action instanceof CryHavocAttack) {
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
            // Somehow, Belthir ended his movement in a pit or lava and defeated himself.
            // Idiot.
            complete = true;
            attacking = false;
            return;
        }

        if (!complete)
        {
            // If Belthir wants to stop moving to attack, do so now
            if (action instanceof Land)
            {
                // Start the attack
                belthir.setAttributeToMin(Figure.Attribute.MovePoints);
                attacking = true;
            }

            // Check if Belthir has, somehow, run out of Move Points without flying over any targets.
            // Idiot.
            if (belthir.getAttribute(Figure.Attribute.MovePoints).isMinimum()) {
                if (targets.isEmpty()) complete = true;
            }
        }
        if (complete) {
            attacking = false;
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
        retVal.targets = new ArrayList<>();
        retVal.targets.addAll(targets);
        retVal.oldMovePoints = oldMovePoints;
        retVal.attacking = attacking;
        retVal.complete = complete;
        return retVal;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CryHavoc cryHavoc) {
            return super.equals(cryHavoc) &&
                    this.attackingFigure == cryHavoc.attackingFigure &&
                    this.range == cryHavoc.range &&
                    this.oldMovePoints == cryHavoc.oldMovePoints &&
                    this.attacking == cryHavoc.attacking &&
                    this.complete == cryHavoc.complete &&
                    this.targets.equals(cryHavoc.targets);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attackingFigure, range, oldMovePoints, attacking, complete, targets);
    }
}
