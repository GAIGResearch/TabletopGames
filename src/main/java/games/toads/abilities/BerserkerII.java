package games.toads.abilities;

import utilities.Pair;

import java.util.List;

import static games.toads.abilities.AbilityConstants.DURING;

public class BerserkerII implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        // the owner is Angry, so keeps both Hostages if they win both lanes
        return List.of(new Pair<>(DURING, (isAttacker, isFlank, br) -> br.frogOverride[isAttacker ? 0 : 1] = true));
    }
}
