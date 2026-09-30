package games.toads.abilities;

import utilities.Pair;

import java.util.List;

import static games.toads.abilities.AbilityConstants.START;

public class GeneralFlags implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        // +1 to the Ally for each of the owner's Flags in the Shrine this War
        return List.of(new Pair<>(START, (isAttacker, isFlank, br) -> {
            int owner = isAttacker ? br.attacker : 1 - br.attacker;
            br.addValue(isAttacker, !isFlank, br.state.getShrineFlags(br.state.getRoundCounter(), owner));
        }));
    }
}
