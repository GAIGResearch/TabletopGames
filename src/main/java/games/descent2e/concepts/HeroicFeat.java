package games.descent2e.concepts;

import games.descent2e.DescentGameState;
import games.descent2e.DescentHelper;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.herofeats.*;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;

import java.util.ArrayList;
import java.util.List;

import static games.descent2e.DescentHelper.*;
import static games.descent2e.concepts.HeroicFeat.HeroFeat.*;

public class HeroicFeat {
    public enum HeroFeat {
        DoubleAttack,
        ExtraAttack,
        FreeMoveFriend,
        HealAll,
        MagicAttackAll,
        RunAttack,
        StunMonsters,
        Vanish,
        NONE;
    }

    public static List<DescentAction> getHeroicFeatActions(DescentGameState dgs) {
        Figure f = dgs.getActingFigure();
        if (!(f instanceof Hero)) {
            return null;
        }

        HeroFeat heroFeat = ((Hero) f).getHeroicFeat();
        if (heroFeat == null || heroFeat == NONE) return null;
        List<DescentAction> myFeats = new ArrayList<>();
        DescentAction feat;
        switch (heroFeat) {

            // ----- HEALER -----

            case HealAll:
                feat = new HealAllInRange(3);
                if (feat.canExecute(dgs)) myFeats.add(feat);
                break;

            case StunMonsters:
                for (List<Monster> monsters : dgs.getMonsters()) {
                    if (monsters.isEmpty()) continue;
                    feat = new StunAllInMonsterGroup(monsters, 3);
                    if (feat.canExecute(dgs))
                        myFeats.add(feat);
                }
                break;

            // ----- MAGE -----

            case DoubleAttack:
                List<Integer> targets = getRangedTargets(dgs, f);
                for (int i = 0; i < targets.size(); i++) {
                    for (int j = i + 1; j < targets.size(); j++) {
                        List<Integer> targetPair = new ArrayList<>();

                        int targetA = targets.get(i);
                        int targetB = targets.get(j);

                        // Ensure that we always put the closest target first
                        if (DescentHelper.getDistance(f.getPosition(), ((Figure) dgs.getComponentById(targetA)).getPosition()) >
                                DescentHelper.getDistance(f.getPosition(), ((Figure) dgs.getComponentById(targetB)).getPosition())) {
                            targetPair.add(targetB);
                            targetPair.add(targetA);
                        }
                        else {
                            targetPair.add(targetA);
                            targetPair.add(targetB);
                        }

                        feat = new DoubleAttack(f.getComponentID(), targetPair);
                        if (feat.canExecute(dgs)) myFeats.add(feat);
                    }
                }
                break;

            case MagicAttackAll:
                feat = new MagicAttackAll();
                if (feat.canExecute(dgs)) myFeats.add(feat);
                break;

            // ----- SCOUT -----

            case Vanish:
                feat = new RemoveFromMap();
                if (feat.canExecute(dgs)) myFeats.add(feat);
                feat = new ReturnToMapMove(4);
                if (feat.canExecute(dgs)) myFeats.add(feat);
                feat = new ReturnToMapPlace();
                if (feat.canExecute(dgs)) myFeats.add(feat);
                break;

            case RunAttack:
                feat = new DoubleMoveAttack();
                if (feat.canExecute(dgs)) myFeats.add(feat);
                break;

            // ----- WARRIOR -----

            case FreeMoveFriend:

                for (Hero ally : dgs.getHeroes()) {
                    if (ally.getComponentID() == f.getComponentID())
                        continue;
                    if (DescentHelper.inRange(f.getPosition(), ally.getPosition(), 3)) {
                        feat = new HeroicFeatExtraMovement(f.getComponentID(), ally.getComponentID());
                        if (feat.canExecute(dgs)) myFeats.add(feat);
                    }
                }
                break;

            case ExtraAttack:
                DescentTypes.AttackType attackType = getAttackType(f);
                if (attackType == DescentTypes.AttackType.MELEE || attackType == DescentTypes.AttackType.BOTH) {
                    for (int target : getMeleeTargets(dgs, f)) {
                        feat = new HeroicFeatExtraAttack(dgs.getActingFigure().getComponentID(), target, true);
                        if (feat.canExecute(dgs)) myFeats.add(feat);
                    }
                }
                if (attackType == DescentTypes.AttackType.RANGED || attackType == DescentTypes.AttackType.BOTH) {
                    for (int target : getRangedTargets(dgs, f)) {
                        feat = new HeroicFeatExtraAttack(dgs.getActingFigure().getComponentID(), target, false);
                        if (feat.canExecute(dgs)) myFeats.add(feat);
                    }
                }
                break;
        }

        if (myFeats.isEmpty()) return null;
        return myFeats;
    }

}
