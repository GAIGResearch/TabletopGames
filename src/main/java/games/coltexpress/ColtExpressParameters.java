package games.coltexpress;

import core.GameParameters;
import games.coltexpress.cards.ColtExpressCard;

import java.util.HashMap;

public class ColtExpressParameters extends GameParameters {

    HashMap<ColtExpressCard.CardType, Integer> cardCounts = new HashMap<ColtExpressCard.CardType, Integer>() {{
        put(ColtExpressCard.CardType.MoveSideways, 2);
        put(ColtExpressCard.CardType.MoveUp, 2);
        put(ColtExpressCard.CardType.Punch, 1);
        put(ColtExpressCard.CardType.MoveMarshal, 1);
        put(ColtExpressCard.CardType.Shoot, 2);
        put(ColtExpressCard.CardType.CollectMoney, 2);
    }};

    public enum CharacterType {
        Ghost,
        Cheyenne,
        Django,
        Tuco,
        Doc,
        Belle
    }
}
