package games.toads.abilities;

import static games.toads.ToadConstants.*;
import games.toads.components.ToadCard;
import utilities.Pair;

import java.util.List;

public class Assassin implements ToadAbility {

    @Override
    public List<CardModifier> attributes() {
        return List.of((isAttacker, isFlank, br) -> {
            // If the opponent card is a General, then we set its value to 0.
            ToadCard card = br.getOpponent(isAttacker, isFlank);
            if (card.value == ASSASSIN_KILLS)
                return 20; // we win
            return 0;
        });
    }

    @Override
    public List<Pair<Integer, BattleEffect>> tactics() {
        // assassins copy their ally's tactics
        return List.of(new Pair<>(-1, (isAttacker, isFlank, br) -> {
            ToadCard ally = br.getAlly(isAttacker, isFlank);
            br.tacticsToApply.addAll(ally.ability.tactics().stream().map(pair -> new BattleResult.Tactic(pair.a, isAttacker, isFlank, pair.b)).toList());
        }));
    }
}
