package games.coltexpress.cards;

import core.components.Card;

public class ColtExpressCard extends Card {
    public enum CardType {
        MoveSideways,
        CollectMoney,
        Punch,
        MoveSheriff,
        MoveUp,
        Shoot,
        Bullet
    }

    public CardType cardType;

    public ColtExpressCard(CardType cardType) {
        this.cardType = cardType;
    }


}
