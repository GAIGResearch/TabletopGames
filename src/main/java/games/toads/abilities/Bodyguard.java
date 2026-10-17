package games.toads.abilities;

import utilities.Pair;

import java.util.List;

import static games.toads.abilities.AbilityConstants.BLOCK;

public class Bodyguard implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        // blocks the opponent's hidden card
        return List.of(new Pair<>(BLOCK, (isAttacker, isFlank, br) -> br.block(!isAttacker, true)));
    }
}
