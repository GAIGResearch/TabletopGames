package games.toads.abilities;

import games.toads.components.ToadCard;
import utilities.Pair;

import java.util.List;

public class IconBearer implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        return List.of(
                new Pair<>(-1, (isAttacker, isFlank, br) -> {
                    // we activate the allied card (if it is not already activated)
                    if (!br.isActivated(isAttacker, !isFlank)) {
                        br.setActivation(isAttacker, !isFlank, true);
                    }
                }),
                new Pair<>(5, (isAttacker, isFlank, br) -> {
                    // ally gains 1 value if this would create a tie
                    if (br.getCurrentValue(isAttacker, !isFlank) == br.getCurrentValue(!isAttacker, !isFlank) - 1) {
                        br.addValue(isAttacker, !isFlank, 1);
                    }
                }));
    }
}
