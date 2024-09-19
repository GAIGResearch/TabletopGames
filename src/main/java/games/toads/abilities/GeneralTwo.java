package games.toads.abilities;

import utilities.Pair;

import java.util.List;

public class GeneralTwo implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        return List.of(
                new Pair<>(5, (isAttacker, isFlank, br) -> br.addValue(isAttacker, !isFlank, br.state.getBattlesTied(br.state.getRoundCounter()))
                ));
    }

}
