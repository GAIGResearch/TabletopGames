package games.toads.abilities;

import utilities.Pair;

import java.util.List;

public class GeneralOne implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        return List.of(
                new Pair<>(5, (isAttacker, isFlank, br) -> br.frogOverride[isAttacker ? 0 : 1] = true
        ));
    }
}
