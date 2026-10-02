package games.toads.abilities;

import games.toads.actions.SiegeCannonGuess;
import utilities.Pair;

import java.util.List;

import static games.toads.abilities.AbilityConstants.AFTER;

public class SiegeCannon implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        // the owner guesses a card in the opponent's hand after the battle
        return List.of(new Pair<>(AFTER, (isAttacker, isFlank, br) ->
                br.postBattleActions.add(new SiegeCannonGuess(isAttacker ? br.attacker : 1 - br.attacker))));
    }
}
