package games.terraformingmars;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.actions.ModifyCounter;
import core.components.Counter;
import core.components.Deck;
import core.components.GridBoard;
import games.terraformingmars.actions.BuyCard;
import games.terraformingmars.actions.DiscardCard;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.Award;
import games.terraformingmars.rules.Bonus;
import games.terraformingmars.actions.PlaceTile;
import games.terraformingmars.actions.PlaceholderModifyCounter;
import games.terraformingmars.components.TMMapTile;
import games.terraformingmars.rules.Milestone;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Utils;
import utilities.Vector2D;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static games.terraformingmars.TMGameState.TMPhase.*;
import static games.terraformingmars.TMGameState.stringToGPCounter;
import static games.terraformingmars.TMTypes.neighbor_directions;

public class TMForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        TMGameState gs = (TMGameState) firstState;
        TMGameParameters params = (TMGameParameters) firstState.getGameParameters();
        Random rnd = new Random(params.getRandomSeed());

        gs.globalParameters = new Counter[3];  // hardcoded, read from json/params
        gs.globalParameters[0] = new Counter(params.temperatureScales, "temperature");
        gs.globalParameters[1] = new Counter(params.oxygenScales, "oxygen");
        gs.globalParameters[2] = new Counter(params.nOceanTiles, 0, params.nOceanTiles, "oceanTiles");

        gs.playerResources = new HashMap[gs.getNPlayers()];
        gs.playerProduction = new HashMap[gs.getNPlayers()];

        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.playerResources[i] = new HashMap<>();
            gs.playerProduction[i] = new HashMap<>();
            for (TMTypes.Resource res: TMTypes.Resource.values()) {
                gs.playerResources[i].put(res, new Counter(params.startingResources.get(res), 0, params.maxPoints, res.toString() + "-" + i));
                gs.playerProduction[i].put(res, new Counter(params.startingProduction.get(res), params.minimumProduction.get(res), params.maxPoints, res.toString() + "-prod-" + i));
            }
        }

        gs.projectCards = new Deck<>("Projects", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        gs.corpCards = new Deck<>("Corporations", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        gs.discardCards = new Deck<>("Discard", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);

        loadCards(params.projectsPath, gs.projectCards);
        loadCards(params.corpsPath, gs.corpCards);
        loadBoard(gs);

        HashMap<TMTypes.Tag, Counter>[] playerCardsPlayedTags;
        HashSet<AbstractAction>[] playerCardsPlayedEffects;
        HashSet<AbstractAction>[] playerCardsPlayedActions;
        HashMap<TMTypes.CardType, Counter>[] playerCardsPlayedTypes;
        HashMap<TMTypes.Tile, Counter>[] tilesPlaced;

        gs.playerCorporations = new TMCard[gs.getNPlayers()];
        gs.playerCardChoice = new Deck[gs.getNPlayers()];
        gs.playerHands = new Deck[gs.getNPlayers()];
        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.playerHands[i] = new Deck<>("Hand Player " + i, i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
            gs.playerCardChoice[i] = new Deck<>("Card Choice Player " + i, i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        }

        gs.tilesPlaced = new HashMap[gs.getNPlayers()];
        gs.playerCardsPlayedTypes = new HashMap[gs.getNPlayers()];
        gs.playerCardsPlayedTags = new HashMap[gs.getNPlayers()];
        gs.playerCardsPlayedActions = new HashSet[gs.getNPlayers()];
        gs.playerCardsPlayedEffects = new HashSet[gs.getNPlayers()];
        for (int i = 0; i < gs.getNPlayers(); i++) {
            gs.tilesPlaced[i] = new HashMap<>();
            for (TMTypes.Tile t: TMTypes.Tile.values()) {
                gs.tilesPlaced[i].put(t, new Counter(0, 0, params.maxPoints, t.name() + " tiles placed player " + i));
            }
            gs.playerCardsPlayedTypes[i] = new HashMap<>();
            for (TMTypes.CardType t: TMTypes.CardType.values()) {
                gs.playerCardsPlayedTypes[i].put(t, new Counter(0,0,params.maxPoints,t.name() + " cards played player " + i));
            }
            gs.playerCardsPlayedTags[i] = new HashMap<>();
            for (TMTypes.Tag t: TMTypes.Tag.values()) {
                gs.playerCardsPlayedTags[i].put(t, new Counter(0, 0, params.maxPoints, t.name() + " cards played player " + i));
            }
            gs.playerCardsPlayedActions[i] = new HashSet<>();
            gs.playerCardsPlayedEffects[i] = new HashSet<>();
        }

        // First thing to do is select corporations
        gs.setGamePhase(CorporationSelect);
        for (int i = 0; i < gs.getNPlayers(); i++) {
            for (int j = 0; j < params.nCorpChoiceStart; j++) {
                gs.playerCardChoice[i].add(gs.corpCards.pick(rnd));
            }
        }
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        action.execute(currentState);
        currentState.getTurnOrder().endPlayerTurn(currentState);
        TMGameState gs = (TMGameState)currentState;
        TMGameParameters params = (TMGameParameters) gs.getGameParameters();
        Random rnd = new Random(params.getRandomSeed());

        // Test
        gs.milestones[2].claim(gs);

        if (gs.getGamePhase() == CorporationSelect) {
            boolean allChosen = true;
            for (TMCard card: gs.getPlayerCorporations()) {
                if (card == null) {
                    allChosen = false;
                    break;
                }
            }
            if (allChosen) {
                gs.setGamePhase(Research);
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    for (int j = 0; j < params.nProjectsStart; j++) {
                        gs.playerCardChoice[i].add(gs.projectCards.pick(rnd));
                    }
                }
            }
        } else if (gs.getGamePhase() == Research) {
            // Check if finished: no ore cards in card choice decks
            boolean allDone = true;
            for (Deck<TMCard> deck: gs.getPlayerCardChoice()) {
                if (deck.getSize() > 0) {
                    allDone = false;
                }
            }
            if (allDone) {
                gs.setGamePhase(Actions);
            }
        } else if (gs.getGamePhase() == Actions) {
            // TODO Check if finished: all players passed
            // TODO production
            gs.setGamePhase(Research);
            for (int i = 0; i < gs.getNPlayers(); i++) {
                for (int j = 0; j < params.nProjectsResearch; j++) {
                    gs.playerCardChoice[i].add(gs.projectCards.pick(rnd));
                }
            }
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        // play a card (if valid), standard projects, claim milestone, fund award, card actions, 8 plants -> greenery, 8 heat -> temperature, pass
        // event cards are face-down after played, tags don't apply!
        ArrayList<AbstractAction> actions = new ArrayList<>();
        TMGameState gs = (TMGameState)gameState;

        if (gs.getGamePhase() == CorporationSelect || gs.getGamePhase() == Research) {
            // Decide one at a time, alternating
            actions.add(new BuyCard(0));
            if (gs.getPlayerCardChoice()[gs.getCurrentPlayer()].get(0).cardType != TMTypes.CardType.Corporation) {
                actions.add(new DiscardCard(0));
            }
        } else {
            actions.add(new DoNothing());
            for (int i = 0; i < gs.board.getHeight(); i++) {
                for (int j = 0; j < gs.board.getWidth(); j++) {
                    TMMapTile mt = gs.board.getElement(j, i);
                    if (mt != null) {
                        if (mt.getTileType() == TMTypes.MapTileType.Ground && mt.getTilePlaced() == null) {
                            actions.add(new PlaceTile(j, i, TMTypes.Tile.Greenery));
                        }
                    }
                }
            }
        }

        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new TMForwardModel();
    }

    List<Vector2D> getNeighbours(Vector2D cell) {
        ArrayList<Vector2D> neighbors = new ArrayList<>();
        int parity = cell.getY() % 2;
        for (Vector2D v: neighbor_directions[parity]) {
            neighbors.add(cell.add(v));
        }
        return neighbors;
    }

    private void loadBoard(TMGameState gs) {
        TMGameParameters params = (TMGameParameters) gs.getGameParameters();

        gs.board = new GridBoard<>(params.boardSize, params.boardSize);

        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader(params.boardPath)) {
            JSONObject data = (JSONObject) jsonParser.parse(reader);

            // Process main map
            JSONArray b = (JSONArray) data.get("board");
            int y = 0;
            for (Object g : b) {
                JSONArray row = (JSONArray) g;
                int x = 0;
                for (Object o1 : row) {
                    gs.board.setElement(x, y, stringToMapTile((String) o1));
                    x++;
                }
                y++;
            }

            // Process extra tiles not on regular board
            JSONArray extra = (JSONArray) data.get("extra");
            gs.extraTiles = new TMMapTile[extra.size()];
            for (int i = 0; i < extra.size(); i++) {
                gs.extraTiles[i] = stringToMapTile((String)extra.get(i));
            }

            // Process bonuses for this game when counters reach specific points
            JSONArray bonus = (JSONArray) data.get("bonus");
            gs.bonuses = new Bonus[bonus.size()];
            for (int i = 0; i < bonus.size(); i++) {
                gs.bonuses[i] = stringToBonus(gs, (String)bonus.get(i));
            }

            // Process milestones and awards
            JSONArray milestones = (JSONArray) data.get("milestones");
            gs.milestones = new Milestone[milestones.size()];
            for (int i = 0; i < milestones.size(); i++) {
                String[] split = ((String)milestones.get(i)).split(":");
                gs.milestones[i] = new Milestone(split[0], Integer.parseInt(split[2]), split[1]);
            }
            JSONArray awards = (JSONArray) data.get("awards");
            gs.awards = new Award[awards.size()];
            for (int i = 0; i < awards.size(); i++) {
                String[] split = ((String)awards.get(i)).split(":");
                gs.awards[i] = new Award(split[0], split[1]);
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private TMMapTile stringToMapTile(String s) {
        if (s.equals("0")) return null;

        TMMapTile mt = new TMMapTile();

        String[] split = s.split("-");

        // First element is tile type
        TMTypes.MapTileType type = null;
        try{
            type = TMTypes.MapTileType.valueOf(split[0]);
        } catch(Exception ignored) {}
        if (type == null) {
            type = TMTypes.MapTileType.City;
            mt.setComponentName(split[0]); // Keep city name
        }
        mt.setType(type);

        // The rest are resources existing here
        int nResources = split.length-1;
        TMTypes.Resource[] resources = new TMTypes.Resource[nResources];
        for (int i = 1; i < split.length; i++) {
            resources[i-1] = TMTypes.Resource.valueOf(split[i]);
        }
        mt.setResources(resources);

        return mt;
    }

    private Bonus stringToBonus(TMGameState gs, String s) {
        /*
        Bonus options implemented:
            - Increase/Decrease counter (global parameter, player resource, or player production)
            - Place ocean tile
         */
        String[] split = s.split(":");

        // First element is the counter
        Counter c = stringToGPCounter(gs, split[0]);

        // Second element is threshold, int
        int threshold = Integer.parseInt(split[1]);

        // Third element is effect
        AbstractAction effect = null;
        String effectString = "";

        if (split[2].contains("inc") || split[2].contains("dec")) {
            // Increase/Decrease counter action
            String[] split2 = split[2].split("-");
            // Find how much
            int increment = Integer.parseInt(split2[2]);
            if (split[2].contains("dec")) increment *= -1;

            effectString = split2[1];

            // Find which counter
            Counter which = stringToGPCounter(gs, split2[1]);

            if (which == null) {
                // A resource or production instead
                String resString = split2[1].split("prod")[0];
                TMTypes.Resource res = TMTypes.Resource.valueOf(resString);
                effect = new PlaceholderModifyCounter(increment, res, split2[1].contains("prod"));
            } else {
                // A global counter (temp, oxygen, oceantiles)
                effect = new ModifyCounter(which.getComponentID(), increment);
            }
        } else if (split[2].contains("placetile")) {
            // equals("placetile:ocean:ocean")
            // PlaceTile action
            String[] split2 = split[2].split("/");
            // split2[1] is type of tile to place
            TMTypes.Tile toPlace = Utils.searchEnum(TMTypes.Tile.class, split2[1]);
            // split2[2] is where to place it. can be a map tile, or a city name.
            TMTypes.MapTileType where = Utils.searchEnum(TMTypes.MapTileType.class, split2[2]);
            HashSet<Vector2D> legalPositions = new HashSet<>();
            for (int i = 0; i < gs.board.getHeight(); i++) {
                for (int j = 0; j < gs.board.getWidth(); j++) {
                    TMMapTile mt = gs.board.getElement(j, i);
                    if (mt != null) {
                        if (where != null && mt.getTileType() == where || where == null && mt.getComponentName().equalsIgnoreCase(split2[2])) {
                            legalPositions.add(new Vector2D(j, i));
                        }
                    }
                }
            }
            effect = new PlaceTile(toPlace, legalPositions);  // Extended sequence, will ask player where to put it
            effectString = split2[1];
        }
        if (c != null) {
            return new Bonus(c.getComponentID(), threshold, effect, effectString);
        }
        return null;
    }

    private void loadCards(String path, Deck<TMCard> deck) {
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader(path)) {
            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for (Object o: data) {
                TMCard card;
                if (deck.getComponentName().equalsIgnoreCase("corporations")) {
                    card = TMCard.loadCorporationHTML((JSONObject)o);
                } else {
                    card = TMCard.loadCardHTML((JSONObject) o);
                }  // TODO: other types of cards
                deck.add(card);
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
