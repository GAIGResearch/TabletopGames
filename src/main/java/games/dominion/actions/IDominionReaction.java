package games.dominion.actions;

import games.dominion.cards.CardType;

public interface IDominionReaction {

    public CardType getCardType();
    public int getPlayer();

}
