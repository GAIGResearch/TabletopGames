package games.wonders7.cards;

import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameState;

public class GainResourceEffect implements CardEffect {

    final Wonders7Constants.Resource resource;
    final int multiplier;
    final boolean self;
    final boolean neighbours;
    final Wonder7Card.Type type;

    public GainResourceEffect(Wonders7Constants.Resource resource, Wonder7Card.Type type, int multiplier, boolean self, boolean neighbours) {
        this.resource = resource;
        this.multiplier = multiplier;
        this.self = self;
        this.neighbours = neighbours;
        this.type = type;
    }

    public int gain(Wonders7GameState state, int player) {
        int count = 0;
        if (self) {
            count += state.cardsOfType(type, player);
        }
        if (neighbours) {
            for (int i = -1; i < 2; i += 2) {
                count += state.cardsOfType(type, (player + i + state.getNPlayers()) % state.getNPlayers());
            }
        }
        count *= multiplier;
        return count;
    }

    public void apply(Wonders7GameState state, int playerId) {
        int gain = gain(state, playerId);
        state.getPlayerResources(playerId).put(resource, state.getPlayerResources(playerId).get(resource) + gain);
    }
}
