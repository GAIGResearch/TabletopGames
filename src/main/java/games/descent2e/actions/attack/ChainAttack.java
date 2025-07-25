package games.descent2e.actions.attack;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.descent2e.DescentHelper.*;

public class ChainAttack extends MultiAttack{

    protected int distance;
    protected List<Vector2D> pathway;
    boolean lineOfSight = false; // Check whether we need Line Of Sight for our attacks

    // A Chain Attack is a Multi Attack where all targets are within a set distance from the first target
    // Think of it like Chain Lightning - you have a set distance you can travel for the attack
    // And you can only hit targets that you can pass through during that distance

    public ChainAttack(int attackingFigure, List<Integer> defendingFigures, int distance, List<Vector2D> pathway, boolean lineOfSight) {
        super(attackingFigure, defendingFigures);
        this.distance = distance;
        this.lineOfSight = lineOfSight;
        this.pathway = new ArrayList<>();
        for (Vector2D v : pathway) {
            this.pathway.add(v.copy());
        }
    }

    @Override
    public boolean execute(DescentGameState state) {
        return super.execute(state);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();

        if (!isFreeAttack) {
            if (f.getNActionsExecuted().isMaximum()) return false;
        }

        // Figures can't end turn on an occupied space
        // Therefore, an attack cannot have more targets than the distance it can legally move
        if (defendingFigures.size() > distance) return false;

        int remaining = distance;

        for (int i = 0; i < defendingFigures.size(); i++)
        {
            int defendingFigure = defendingFigures.get(i);
            Figure target = (Figure) dgs.getComponentById(defendingFigure);
            if (target == null) return false;

            if (target instanceof Monster)
            {
                if (((Monster) target).hasPassive(MonsterAbilities.MonsterPassive.AIR) &&
                        !checkAdjacent(dgs, f, target)) {
                    // If the target has the Air Immunity passive and we are not adjacent, we cannot attack them
                    return false;
                }
            }

            // We need to check from the initial target's position
            if (i < 1) continue;
            Figure firstTarget = (Figure) dgs.getComponentById(defendingFigures.get(0));
            if (!checkAllSpaces(dgs, firstTarget, target, distance, lineOfSight)) return false;

            // And then compare it to the previous target's position
            if (i < 2) continue;
            Figure previousTarget = (Figure) dgs.getComponentById(defendingFigures.get(i - 1));
            int difference = getRangeAllSpaces(dgs, previousTarget, target);
            remaining -= difference;
            if (remaining < 0) return false;
            if (!checkAllSpaces(dgs, previousTarget, target, remaining, lineOfSight)) return false;
        }
        return true;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String path = pathway.isEmpty() ? "Chain Attack by" : "Chain Attack along ";
        for (int i = 0; i < pathway.size(); i++) {
            if (i == pathway.size() - 1)
                path += pathway.get(i) + " by ";
            else
                path += pathway.get(i) + ", ";
        }

        return super.getString(gameState).replace("Multi Attack by ", path);
    }

    @Override
    public String toString() {
        return super.toString().replace("Multi Attack by ", "Chain Attack by ");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ChainAttack that = (ChainAttack) o;
        return index == that.index && Objects.equals(defendingFigures, that.defendingFigures)
                && distance == that.distance && Objects.equals(pathway, that.pathway) && lineOfSight == that.lineOfSight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), distance, pathway.hashCode(), lineOfSight);
    }

    @Override
    public ChainAttack copy() {
        ChainAttack retValue = new ChainAttack(attackingFigure, defendingFigures, distance, pathway, lineOfSight);
        copyComponentTo(retValue);
        return retValue;
    }

}
