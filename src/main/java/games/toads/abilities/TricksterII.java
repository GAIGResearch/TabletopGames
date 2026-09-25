package games.toads.abilities;

import utilities.Pair;

import java.util.List;

import static games.toads.abilities.AbilityConstants.START;

public class TricksterII implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        return List.of(new Pair<>(START, (isAttacker, isFlank, br) -> br.swapFieldAndFlank(isAttacker ? 0 : 1)));
    }

    @Override
    public boolean canBeBlocked() {
        return false;
    }
}
