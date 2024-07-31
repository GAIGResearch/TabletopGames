package games.toads.abilities;

import games.toads.ToadConstants;
import games.toads.components.ToadCard;
import utilities.Pair;

import java.util.List;

public class Saboteur implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        // saboteur cancels the opponent's tactics
        return List.of(new Pair<>(-5, (isAttacker, isFlank, br) -> {
            // we inactivate the card opposite (this removes any Tactics as well)
            br.setActivation(!isAttacker, isFlank, false);
        }));
    }
}
