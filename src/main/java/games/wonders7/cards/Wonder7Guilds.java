package games.wonders7.cards;

import core.CoreConstants;
import core.components.Deck;

import java.util.Collections;
import java.util.List;

import static games.wonders7.Wonders7Constants.Resource.*;
import static games.wonders7.Wonders7Constants.createCardHash;
import static games.wonders7.cards.Wonder7Card.Type.*;

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

    public Wonder7Card workersGuild = new Wonder7Card("Workers Guild", Guilds, createCardHash(Ore, Ore, Clay, Stone, Wood),
            Collections.emptyList(),
            List.of(new GainResourceEffect(Victory, RawMaterials, 1, false, true)));

    public Wonder7Card craftsmenGuild = new Wonder7Card("Craftsmen Guild", Guilds, createCardHash(Ore, Ore, Stone, Stone),
            Collections.emptyList(),
            List.of(new GainResourceEffect(Victory, ManufacturedGoods, 2, false, true)));

    public Wonder7Card magistratesGuild = new Wonder7Card("Magistrates Guild", Guilds, createCardHash(Wood, Wood, Wood, Stone, Textile),
            Collections.emptyList(),
            List.of(new GainResourceEffect(Victory, CivilianStructures, 1, false, true)));

    public Wonder7Card tradersGuild = new Wonder7Card("Traders Guild", Guilds, createCardHash(Ore, Ore, Clay, Glass, Papyrus),
            Collections.emptyList(),
            List.of(new GainResourceEffect(Victory, CommercialStructures, 1, false, true)));

    public Wonder7Card spiesGuild = new Wonder7Card("Spies Guild", Guilds, createCardHash(Clay, Clay, Glass),
            Collections.emptyList(),
            List.of(new GainResourceEffect(Victory, MilitaryStructures, 1, false, true)));

    public Wonder7Card philosophersGuild = new Wonder7Card("Philosophers Guild", Guilds, createCardHash(Clay, Clay, Clay, Papyrus, Textile),
            Collections.emptyList(),
            List.of(new GainResourceEffect(Victory, ScientificStructures, 1, false, true)));

    public Wonder7Card shipownersGuild = new Wonder7Card("Shipowners Guild", Guilds, createCardHash(Wood, Wood, Wood, Glass, Papyrus),
            Collections.emptyList(),
            List.of(new GainResourceEffect(Victory, CommercialStructures, 1, true, false),
                    new GainResourceEffect(Victory, RawMaterials, 1, true, false),
                    new GainResourceEffect(Victory, ManufacturedGoods, 1, true, false)));

    public Wonder7Card buildersGuild = new Wonder7Card("Builders Guild", Guilds, createCardHash(Stone, Stone, Stone, Clay, Clay, Glass),
            Collections.emptyList(),
            List.of((state, player) -> {
                int retValue = 0;
                for (int i = -1; i <= 1; i++) {
                    retValue += state.getPlayerWonderBoard((player + i + state.getNPlayers()) % state.getNPlayers()).wonderStage - 1;
                }
                state.getPlayerResources(player).put(Victory, state.getPlayerResources(player).get(Victory) + retValue);
            }));

    public Wonder7Card decoratorsGuild = new Wonder7Card("Decorators Guild", Guilds, createCardHash(Ore, Ore, Stone, Stone, Glass, Glass),
            Collections.emptyList(),
            List.of((state, player) -> {
                int vp = 0;
                for (int i = -1; i <= 1; i++) {
                    vp += state.getPlayerWonderBoard((player + i + state.getNPlayers()) % state.getNPlayers()).wonderStage == 3 ? 3 : 0;
                }
                state.getPlayerResources(player).put(Victory, state.getPlayerResources(player).get(Victory) + vp);
            }));

    public Wonder7Card scientistsGuild = new Wonder7Card("Scientists Guild",
            Guilds, createCardHash(Wood, Wood, Ore, Ore, Papyrus),
            createCardHash(ScienceWild));
}
