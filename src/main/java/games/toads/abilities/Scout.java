package games.toads.abilities;


import games.toads.actions.ScoutCards;
import utilities.Pair;

import java.util.List;

public class Scout implements ToadAbility {

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        return List.of(
                new Pair<>(3, (isAttacker, isFlank, br) -> {
                    // add one to the ally card
                    br.addValue(isAttacker, !isFlank, 1);
                    // and look at opponent's hand (if we are the Defender, we must be the current player)
                    br.postBattleActions.add(new ScoutCards(isAttacker ? 0 : 1));
                    //    br.state.seeOpponentsHand(isAttacker ? br.attacker : 1 - br.attacker);
                }));
    }
}
