package games.toads.abilities;

import games.toads.components.ToadCard;
import utilities.Pair;

import java.util.List;

public class Berserker implements ToadAbility {


    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        // assassins copy their ally's tactics
        return List.of(new Pair<>(5, (isAttacker, isFlank, br) -> {
            // we add to our value the number of battles won by the opponent
            int opponent = isAttacker ? 1 - br.attacker : br.attacker;
            int battlesWon = br.state.getBattlesWon(br.state.getRoundCounter(), opponent);
            br.addValue(isAttacker, isFlank, battlesWon);
        }));
    }

}
