package games.toads.abilities;

import utilities.Pair;

import java.util.List;

import static games.toads.abilities.AbilityConstants.START;

public class GeneralHostages implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        // +1 to the Ally for each Hostage the opponent has captured this War
        return List.of(new Pair<>(START, (isAttacker, isFlank, br) -> {
            int opponent = isAttacker ? 1 - br.attacker : br.attacker;
            br.addValue(isAttacker, !isFlank, br.state.getBattlesWon(br.state.getRoundCounter(), opponent));
        }));
    }
}
