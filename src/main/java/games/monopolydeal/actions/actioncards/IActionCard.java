package games.monopolydeal.actions.actioncards;

import games.monopolydeal.MonopolyDealGameState;

public interface IActionCard {
    int getTarget(MonopolyDealGameState gs);
}
