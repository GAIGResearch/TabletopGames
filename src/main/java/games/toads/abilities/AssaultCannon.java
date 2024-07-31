package games.toads.abilities;

import games.toads.ToadConstants;
import games.toads.components.ToadCard;

import java.util.List;

public class AssaultCannon implements ToadAbility {
    @Override
    public List<CardModifier> attributes() {
        return List.of((isAttacker, isFlank, br) -> {
            ToadCard card = br.getOpponent(isAttacker, isFlank);
            // if we are not attacking, then our value is unchanged
            if (!isAttacker)
                return 0;
            // we also have a value of zero against a Saboteur
            if (card.type == ToadConstants.ToadCardType.SABOTEUR || card.type == ToadConstants.ToadCardType.SABOTEURII)
                return 0; // we win
            // otherwise we are the attacker, and are not facing the Saboteur, so we win
            return 20;
        });
    }
}
