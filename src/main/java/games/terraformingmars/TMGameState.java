package games.terraformingmars;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.*;
import core.interfaces.IGamePhase;
import core.turnorders.AlternatingTurnOrder;
import games.terraformingmars.components.Corporation;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.Bonus;
import games.terraformingmars.components.TMMapTile;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TMGameState extends AbstractGameState {

    enum TMPhase implements IGamePhase {
        Setup,
        Research,
        Actions,
        Production
    }

    GridBoard<TMMapTile> board;
    TMMapTile[] extraTiles;

    Counter[] globalParameters;
    Bonus[] bonuses;

    Deck<TMCard> projectCards, corpCards;  // Face-down decks

    HashMap<TMTypes.Resource, Counter>[] playerResources;
    HashMap<TMTypes.Resource, Counter>[] playerProduction;

    // TODO: initialize
    PartialObservableDeck<TMCard>[] playerCardsPlayed;
    HashMap<TMTypes.Tag, Counter>[] playerCardsPlayedTags;
    Counter[] playerNCities;
    Counter[] playerNGreeneries;

    PartialObservableDeck<TMCard>[] playerHands;
    PartialObservableDeck<TMCard>[] playerCardChoice;
    Deck<Corporation>[] playerCorporations;

    // To add: milestones, awards

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     */
    public TMGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new AlternatingTurnOrder(nPlayers));
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(board);
            add(projectCards);
            add(corpCards);
            this.addAll(Arrays.asList(extraTiles));
            addAll(Arrays.asList(globalParameters));
//            this.addAll(Arrays.asList(playerCardsPlayed));
//            this.addAll(Arrays.asList(playerNCities));
//            this.addAll(Arrays.asList(playerNGreeneries));
//            this.addAll(Arrays.asList(playerHands));
//            this.addAll(Arrays.asList(playerCardChoice));
//            this.addAll(Arrays.asList(playerCorporations));
            for (int i = 0; i < getNPlayers(); i++) {
                addAll(playerResources[i].values());
                addAll(playerProduction[i].values());
//                addAll(playerCardsPlayedTags[i].values());
            }
        }};  // TODO
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
//        TMGameState copy = new TMGameState(gameParameters, getNPlayers());  // TODO

        return this;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return 0;
    }

    @Override
    public double getGameScore(int playerId) {
        return 0;
    }

    @Override
    protected void _reset() {

    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }

    public static Counter stringToGPCounter(TMGameState gs, String s) {
        for (Counter c: gs.globalParameters) {
            if (c.getComponentName().equalsIgnoreCase(s)) {
                return c;
            }
        }
        return null;
    }

    public static TMTypes.GlobalParameter counterToGP(Counter c) {
        return Utils.searchEnum(TMTypes.GlobalParameter.class, c.getComponentName());
    }

    /*
     * Public API
     */

    public HashMap<TMTypes.Resource, Counter>[] getPlayerProduction() {
        return playerProduction;
    }

    public HashMap<TMTypes.Resource, Counter>[] getPlayerResources() {
        return playerResources;
    }

    public Deck<TMCard>[] getPlayerCardsPlayed() {
        return playerCardsPlayed;
    }

    public GridBoard<TMMapTile> getBoard() {
        return board;
    }

    public Bonus[] getBonuses() {
        return bonuses;
    }

    public Counter[] getGlobalParameters() {
        return globalParameters;
    }

    public TMMapTile[] getExtraTiles() {
        return extraTiles;
    }

    public PartialObservableDeck<TMCard>[] getPlayerHands() {
        return playerHands;
    }
}
