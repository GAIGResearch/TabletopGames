package games.coltexpress.cards;

import core.components.Card;

public class ColtExpressCard extends Card {
    public enum CardType {
        MoveSideways,
        CollectMoney,
        Punch,
        MoveMarshal,
        MoveUp,
        Shoot,
        Bullet
    }

    public CardType cardType;
    public final int playerID;

    public ColtExpressCard(int playerID, CardType cardType) {
        super(cardType.toString());
        this.cardType = cardType;
        this.playerID = playerID;
    }

    public String toString(){
        return cardType.toString() + "(" + playerID + ")";
    }

}
