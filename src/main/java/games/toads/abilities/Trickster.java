package games.toads.abilities;

import games.toads.components.ToadCard;
import utilities.Pair;

import java.util.List;

public class Trickster implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        // trickster swaps itself with its ally
        return List.of(
                new Pair<>(-1, (isAttacker, isFlank, br) -> br.swapFieldAndFlank(isAttacker ? 0 : 1)),
                new Pair<>(3, (isAttacker, isFlank, br) -> {
                    ToadCard ally = br.getAlly(isAttacker, isFlank);
                    br.addValue(isAttacker, isFlank, ally.value / 2);
                }));
    }
}
