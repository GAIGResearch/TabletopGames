package games.terraformingmars;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.components.*;
import core.interfaces.IGamePhase;
import core.turnorders.AlternatingTurnOrder;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.Award;
import games.terraformingmars.rules.Bonus;
import games.terraformingmars.components.TMMapTile;
import games.terraformingmars.rules.Milestone;
import utilities.Utils;

import java.util.*;

public class TMGameState extends AbstractGameState {

    enum TMPhase implements IGamePhase {
        CorporationSelect,
        Research,
        Actions,
        Production
    }

    // General state info
    GridBoard<TMMapTile> board;
    TMMapTile[] extraTiles;
    Counter[] globalParameters;
    Bonus[] bonuses;
    Deck<TMCard> projectCards, corpCards, discardCards;  // Face-down decks

    // Effects and actions played
    HashSet<AbstractAction>[] playerCardsPlayedEffects;
    HashSet<AbstractAction>[] playerCardsPlayedActions;

    // Player-specific counters
    HashMap<TMTypes.Resource, Counter>[] playerResources;
    HashMap<TMTypes.Resource, Counter>[] playerProduction;
    HashMap<TMTypes.Tag, Counter>[] playerCardsPlayedTags;
    HashMap<TMTypes.CardType, Counter>[] playerCardsPlayedTypes;
    HashMap<TMTypes.Tile, Counter>[] tilesPlaced;

    // Player cards
    Deck<TMCard>[] playerHands;
    Deck<TMCard>[] playerCardChoice;
    TMCard[] playerCorporations;

    // Milestones and awards TODO: components?
    Milestone[] milestones;
    Award[] awards;

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
            this.addAll(Arrays.asList(globalParameters));
            this.addAll(Arrays.asList(playerHands));
            this.addAll(Arrays.asList(playerCardChoice));
//            this.addAll(Arrays.asList(playerCorporations));
            for (int i = 0; i < getNPlayers(); i++) {
                addAll(playerResources[i].values());
                addAll(playerProduction[i].values());
                addAll(playerCardsPlayedTags[i].values());
                addAll(tilesPlaced[i].values());
                addAll(playerCardsPlayedTypes[i].values());
            }
        }};
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

    public Deck<TMCard>[] getPlayerHands() {
        return playerHands;
    }

    public HashMap<TMTypes.Tag, Counter>[] getPlayerCardsPlayedTags() {
        return playerCardsPlayedTags;
    }

    public HashMap<TMTypes.CardType, Counter>[] getPlayerCardsPlayedTypes() {
        return playerCardsPlayedTypes;
    }

    public HashSet<AbstractAction>[] getPlayerCardsPlayedActions() {
        return playerCardsPlayedActions;
    }

    public HashSet<AbstractAction>[] getPlayerCardsPlayedEffects() {
        return playerCardsPlayedEffects;
    }

    public HashMap<TMTypes.Tile, Counter>[] getTilesPlaced() {
        return tilesPlaced;
    }

    public Milestone[] getMilestones() {
        return milestones;
    }

    public Award[] getAwards() {
        return awards;
    }

    public TMCard[] getPlayerCorporations() {
        return playerCorporations;
    }

    public Deck<TMCard>[] getPlayerCardChoice() {
        return playerCardChoice;
    }

    public boolean canPlayerPay(int amount) {
        // TODO: "use resource as MC effects"
        return playerResources[getCurrentPlayer()].get(TMTypes.Resource.MegaCredit).getValue() >= amount;
    }

    public Deck<TMCard> getDiscardCards() {
        return discardCards;
    }
}
