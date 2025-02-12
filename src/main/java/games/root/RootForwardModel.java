package games.root;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.*;
import games.root.actions.EndTurn;
import games.root.components.cards.EyrieRulers;
import games.root.components.cards.RootCard;
import games.root.components.cards.RootQuestCard;
import games.root.components.*;

import java.util.*;
import java.util.stream.IntStream;

import static core.CoreConstants.GameResult.*;
import static games.root.RootParameters.VictoryCondition.Score;

/**
 * <p>The forward model contains all the game rules and logic. It is mainly responsible for declaring rules for:</p>
 * <ol>
 *     <li>Game setup</li>
 *     <li>Actions available to players in a given game state</li>
 *     <li>Game events or rules applied after a player's action</li>
 *     <li>Game end</li>
 * </ol>
 */
public class RootForwardModel extends StandardForwardModel {

    /**
     * Initializes all variables in the given game state. Performs initial game setup according to game rules, e.g.:
     * <ul>
     *     <li>Sets up decks of cards and shuffles them</li>
     *     <li>Gives player cards</li>
     *     <li>Places tokens on boards</li>
     *     <li>...</li>
     * </ul>
     *
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {
        RootGameState state = (RootGameState) firstState;
        RootParameters rp = (RootParameters) firstState.getGameParameters();
        Arrays.fill(state.playerVictoryConditions, Score);
        Arrays.fill(state.playerScores,0);
        state.gameMap = createRootNodeGraph(true);
        state.mapType = RootParameters.MapType.Summer;
        state.actionsPlayed = 0;
        state.playerSubGamePhase = 0;
        state.playersSetUp = 0;
        state.setGamePhase(RootGameState.RootGamePhase.Setup);
        //Create Root Draw and Discard Card Deck
        Deck<RootCard> cards = new Deck<>("Draw Pile", -1, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        addCards(cards, rp);
        //System.out.println("Deck initialized with " + cards.getSize() + " cards");
        Deck<RootCard> discardCards = new Deck<>("Discard Pile", -1, CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        cards.shuffle(0, cards.getSize(), state.getRnd());
        state.drawPile = cards;
        state.discardPile = discardCards;
        //Create player Hands and draw initial cards
        state.playerDecks = new ArrayList<>();
        state.playerFactions = new ArrayList<>();
        state.playerCraftedCards = new ArrayList<>();
        state.craftedItems = new ArrayList<>();
        state.startingItems = new ArrayList<>();
        state.ruinItems = new ArrayList<>();
        state.craftableItems = new ArrayList<>();
        for (Item.ItemType itemType : rp.craftableItems.keySet()) {
            for (int i = 0; i < rp.craftableItems.get(itemType); i++) {
                state.craftableItems.add(new Item(CoreConstants.ComponentType.TOKEN, itemType));
            }
        }
        for (int counter = 0; counter < state.getNPlayers(); counter++) {
            boolean[] visibility = new boolean[state.getNPlayers()];
            visibility[counter] = true;
            PartialObservableDeck<RootCard> playerCards = new PartialObservableDeck<>("Player " + counter + " Hand", counter, visibility);
            for (int e = 0; e < rp.handSize; e++) {
                playerCards.add(cards.draw());
            }
            state.playerDecks.add(playerCards);
            state.playerFactions.add(rp.getPlayerFaction(counter));
            state.playerCraftedCards.add(new Deck<>("Player" + counter + "Crafted Cards", CoreConstants.VisibilityMode.VISIBLE_TO_ALL));
            state.craftedItems.add(new ArrayList<>());
        }
        //Create player pieces
        for (int counter = 0; counter < state.getNPlayers(); counter++) {
            RootParameters.Factions playerRole = state.getPlayerFaction(counter);
            if (playerRole.equals(RootParameters.Factions.MarquiseDeCat)) {
                createCatPieces(state, rp);
            } else if (playerRole.equals(RootParameters.Factions.EyrieDynasties)) {
                createBirdPieces(state, rp, counter);
            } else if (playerRole.equals(RootParameters.Factions.WoodlandAlliance)) {
                createWoodlandAlliancePieces(state, rp, counter);
            } else if (playerRole.equals(RootParameters.Factions.Vagabond)) {
                createVagabondPieces(state, rp);
            } else {
                System.out.println("Player faction not assigned");
            }
        }
    }

    protected void addCards(Deck<RootCard> cards, RootParameters rp) {
        for (HashMap.Entry<RootCard.CardType, Integer> entry : rp.birdCards.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {

                RootCard card = new RootCard(entry.getKey(), RootParameters.ClearingTypes.Bird);
                cards.add(card);
            }
        }
        for (HashMap.Entry<RootCard.CardType, Integer> entry : rp.mouseCards.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {

                RootCard card = new RootCard(entry.getKey(), RootParameters.ClearingTypes.Mouse);
                cards.add(card);
            }
        }
        for (HashMap.Entry<RootCard.CardType, Integer> entry : rp.rabbitCards.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {

                RootCard card = new RootCard(entry.getKey(), RootParameters.ClearingTypes.Rabbit);
                cards.add(card);
            }
        }
        for (HashMap.Entry<RootCard.CardType, Integer> entry : rp.foxCards.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {

                RootCard card = new RootCard(entry.getKey(), RootParameters.ClearingTypes.Fox);
                cards.add(card);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected RootGraphBoard createRootNodeGraph(boolean summerMap) {
        if (summerMap) {
            RootGraphBoard gameMap = new RootGraphBoard();
            int multiplier = 3;
            RootBoardNodeWithRootEdges node1 = new RootBoardNodeWithRootEdges(true, "Top Left", RootParameters.ClearingTypes.Fox, 1);
            RootBoardNodeWithRootEdges node2 = new RootBoardNodeWithRootEdges(false, "Top", RootParameters.ClearingTypes.Rabbit, 2);
            RootBoardNodeWithRootEdges node3 = new RootBoardNodeWithRootEdges(true, "Top Right", RootParameters.ClearingTypes.Mouse, 2);
            RootBoardNodeWithRootEdges node4 = new RootBoardNodeWithRootEdges(false, "Left", RootParameters.ClearingTypes.Mouse, 2);
            RootBoardNodeWithRootEdges node5 = new RootBoardNodeWithRootEdges(false, "Middle", RootParameters.ClearingTypes.Rabbit, 2);
            node5.build(RootParameters.BuildingType.Ruins);
            RootBoardNodeWithRootEdges node6 = new RootBoardNodeWithRootEdges(false, "Middle Two", RootParameters.ClearingTypes.Fox, 2);
            node6.build(RootParameters.BuildingType.Ruins);
            RootBoardNodeWithRootEdges node7 = new RootBoardNodeWithRootEdges(false, "Middle Three", RootParameters.ClearingTypes.Mouse, 3);
            node7.build(RootParameters.BuildingType.Ruins);
            RootBoardNodeWithRootEdges node8 = new RootBoardNodeWithRootEdges(false, "Right", RootParameters.ClearingTypes.Fox, 2);
            node8.build(RootParameters.BuildingType.Ruins
            );
            RootBoardNodeWithRootEdges node9 = new RootBoardNodeWithRootEdges(true, "Bottom Left", RootParameters.ClearingTypes.Rabbit, 1);
            RootBoardNodeWithRootEdges node10 = new RootBoardNodeWithRootEdges(false, "Bottom", RootParameters.ClearingTypes.Fox, 2);
            RootBoardNodeWithRootEdges node11 = new RootBoardNodeWithRootEdges(false, "Bottom Two", RootParameters.ClearingTypes.Mouse, 2);
            RootBoardNodeWithRootEdges node12 = new RootBoardNodeWithRootEdges(true, "Bottom Right", RootParameters.ClearingTypes.Rabbit, 1);
            RootBoardNodeWithRootEdges nodeForrest1 = new RootBoardNodeWithRootEdges(false, "Forrest One", RootParameters.ClearingTypes.Forrest, 0);
            RootBoardNodeWithRootEdges nodeForrest2 = new RootBoardNodeWithRootEdges(false, "Forrest Two", RootParameters.ClearingTypes.Forrest, 0);
            RootBoardNodeWithRootEdges nodeForrest3 = new RootBoardNodeWithRootEdges(false, "Forrest Three", RootParameters.ClearingTypes.Forrest, 0);
            RootBoardNodeWithRootEdges nodeForrest4 = new RootBoardNodeWithRootEdges(false, "Forrest Four", RootParameters.ClearingTypes.Forrest, 0);
            RootBoardNodeWithRootEdges nodeForrest5 = new RootBoardNodeWithRootEdges(false, "Forrest Five", RootParameters.ClearingTypes.Forrest, 0);
            RootBoardNodeWithRootEdges nodeForrest6 = new RootBoardNodeWithRootEdges(false, "Forrest Six", RootParameters.ClearingTypes.Forrest, 0);
            RootBoardNodeWithRootEdges nodeForrest7 = new RootBoardNodeWithRootEdges(false, "Forrest Seven", RootParameters.ClearingTypes.Forrest, 0);
            RootBoardNodeWithRootEdges nodeForrest8 = new RootBoardNodeWithRootEdges(false, "Forrest Eight", RootParameters.ClearingTypes.Forrest, 0);
            RootBoardNodeWithRootEdges nodeForrest9 = new RootBoardNodeWithRootEdges(false, "Forrest Nine", RootParameters.ClearingTypes.Forrest, 0);

            node1.setXY(30 * multiplier, 30 * multiplier);
            node2.setXY(140 * multiplier, 20 * multiplier);
            node3.setXY(233 * multiplier, 50 * multiplier);
            node4.setXY(33 * multiplier, 100 * multiplier);
            node5.setXY(110 * multiplier, 70 * multiplier);
            node6.setXY(80 * multiplier, 135 * multiplier);
            node7.setXY(165 * multiplier, 120 * multiplier);
            node8.setXY(245 * multiplier, 125 * multiplier);
            node9.setXY(30 * multiplier, 200 * multiplier);
            node10.setXY(100 * multiplier, 205 * multiplier);
            node11.setXY(150 * multiplier, 185 * multiplier);
            node12.setXY(215 * multiplier, 200 * multiplier);
            nodeForrest1.setXY(100 * multiplier, 40 * multiplier);
            nodeForrest2.setXY(160 * multiplier, 45 * multiplier);
            nodeForrest3.setXY(70 * multiplier, 80 * multiplier);
            nodeForrest4.setXY(130 * multiplier, 100 * multiplier);
            nodeForrest5.setXY(200 * multiplier, 90 * multiplier);
            nodeForrest6.setXY(45 * multiplier, 135 * multiplier);
            nodeForrest7.setXY(150 * multiplier, 150 * multiplier);
            nodeForrest8.setXY(210 * multiplier, 145 * multiplier);
            nodeForrest9.setXY(90 * multiplier, 170 * multiplier);

            // Add nodes to the game map
            gameMap.addBoardNode(node1);
            gameMap.addBoardNode(node2);
            gameMap.addBoardNode(node3);
            gameMap.addBoardNode(node4);
            gameMap.addBoardNode(node5);
            gameMap.addBoardNode(node6);
            gameMap.addBoardNode(node7);
            gameMap.addBoardNode(node8);
            gameMap.addBoardNode(node9);
            gameMap.addBoardNode(node10);
            gameMap.addBoardNode(node11);
            gameMap.addBoardNode(node12);
            gameMap.addBoardNode(nodeForrest1);
            gameMap.addBoardNode(nodeForrest2);
            gameMap.addBoardNode(nodeForrest3);
            gameMap.addBoardNode(nodeForrest4);
            gameMap.addBoardNode(nodeForrest5);
            gameMap.addBoardNode(nodeForrest6);
            gameMap.addBoardNode(nodeForrest7);
            gameMap.addBoardNode(nodeForrest8);
            gameMap.addBoardNode(nodeForrest9);

            // Create edges to fully connect the nodes
            gameMap.addConnection(node1.getComponentID(), node2.getComponentID());
            gameMap.addConnection(node1.getComponentID(), node5.getComponentID());
            gameMap.addConnection(node1.getComponentID(), node4.getComponentID());
            gameMap.addConnection(node2.getComponentID(), node3.getComponentID());
            gameMap.addConnection(node3.getComponentID(), node5.getComponentID());
            gameMap.addConnection(node3.getComponentID(), node8.getComponentID());
            gameMap.addConnection(node4.getComponentID(), node6.getComponentID());
            gameMap.addConnection(node4.getComponentID(), node9.getComponentID());
            gameMap.addConnection(node5.getComponentID(), node6.getComponentID());
            gameMap.addConnection(node6.getComponentID(), node7.getComponentID());
            gameMap.addConnection(node6.getComponentID(), node9.getComponentID());
            gameMap.addConnection(node6.getComponentID(), node11.getComponentID());
            gameMap.addConnection(node7.getComponentID(), node8.getComponentID());
            gameMap.addConnection(node7.getComponentID(), node12.getComponentID());
            gameMap.addConnection(node8.getComponentID(), node12.getComponentID());
            gameMap.addConnection(node9.getComponentID(), node10.getComponentID());
            gameMap.addConnection(node10.getComponentID(), node11.getComponentID());
            gameMap.addConnection(node11.getComponentID(), node12.getComponentID());
            gameMap.addConnection(node1.getComponentID(), nodeForrest1.getComponentID());
            gameMap.addConnection(node2.getComponentID(), nodeForrest1.getComponentID());
            gameMap.addConnection(node5.getComponentID(), nodeForrest1.getComponentID());
            gameMap.addConnection(node2.getComponentID(), nodeForrest2.getComponentID());
            gameMap.addConnection(node3.getComponentID(), nodeForrest2.getComponentID());
            gameMap.addConnection(node5.getComponentID(), nodeForrest2.getComponentID());
            gameMap.addConnection(node1.getComponentID(), nodeForrest3.getComponentID());
            gameMap.addConnection(node4.getComponentID(), nodeForrest3.getComponentID());
            gameMap.addConnection(node5.getComponentID(), nodeForrest3.getComponentID());
            gameMap.addConnection(node6.getComponentID(), nodeForrest3.getComponentID());
            gameMap.addConnection(node5.getComponentID(), nodeForrest4.getComponentID());
            gameMap.addConnection(node6.getComponentID(), nodeForrest4.getComponentID());
            gameMap.addConnection(node7.getComponentID(), nodeForrest4.getComponentID());
            gameMap.addConnection(node3.getComponentID(), nodeForrest5.getComponentID());
            gameMap.addConnection(node5.getComponentID(), nodeForrest5.getComponentID());
            gameMap.addConnection(node7.getComponentID(), nodeForrest5.getComponentID());
            gameMap.addConnection(node8.getComponentID(), nodeForrest5.getComponentID());
            gameMap.addConnection(node4.getComponentID(), nodeForrest6.getComponentID());
            gameMap.addConnection(node6.getComponentID(), nodeForrest6.getComponentID());
            gameMap.addConnection(node9.getComponentID(), nodeForrest6.getComponentID());
            gameMap.addConnection(node6.getComponentID(), nodeForrest7.getComponentID());
            gameMap.addConnection(node7.getComponentID(), nodeForrest7.getComponentID());
            gameMap.addConnection(node11.getComponentID(), nodeForrest7.getComponentID());
            gameMap.addConnection(node12.getComponentID(), nodeForrest7.getComponentID());
            gameMap.addConnection(node7.getComponentID(), nodeForrest8.getComponentID());
            gameMap.addConnection(node8.getComponentID(), nodeForrest8.getComponentID());
            gameMap.addConnection(node12.getComponentID(), nodeForrest8.getComponentID());
            gameMap.addConnection(node6.getComponentID(), nodeForrest9.getComponentID());
            gameMap.addConnection(node9.getComponentID(), nodeForrest9.getComponentID());
            gameMap.addConnection(node10.getComponentID(), nodeForrest9.getComponentID());
            gameMap.addConnection(node11.getComponentID(), nodeForrest9.getComponentID());
            gameMap.addConnection(nodeForrest1.getComponentID(), nodeForrest2.getComponentID());
            gameMap.addConnection(nodeForrest1.getComponentID(), nodeForrest3.getComponentID());
            gameMap.addConnection(nodeForrest2.getComponentID(), nodeForrest5.getComponentID());
            gameMap.addConnection(nodeForrest3.getComponentID(), nodeForrest4.getComponentID());
            gameMap.addConnection(nodeForrest3.getComponentID(), nodeForrest6.getComponentID());
            gameMap.addConnection(nodeForrest4.getComponentID(), nodeForrest5.getComponentID());
            gameMap.addConnection(nodeForrest4.getComponentID(), nodeForrest7.getComponentID());
            gameMap.addConnection(nodeForrest5.getComponentID(), nodeForrest8.getComponentID());
            gameMap.addConnection(nodeForrest6.getComponentID(), nodeForrest9.getComponentID());
            gameMap.addConnection(nodeForrest7.getComponentID(), nodeForrest8.getComponentID());
            gameMap.addConnection(nodeForrest7.getComponentID(), nodeForrest9.getComponentID());


            return gameMap;
        } else {
            //TODO: add winter map
            return new RootGraphBoard();
        }
    }

    protected void createCatPieces(RootGameState state, RootParameters rp) {
        //Create Warriors, Tokens, Buildings and any faction specific components
        state.CatWarriors = rp.maxWarriors.get(RootParameters.Factions.MarquiseDeCat);
        state.Wood = rp.maxWood;
        state.Keep = true;
        state.Workshops = rp.buildingCount.get(RootParameters.BuildingType.Workshop);
        state.Sawmills = rp.buildingCount.get(RootParameters.BuildingType.Sawmill);
        state.Recruiters = rp.buildingCount.get(RootParameters.BuildingType.Sawmill);
    }

    protected void createBirdPieces(RootGameState state, RootParameters rp, int playerID) {
        //Create Warriors
        state.eyrieWarriors = rp.maxWarriors.get(RootParameters.Factions.EyrieDynasties);
        state.eyrieDecree = new ArrayList<>();
        state.playedSuits = new ArrayList<>();
        for (int e = 0; e < 4; e++) {
            state.eyrieDecree.add(new Deck<>(rp.decreeInitializer.get((Integer) e).toString() + "decree", playerID, CoreConstants.VisibilityMode.VISIBLE_TO_ALL));
        }
        state.roosts = rp.buildingCount.get(RootParameters.BuildingType.Roost);
        state.rulers = new Deck<>("Player " + playerID + "Rulers", playerID, CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        state.activeRuler = null;
        for (HashMap.Entry<EyrieRulers.CardType, Boolean[]> entry : rp.eyrieRulers.entrySet()) {
            EyrieRulers ruler = new EyrieRulers(entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2], entry.getValue()[3]);
            state.addRulerToRulers(ruler);
        }
        state.viziers = new Deck<>("Player " + playerID + "viziers", playerID, CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        for (int i = 0; i < rp.maxViziers; i++) {
            RootCard vizier = new RootCard(RootCard.CardType.Vizier,  RootParameters.ClearingTypes.Bird);
            state.viziers.add(vizier);
        }
    }

    protected void createWoodlandAlliancePieces(RootGameState state, RootParameters rp, int playerID) {
        state.woodlandWarriors = rp.maxWarriors.get(RootParameters.Factions.WoodlandAlliance);
        state.foxBase = 1;
        state.mouseBase = 1;
        state.rabbitBase = 1;
        state.officers = 0;
        state.sympathyTokens = rp.sympathyTokens;
        boolean[] visibility = new boolean[state.getNPlayers()];
        visibility[playerID] = true;
        state.supporters = new PartialObservableDeck<>("Player " + playerID + " Supporters", playerID, visibility);
    }

    protected void createVagabondPieces(RootGameState state, RootParameters rp) {
        state.vagabond = rp.maxWarriors.get(RootParameters.Factions.Vagabond);
        state.questDrawPile = new Deck<>("Quest Draw Pile", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        state.activeQuests = new Deck<>("Active Quests", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        for (Map.Entry<RootQuestCard.CardType, RootParameters.ClearingTypes[]> entry : rp.questCardInitializer.entrySet()) {
            for (RootParameters.ClearingTypes clearingType: entry.getValue()){
                state.questDrawPile.add(new RootQuestCard(entry.getKey(), clearingType));
            }
        }
        state.questDrawPile.shuffle(0, state.questDrawPile.getSize(), state.getRnd());
        for (int i = 0; i < 3; i++){
            state.activeQuests.add(state.questDrawPile.draw());
        }

        state.ruinItems = new ArrayList<>();
        for (Item.ItemType itemType : rp.ruinItems.keySet()) {
            for (int i = 0; i < rp.ruinItems.get(itemType); i++) {
                state.ruinItems.add(new Item(CoreConstants.ComponentType.TOKEN, itemType));
            }
        }
        state.startingItems = new ArrayList<>();
        for (Item.ItemType itemType : rp.startingItems.keySet()) {
            for (int i = 0; i < rp.startingItems.get(itemType); i++) {
                state.startingItems.add(new Item(CoreConstants.ComponentType.TOKEN, itemType));
            }
        }
        state.satchel = new ArrayList<>();
        state.bags = new ArrayList<>();
        state.coins = new ArrayList<>();
        state.teas = new ArrayList<>();
        state.foxQuests = 0;
        state.mouseQuests = 0;
        state.rabbitQuests = 0;

        state.relationships = new HashMap<>(){{
            put(RootParameters.Factions.MarquiseDeCat, RootParameters.Relationship.Neutral);
            put(RootParameters.Factions.EyrieDynasties, RootParameters.Relationship.Neutral);
            put(RootParameters.Factions.WoodlandAlliance, RootParameters.Relationship.Neutral);
        }};

        state.aidNumbers = new HashMap<>(){{
            put(RootParameters.Factions.MarquiseDeCat, 0);
            put(RootParameters.Factions.EyrieDynasties, 0);
            put(RootParameters.Factions.WoodlandAlliance, 0);
        }};

    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        if (gameState.getGamePhase() == RootGameState.RootGamePhase.Setup) {
            return RootActionFactory.getSetupActions(gameState);
        } else if (gameState.getGamePhase() == RootGameState.RootGamePhase.Birdsong) {
            return RootActionFactory.getBirdsongActions(gameState);
        } else if (gameState.getGamePhase() == RootGameState.RootGamePhase.Daylight) {
            return RootActionFactory.getDaylightActions(gameState);
        } else if (gameState.getGamePhase() == RootGameState.RootGamePhase.Evening) {
            return RootActionFactory.getEveningActions(gameState);
        } else {
            System.out.println("NO AVAILABLE ACTIONS!!!");
            return actions;
        }
    }

    @Override
    protected void _afterAction(AbstractGameState gs, AbstractAction action) {
        //if (gs.getTurnCounter() % 50 == 0) System.out.println(gs.getTurnCounter() + ": " + gs.getGameScore(0) + " " + gs.getGameScore(1) + gs.getGameScore(2) + " " + gs.getGameScore(3));
        RootGameState state = (RootGameState) gs;
        //1st Recalculate Clearing rulers -> based on number of buildings/warriors of each player
        state.getGameMap().updateRulers();
        if (state.isActionInProgress()) return;

        if (state.scoreGameOver() || state.getTurnCounter() > 200) {
            endGame(state);
        }
        //For End turn action -> handle GamePhase changes and current player changes
        if (action instanceof EndTurn) {
            int nextPlayerID = (state.getCurrentPlayer() + 1) % state.getNPlayers();
            RootGameState.RootGamePhase phase = (RootGameState.RootGamePhase) state.getGamePhase();
            //System.out.println(phase.toString());
            switch (phase) {
                case Setup:
                    if (state.playersSetUp == state.getNPlayers()) {
                        state.setGamePhase(RootGameState.RootGamePhase.Birdsong);
                    }
                    break;
                case Evening:
                    state.setGamePhase(RootGameState.RootGamePhase.Birdsong);
                    break;
                default:
                    System.out.println("Something went wrong"); // should not be possible to end turn outside of Evening/Setup game phase
                    break;
            }
            state.setActionsPlayed(0);
            state.setPlayerSubGamePhase(0);
            endPlayerTurn(state, nextPlayerID);
            if (state.getPlayerVictoryCondition(state.getCurrentPlayer()) != Score){
                checkDominanceVictory(state, nextPlayerID); // when entering birdsong -> players with dominance victory check their win condition
            }
        }
    }

    private void checkDominanceVictory(RootGameState gs, int playerID){
        switch (gs.getPlayerVictoryCondition(playerID)){
            case DF:
                int ruledFoxClearings = 0;
                for (RootBoardNodeWithRootEdges clearing: gs.getGameMap().getNonForrestBoardNodes()){
                    if (clearing.getClearingType() == RootParameters.ClearingTypes.Fox && clearing.rulerID == playerID){
                        ruledFoxClearings++;
                    }
                }
                if (ruledFoxClearings >= 3){
                    gs.setGameStatus(CoreConstants.GameResult.GAME_END);
                    gs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, playerID);
                    for (int i = 0 ; i < gs.getNPlayers(); i++){
                        if (i!= playerID){
                            gs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, i);
                        }
                    }
                }
                break;
            case DM:
                int ruledMouseClearings = 0;
                for (RootBoardNodeWithRootEdges clearing: gs.getGameMap().getNonForrestBoardNodes()){
                    if (clearing.getClearingType() == RootParameters.ClearingTypes.Mouse && clearing.rulerID == playerID){
                        ruledMouseClearings++;
                    }
                }
                if (ruledMouseClearings >= 3){
                    gs.setGameStatus(CoreConstants.GameResult.GAME_END);
                    gs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, playerID);
                    for (int i = 0 ; i < gs.getNPlayers(); i++){
                        if (i!= playerID){
                            gs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, i);
                        }
                    }
                }
                break;
            case DR:
                int ruledRabbitClearings = 0;
                for (RootBoardNodeWithRootEdges clearing: gs.getGameMap().getNonForrestBoardNodes()){
                    if (clearing.getClearingType() == RootParameters.ClearingTypes.Rabbit && clearing.rulerID == playerID){
                        ruledRabbitClearings++;
                    }
                }
                if (ruledRabbitClearings >= 3){
                    gs.setGameStatus(CoreConstants.GameResult.GAME_END);
                    gs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, playerID);
                    for (int i = 0 ; i < gs.getNPlayers(); i++){
                        if (i!= playerID){
                            gs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, i);
                        }
                    }
                }
                break;
            case DB:
                boolean rulesTOPLEFTANDBOTTOMRIGHT;
                boolean rulesTOPRIGHTANDBOTTOMLEFT;
                int firstPair = 0;
                int secondPair = 0;
                for (RootBoardNodeWithRootEdges clearing: gs.getGameMap().getNonForrestBoardNodes()){
                    if (clearing.getCorner() && clearing.rulerID == playerID){
                        switch (clearing.identifier) {
                            case "Top Left", "Bottom Right" -> firstPair++;
                            case "Top Right", "Bottom Left" -> secondPair++;
                        }
                    }
                }
                rulesTOPLEFTANDBOTTOMRIGHT = firstPair == 2;
                rulesTOPRIGHTANDBOTTOMLEFT = secondPair == 2;
                if (rulesTOPLEFTANDBOTTOMRIGHT || rulesTOPRIGHTANDBOTTOMLEFT){
                    gs.setGameStatus(CoreConstants.GameResult.GAME_END);
                    gs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, playerID);
                    for (int i = 0 ; i < gs.getNPlayers(); i++){
                        if (i!= playerID){
                            gs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, i);
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void endGame(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if (gs.getGamePhase() == RootGameState.RootGamePhase.Birdsong && gs.getGameStatus() == GAME_END && state.getPlayerVictoryCondition(gs.getCurrentPlayer()) != Score){
            return;
        }
        gs.setGameStatus(CoreConstants.GameResult.GAME_END);
        // If we have more than one person in Ordinal position of 1, then this is a draw
        boolean drawn = IntStream.range(0, gs.getNPlayers()).map(gs::getOrdinalPosition).filter(i -> i == 1).count() > 1;
        for (int p = 0; p < gs.getNPlayers(); p++) {
            int o = gs.getOrdinalPosition(p);
            if (o == 1 && drawn)
                gs.setPlayerResult(DRAW_GAME, p);
            else if (o == 1)
                gs.setPlayerResult(WIN_GAME, p);
            else
                gs.setPlayerResult(LOSE_GAME, p);
        }
        if (gs.getCoreGameParameters().verbose) {
            System.out.println(Arrays.toString(gs.getPlayerResults()));
        }
    }
}
