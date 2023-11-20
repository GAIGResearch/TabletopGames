package games.descent2e.concepts;

import games.descent2e.actions.DescentAction;
import games.descent2e.actions.herofeats.*;

import java.util.ArrayList;
import java.util.List;

public enum HeroicFeat {
    DoubleAttack (new DoubleAttack()),
    MagicAttackAll (new MagicAttackAll()),
    HealAll (new HealAllInRange()),
    StunMonsters (new StunAllInMonsterGroup()),
    FreeMoveFriend (new HeroicFeatExtraMovement()),
    ExtraAttack (new HeroicFeatExtraAttack()),
    Vanish (new RemoveFromMap()),
    RunAttack (new DoubleMoveAttack());

    public final List<DescentAction> actions = new ArrayList<>();
    HeroicFeat(DescentAction action) {
        this.actions.add(action);
    }
}
