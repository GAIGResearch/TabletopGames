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
                    if (isAttacker) {
                        if (isFlank) {
                            if (br.DField - br.AField == 1) {
                                br.AField++;
                            }
                        } else {
                            if (br.DFlank - br.AFlank == 1) {
                                br.AFlank++;
                            }
                        }
                    } else {
                        if (isFlank) {
                            if (br.AField - br.DField == 1) {
                                br.DField++;
                            }
                        } else {
                            if (br.AFlank - br.DFlank == 1) {
                                br.DFlank++;
                            }
                        }
                    }
                }));
    }
}
