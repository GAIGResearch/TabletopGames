package games.wonders7.cards;

import core.CoreConstants;
import core.components.Deck;
import games.wonders7.Wonders7GameState;

import static games.wonders7.Wonders7Constants.Resource.*;
import static games.wonders7.Wonders7Constants.createCardHash;
import static games.wonders7.cards.Wonder7Card.Type.Guilds;

public class Wonder7Guilds {

    public final Deck<Wonder7Card> allGuilds = new Deck<>("Guilds", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);

    public Wonder7Guilds() {
        allGuilds.add(workersGuild);
        allGuilds.add(craftsmenGuild);
        allGuilds.add(magistratesGuild);
        allGuilds.add(tradersGuild);
        allGuilds.add(spiesGuild);
        allGuilds.add(philosophersGuild);
        allGuilds.add(shipownersGuild);
        allGuilds.add(buildersGuild);
        allGuilds.add(decoratorsGuild);
        allGuilds.add(scientistsGuild);
    }
    protected static int neighbourCardsOfType(Wonder7Card.Type type, Wonders7GameState state, int player) {
        int retValue = 0;
        for (int i = -1; i < 2; i += 2) {
            retValue += state.cardsOfType(type, (player + i + state.getNPlayers()) % state.getNPlayers());
        }
        return retValue;
    }

    public Wonder7Card workersGuild = new Wonder7Card("Workers Guild", Guilds, createCardHash(Ore, Ore, Clay, Stone, Wood),
            (state, player) -> neighbourCardsOfType(Wonder7Card.Type.RawMaterials, state, player));

    public Wonder7Card craftsmenGuild = new Wonder7Card("Craftsmen Guild", Guilds, createCardHash(Ore, Ore, Stone, Stone),
            (state, player) -> 2 * neighbourCardsOfType(Wonder7Card.Type.ManufacturedGoods, state, player));

    public Wonder7Card magistratesGuild = new Wonder7Card("Magistrates Guild", Guilds, createCardHash(Wood, Wood, Wood, Stone, Textile),
            (state, player) -> neighbourCardsOfType(Wonder7Card.Type.CivilianStructures, state, player));

    public Wonder7Card tradersGuild = new Wonder7Card("Traders Guild", Guilds, createCardHash(Ore, Ore, Clay, Glass, Papyrus),
            (state, player) -> neighbourCardsOfType(Wonder7Card.Type.CommercialStructures, state, player));

    public Wonder7Card spiesGuild = new Wonder7Card("Spies Guild", Guilds, createCardHash(Clay, Clay, Glass),
            (state, player) -> neighbourCardsOfType(Wonder7Card.Type.MilitaryStructures, state, player));

    public Wonder7Card philosophersGuild = new Wonder7Card("Philosophers Guild", Guilds, createCardHash(Clay, Clay, Clay, Papyrus, Textile),
            (state, player) -> neighbourCardsOfType(Wonder7Card.Type.ScientificStructures, state, player));

    public Wonder7Card shipownersGuild = new Wonder7Card("Shipowners Guild", Guilds, createCardHash(Wood, Wood, Wood, Glass, Papyrus),
            (state, player) -> state.cardsOfType(Wonder7Card.Type.RawMaterials, player) +
                    state.cardsOfType(Wonder7Card.Type.ManufacturedGoods, player) +
                    state.cardsOfType(Guilds, player));

    public Wonder7Card buildersGuild = new Wonder7Card("Builders Guild", Guilds, createCardHash(Stone, Stone, Stone, Clay, Clay, Glass),
            (state, player) -> {
                int retValue = 0;
                for (int i = -1; i <=1; i ++) {
                    retValue += state.getPlayerWonderBoard((player + i + state.getNPlayers()) % state.getNPlayers()).wonderStage - 1;
                }
                return retValue;
            });

    public Wonder7Card decoratorsGuild = new Wonder7Card("Decorators Guild", Guilds, createCardHash(Ore, Ore, Stone, Stone, Glass, Glass),
            (state, player) -> state.getPlayerWonderBoard(player).wonderStage == (state.getPlayerWonderBoard(player).type.wonderStages+1) ? 7 : 0);

    public Wonder7Card scientistsGuild = new Wonder7Card("Scientists Guild",
            Guilds, createCardHash(Wood, Wood, Ore, Ore, Papyrus),
            createCardHash(ScienceWild));
}
