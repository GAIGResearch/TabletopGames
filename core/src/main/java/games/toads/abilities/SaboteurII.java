package games.toads.abilities;

import utilities.Pair;

import java.util.List;

public class SaboteurII implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        return List.of(
                // we break ties in our favour for us and our ally
                new Pair<>(7, (isAttacker, isFlank, br) -> {
                    if (br.getCurrentValue(isAttacker, isFlank) - br.getCurrentValue(!isAttacker, isFlank) == 0) {
                        br.addValue(isAttacker, isFlank, 1);
                    }
                    if (br.getCurrentValue(isAttacker, !isFlank) - br.getCurrentValue(!isAttacker, !isFlank) == 0) {
                        br.addValue(isAttacker, !isFlank, 1);
                    }
                }
                ));
    }
}
