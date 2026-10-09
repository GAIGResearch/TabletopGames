package games.toads.abilities;

import games.toads.ToadConstants.ToadCardType;
import utilities.Pair;

import java.util.List;

import static games.toads.abilities.AbilityConstants.DURING;

public class AssassinII implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        return List.of(new Pair<>(DURING, (isAttacker, isFlank, br) -> {
            // +2.5 to the lower of the Ally and its Foe, as they stood at the start of the stage
            if (br.getAlly(isAttacker, isFlank).type == ToadCardType.SIEGE_CANNON ||
                    br.getCard(!isAttacker, !isFlank).type == ToadCardType.SIEGE_CANNON)
                return;  // a Siege Cannon's lane ignores Strength
            double ally = br.getSnapshotValue(isAttacker, !isFlank);
            double foe = br.getSnapshotValue(!isAttacker, !isFlank);
            if (ally < foe)
                br.addValue(isAttacker, !isFlank, 2.5);
            else if (foe < ally)
                br.addValue(!isAttacker, !isFlank, 2.5);
        }));
    }
}
