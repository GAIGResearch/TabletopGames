package games.explodingkittens.actions;

import core.AbstractGameState;

public interface IsNopeable {
    void nopedExecute(AbstractGameState gs);
    void actionPlayed(AbstractGameState gs);
}
