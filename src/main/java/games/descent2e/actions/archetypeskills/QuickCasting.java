package games.descent2e.actions.archetypeskills;

import com.sun.xml.bind.v2.model.annotation.Quick;
import core.AbstractGameState;
import core.components.BoardNode;
import core.properties.PropertyInt;
import core.properties.PropertyVector2D;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.abilities.NightStalker;
import games.descent2e.actions.attack.ChainAttack;
import games.descent2e.actions.attack.FreeAttack;
import games.descent2e.actions.monsterfeats.Air;
import games.descent2e.actions.monsterfeats.MonsterAbilities;
import games.descent2e.components.DescentCard;
import games.descent2e.components.DicePool;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Vector2D;

import java.util.*;

import static core.CoreConstants.playersHash;
import static games.descent2e.DescentHelper.*;
import static games.descent2e.DescentHelper.checkReach;
import static games.descent2e.actions.attack.MeleeAttack.AttackPhase.PRE_DEFENCE_ROLL;

public class QuickCasting extends FreeAttack {

    public static boolean enabled = false;
    static int cardID = -1;
    public QuickCasting(int attackingFigure, int defendingFigure, boolean isMelee, boolean hasReach) {
        super(attackingFigure, defendingFigure, isMelee, hasReach);
    }

    @Override
    public boolean execute(DescentGameState state) {

        Figure f = (Figure) state.getComponentById(attackingFigure);
        boolean oldExtraAttack = f.hasUsedExtraAction();

        super.execute(state);

        f.setUsedExtraAction(oldExtraAttack);
        DescentCard card = (DescentCard) state.getComponentById(cardID);
        if (card != null)
            f.exhaustCard(card);
        disable();

        return true;
    }

    public boolean canExecute(DescentGameState dgs) {
        if (!isEnabled()) return false;

        Figure f = (Figure) dgs.getComponentById(attackingFigure);
        if (f == null) return false;
        if (!f.hasBonus(DescentTypes.SkillBonus.QuickCasting)) return false;
        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        if (card == null) return false;
        if (f.isExhausted(card)) return false;

        Figure target = (Figure) dgs.getComponentById(defendingFigure);
        if (target == null) return false;

        if (Air.checkAir(dgs, f, target)) {
            // If the target has the Air Immunity passive and we are not adjacent, we cannot attack them
            return false;
        }

        return checkAllSpaces(dgs, f, target, getRange(), true);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return super.getString(gameState).replace("Free ", "Quick Casting: ");
    }

    @Override
    public String toString() {
        return super.toString().replace("Free ", "Quick Casting: ");
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof QuickCasting) {
            return super.equals(o);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), enabled, cardID);
    }

    @Override
    public QuickCasting copy() {
        QuickCasting retValue = new QuickCasting(attackingFigure, defendingFigure, isMelee, hasReach);
        copyComponentTo(retValue);
        return retValue;
    }

    public static Set<QuickCasting> constructQuickCasting(DescentGameState dgs, int attackingFigure)
    {
        Set<QuickCasting> actions = new HashSet<>();

        Figure f = (Figure) dgs.getComponentById(attackingFigure);

        DescentTypes.AttackType attackType = getAttackType(f);
        boolean reach = checkReach(dgs, f);

        List<Integer> targets;

        if (attackType == DescentTypes.AttackType.MELEE || attackType == DescentTypes.AttackType.BOTH)
        {
            targets = getMeleeTargets(dgs, f, reach);
            for (Integer target : targets) {
                QuickCasting quickCasting = new QuickCasting(f.getComponentID(), target, true, reach);
                if (quickCasting.canExecute(dgs))
                    actions.add(quickCasting);
            }
        }

        if (attackType == DescentTypes.AttackType.RANGED || attackType == DescentTypes.AttackType.BOTH)
        {
            targets = getRangedTargets(dgs, f);
            for (Integer target : targets) {
                QuickCasting quickCasting = new QuickCasting(f.getComponentID(), target, false, false);
                if (quickCasting.canExecute(dgs))
                    actions.add(quickCasting);
            }
        }

        return actions;
    }

    public static boolean isEnabled() {
        return QuickCasting.enabled;
    }
    public static void enable() {
        QuickCasting.enabled = true;
    }
    public static void disable() {
        QuickCasting.enabled = false;
    }

    public static void setCardID(int cardID)
    {
        QuickCasting.cardID = cardID;
    }

    public static int getCardID() {
        return  QuickCasting.cardID;
    }
}
