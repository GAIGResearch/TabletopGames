package games.toads.abilities;

import games.toads.components.ToadCard;

import java.util.List;

public class Assassin implements ToadAbility {

    @Override
    public List<CardModifier> attributes() {
        return List.of((isAttacker, isFlank, br) -> {
            // If the opponent card is a General, then we set its value to 0.
            ToadCard card = br.getCardOpposite(isAttacker, isFlank);
            if (card.value == 7)
                return 20; // we win
            return 0;
        });
    }
}
