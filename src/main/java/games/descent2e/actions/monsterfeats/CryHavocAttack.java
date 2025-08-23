package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.attack.MultiAttack;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.List;

import static games.descent2e.DescentHelper.checkAllSpaces;
import static games.descent2e.DescentHelper.inRange;

public class CryHavocAttack extends MultiAttack {

    int range;
    public CryHavocAttack(int attackingFigure, List<Integer> defendingFigures, int range) {
        super(attackingFigure, defendingFigures);
        this.isMelee = true; // By default, Belthir is a Melee figure
        this.range = range; // How many spaces he can move
        this.isFreeAttack = true; // CryHavoc only costs 1 Action despite being two actions
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {

        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (f == null) return false;
        if (f.hasAttacked()) return false; // Monsters can only attack once per turn
        if (!(f instanceof Monster) || !(((Monster) f).hasAction(MonsterAbilities.MonsterAbility.CRYHAVOC))) return false;

        // Belthir's targets can be at maximum one less than the number of spaces he can move
        // as he must finish his movement on an empty space
        int range = getRange();

        if (defendingFigures.isEmpty()) return false; // No targets to attack
        if (defendingFigures.size() > range) return false; // Too many targets to attack

        for (int defendingFigure : defendingFigures)
        {
            Figure target = (Figure) dgs.getComponentById(defendingFigure);
            if (target == null) return false;

            // This is a retroactive attack - narratively, Belthir is striking every target as he moves through them
            // Instead, we gather his movement first, then target everyone he flies over

            if (!checkAllSpaces(dgs, f, target, range, false)) return false;
        }
        return true;
    }

    @Override
    public CryHavocAttack copy()
    {
        CryHavocAttack retValue = new CryHavocAttack(attackingFigure, defendingFigures, range);
        copyComponentTo(retValue);
        return retValue;
    }

    @Override
    public int getRange()
    {
        return range;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Multi Attack", "Cry Havoc Attack");
    }

    @Override
    public String toString()
    {
        return super.toString().replace("Multi Attack", "Cry Havoc Attack");
    }
}
