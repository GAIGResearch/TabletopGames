package games.dominion.actions;

import games.dominion.cards.CardType;

public interface IDominionReaction {

    CardType getCardType();
    int getPlayer();

}
