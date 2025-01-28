package games.wonders7.cards;

import games.wonders7.Wonders7GameState;

public interface CardEffect {
    void apply(Wonders7GameState state, int playerId);
}
