package games.toads.abilities;

import utilities.Pair;

import java.util.List;

import static games.toads.abilities.AbilityConstants.DURING;

public class SaboteurIII implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        // the Ally breaks ties
        return List.of(new Pair<>(DURING, (isAttacker, isFlank, br) -> br.setTieBreak(isAttacker, !isFlank)));
    }
}
