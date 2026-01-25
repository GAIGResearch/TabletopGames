package games.pandemic;

import core.*;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Area;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.properties.Property;
import core.properties.PropertyLong;
import core.properties.PropertyString;
import core.rules.AbstractRuleBasedForwardModel;
import core.rules.GameOverCondition;
import core.rules.Node;
import core.rules.nodetypes.ConditionNode;
import core.rules.nodetypes.RuleNode;
import games.pandemic.actions.AddResearchStation;
import games.pandemic.actions.InfectCity;
import games.pandemic.rules.conditions.*;
import games.pandemic.rules.gameOver.*;
import games.pandemic.rules.rules.*;
import utilities.Hash;

import java.util.*;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static core.CoreConstants.nameHash;
import static core.CoreConstants.playerHandHash;
import static games.pandemic.PandemicActionFactory.*;
import static games.pandemic.PandemicConstants.*;
import static games.pandemic.actions.MovePlayer.placePlayer;

public class PandemicForwardModel extends AbstractRuleBasedForwardModel {

    /**
     * Constructor. Creates the rules for the game and sets up the game rule graph.
     * @param gameParameters - parameters for the game.
     * @param nPlayers - number of players in the game.
     */
    public PandemicForwardModel(AbstractParameters gameParameters, int nPlayers) {
        PandemicParameters pp = (PandemicParameters) gameParameters;

        // Game over conditions
        GameOverCondition infectLose = new GameOverInfection();
        GameOverCondition outbreakLose = new GameOverOutbreak(pp.loseMaxOutbreak);
        GameOverCondition drawCardsLose = new GameOverDrawCards();
        GameOverCondition infectionCardsLose = new GameOverDrawInfectionCards();
        GameOverCondition win = new GameOverDiseasesCured();

        // Rules
        RuleNode infectCities = new InfectCities(pp.infectionRate, pp.maxCubesPerCity, pp.nCubesInfection);
        RuleNode forceDiscardReaction1 = new ForceDiscardReaction();
        RuleNode forceDiscardReaction2 = new ForceDiscardReaction();
        RuleNode epidemic2 = new EpidemicIntensify();
        RuleNode forceRPreaction = new ForceRPReaction();
        RuleNode epidemic1 = new EpidemicInfect(pp.maxCubesPerCity, pp.nCubesEpidemic);
        RuleNode drawCards = new DrawCards();
        RuleNode playerAction = new PlayerAction(pp.nInitialDiseaseCubes);
        RuleNode playerActionInterrupt1 = new PlayerAction(pp.nInitialDiseaseCubes);
        RuleNode playerActionInterrupt2 = new PlayerAction(pp.nInitialDiseaseCubes);
        RuleNode playerActionInterrupt3 = new PlayerAction(pp.nInitialDiseaseCubes);
        RuleNode nextPlayerRule = new NextPlayer();

        // Conditions
        ConditionNode playerHandOverCapacity1 = new PlayerHandOverCapacity();
        ConditionNode playerHandOverCapacity2 = new PlayerHandOverCapacity();
        ConditionNode playerHasRPCard = new HasRPCard();
        ConditionNode enoughDraws = new EnoughDraws(pp.nCardsDraw);
        ConditionNode firstEpidemic = new IsEpidemic();
        ConditionNode enoughActions = new ActionsPerTurnPlayed(pp.nActionsPerTurn);

        // Set up game over conditions in all rules
        playerAction.addGameOverCondition(win);  // Can win after playing an action, but not reactions
        drawCards.addGameOverCondition(drawCardsLose);
        epidemic1.addGameOverCondition(infectionCardsLose);
        infectCities.addGameOverCondition(infectionCardsLose);
        epidemic2.addGameOverCondition(infectLose);
        epidemic2.addGameOverCondition(outbreakLose);
        infectCities.addGameOverCondition(infectLose);
        infectCities.addGameOverCondition(outbreakLose);

        // Putting it all together to set up game turn flow
        root = playerAction;
        // Player hand may end up over capacity after give/take card actions, ideally this should receive parameter from other rule
        playerAction.setNext(playerHandOverCapacity1);
        playerHandOverCapacity1.setParent(playerAction);
        playerHandOverCapacity1.setYesNo(forceDiscardReaction1, enoughActions);
        forceDiscardReaction1.setNext(playerActionInterrupt3);
        playerActionInterrupt3.setNext(playerHandOverCapacity1);
        enoughActions.setYesNo(drawCards, playerAction);  // Loop
        drawCards.setNext(firstEpidemic);
        firstEpidemic.setYesNo(epidemic1, enoughDraws);
        epidemic1.setNext(playerHasRPCard);  // Only 1 of these cards in the game, so only need to ask 1 player for reaction
        playerHasRPCard.setYesNo(forceRPreaction, epidemic2);
        forceRPreaction.setNext(playerActionInterrupt1);
        playerActionInterrupt1.setNext(epidemic2);
        epidemic2.setNext(enoughDraws);  // Loop
        enoughDraws.setYesNo(playerHandOverCapacity2, drawCards);  // Only asks current player for reaction. Loop
        playerHandOverCapacity2.setYesNo(forceDiscardReaction2, infectCities);
        forceDiscardReaction2.setNext(playerActionInterrupt2);
        playerActionInterrupt2.setNext(playerHandOverCapacity2);

        infectCities.setNext(nextPlayerRule);

        // Player reactions for playing events at the end of turn, one for each player
//        RuleNode forceAllPlayersEventReaction = new ForceAllPlayerReaction();
//        infectCities.setNext(forceAllPlayersEventReaction);  // End of turn, event reactions coming next
//        RuleNode[] eventActionInterrupt = new PlayerAction[nPlayers];
//        for (int i = 0; i < nPlayers; i++) {
//            eventActionInterrupt[i] = new PlayerAction(pp.nInitialDiseaseCubes);
//        }
//        for (int i = 0; i < nPlayers-1; i++) {
//            eventActionInterrupt[i].setNext(eventActionInterrupt[i+1]);
//        }
//        forceAllPlayersEventReaction.setNext(eventActionInterrupt[0]);
//        eventActionInterrupt[nPlayers-1].setNext(nextPlayerRule);  // Next player!

        nextPlayerRule.setNext(root);

        // Next rule to execute is root
        nextRule = root;

        // Draw game tree from root
//        new GameFlowDiagram(root);
    }

    /**
     * Copy constructor from root node.
     * @param root - root rule node.
     */
    public PandemicForwardModel(Node root) {
        super(root);
    }

    /**
     * Performs initial game setup according to game rules
     *  - sets up decks and shuffles
     *  - gives player cards
     *  - places tokens on boards
     *  etc.
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {

        PandemicGameState state = (PandemicGameState) firstState;
        state._reset();
        PandemicParameters pp = (PandemicParameters) state.getGameParameters();

        AbstractGameData _data = new AbstractGameData();
        _data.load(pp.getDataPath());

        state.tempDeck = new Deck<>("Temp Deck", VISIBLE_TO_ALL);
        state.areas = new HashMap<>();

        // For each player, initialize their own areas: they get a player hand and a player card
        int capacity = pp.maxCardsPerPlayer;
        for (int i = 0; i < state.getNPlayers(); i++) {
            Area playerArea = new Area(i, "Player Area");
            Deck<Card> playerHand = new Deck<>("Player Hand", VISIBLE_TO_ALL);
            playerHand.setOwnerId(i);
            playerHand.setCapacity(capacity);
            playerArea.putComponent(playerHandHash, playerHand);
            playerArea.putComponent(playerCardHash, new Card("Player Role"));
            state.areas.put(i, playerArea);
        }

        // Initialize the game area
        Area gameArea = new Area(-1, "Game Area");
        state.areas.put(-1, gameArea);

        // Load the board
        state.world = _data.findGraphBoard("cities");
        gameArea.putComponent(pandemicBoardHash, state.world);

        // Initialize game state variables
        state.setNCardsDrawn(0);
        state.researchStationLocations = new ArrayList<>();

        // Set up the counters and sync with game parameters
        Counter infection_rate = _data.findCounter("Infection Rate");
        infection_rate.setMaximum(pp.infectionRate.length);
        infection_rate.setValue(0);
        Counter outbreaks = _data.findCounter("Outbreaks");
        outbreaks.setMaximum(pp.loseMaxOutbreak);
        outbreaks.setValue(0);
        Counter researchStations = _data.findCounter("Research Stations");
        researchStations.setMaximum(pp.nResearchStations);
        researchStations.setValue(pp.nResearchStations);
        gameArea.putComponent(infectionRateHash, infection_rate);
        gameArea.putComponent(outbreaksHash, outbreaks);
        gameArea.putComponent(PandemicConstants.researchStationHash, researchStations);

        for (String color : colors) {
            int hash = Hash.GetInstance().hash("Disease " + color);
            Counter diseaseC = _data.findCounter("Disease " + color);
            diseaseC.setValue(0);  // 0 - cure not discovered; 1 - cure discovered; 2 - eradicated
            gameArea.putComponent(hash, diseaseC);

            hash = Hash.GetInstance().hash("Disease Cube " + color);
            Counter diseaseCubeCounter = _data.findCounter("Disease Cube " + color);
            diseaseCubeCounter.setMaximum(pp.nInitialDiseaseCubes);
            diseaseCubeCounter.setValue(pp.nInitialDiseaseCubes);
            gameArea.putComponent(hash, diseaseCubeCounter);
        }

        // Set up decks
        Deck<Card> playerDeck = new Deck<>("Player Deck", HIDDEN_TO_ALL); // contains city & event cards
        playerDeck.add(_data.findDeck("Cities"));
        pp.nCityCards = playerDeck.getSize();
        Deck<Card> eventCards = _data.findDeck("Events");
        pp.nEventCards = 0;
        for (Card c: eventCards.getComponents()) {
            String name = ((PropertyString)c.getProperty(nameHash)).value;
            if (pp.survivalRules && !name.equals("Airlift") && !name.equals("Government Grant")) continue;
            playerDeck.add(c);
            pp.nEventCards++;
        }
        playerDeck.shuffle(firstState.getRnd());

        Deck<Card> playerRoles = _data.findDeck("Player Roles");
        Deck<Card> infectionDeck =  _data.findDeck("Infections");
        Deck<Card> infectionDiscard =  new Deck<>("Infection Discard", VISIBLE_TO_ALL);

        gameArea.putComponent(PandemicConstants.playerDeckHash, playerDeck);
        gameArea.putComponent(PandemicConstants.playerDeckDiscardHash, new Deck<>("Player Deck Discard", VISIBLE_TO_ALL));
        gameArea.putComponent(PandemicConstants.infectionDiscardHash, infectionDiscard);
        gameArea.putComponent(PandemicConstants.plannerDeckHash, new Deck<>("Planner Deck", VISIBLE_TO_ALL)); // deck to store extra card for the contingency planner
        gameArea.putComponent(PandemicConstants.infectionHash, infectionDeck);
        gameArea.putComponent(PandemicConstants.playerRolesHash, playerRoles);

        state.addComponents();

        // Infection
        infectionDeck.shuffle(firstState.getRnd());
        int nCards = pp.nInfectionCardsSetup;
        int nTimes = pp.nInfectionsSetup;
        for (int j = 0; j < nTimes; j++) {
            for (int i = 0; i < nCards; i++) {
                // Place matching color (nTimes - j) cubes and place on matching city
                new InfectCity(infectionDeck.getComponentID(), infectionDiscard.getComponentID(), 0, pp.maxCubesPerCity, nTimes - j).execute(state);
            }
        }

        // Give players cards
        int nCardsPlayer = pp.nCardsPerPlayer.get(state.getNPlayers());
        playerRoles.shuffle(firstState.getRnd());
        long maxPop = 0;
        int startingPlayer = -1;

        for (int i = 0; i < state.getNPlayers(); i++) {
            // Draw a player card
            Card c = null;
            // Ugly code, but easier for setting parameters and optimisation
            if (i == 0 && !pp.player0Role.equals("Any"))
                c = getPlayerCardWithRole(playerRoles, pp.player0Role, state);
            else if (i == 1 && !pp.player1Role.equals("Any"))
                c = getPlayerCardWithRole(playerRoles, pp.player1Role, state);
            else if (i == 2 && !pp.player2Role.equals("Any"))
                c = getPlayerCardWithRole(playerRoles, pp.player2Role, state);
            else if (i == 3 && !pp.player3Role.equals("Any"))
                c = getPlayerCardWithRole(playerRoles, pp.player3Role, state);
            if (c == null)
                c = playerRoles.draw();

            c.setOwnerId(i);

            // Give the card to this player
            Area playerArea = state.getArea(i);
            playerArea.putComponent(playerCardHash, c);

            // Also add this player in Atlanta
            placePlayer(state, "Atlanta", i);

            // Set up player hands
            Deck<Card> playerHandDeck = (Deck<Card>) playerArea.getComponent(playerHandHash);

            for (int j = 0; j < nCardsPlayer; j++) {
                new DrawCard(playerDeck.getComponentID(), playerHandDeck.getComponentID()).execute(state);
            }

            for (Card card: playerHandDeck.getComponents()) {
                Property property = card.getProperty(Hash.GetInstance().hash("population"));
                if (property != null){
                    long pop = ((PropertyLong) property).value;
                    if (pop > maxPop) {
                        startingPlayer = i;
                        maxPop = pop;
                    }
                }
            }
        }

        // Epidemic cards
        playerDeck.shuffle(state.getRnd());
        int noCards = playerDeck.getSize();
        int noEpidemicCards = pp.nEpidemicCards;
        if (noEpidemicCards > 0) {
            int range = noCards / noEpidemicCards;
            for (int i = 0; i < noEpidemicCards; i++) {
                int index = i * range + i + state.getRnd().nextInt(range);

                Card card = new Card("Epidemic");
                card.setProperty(new PropertyString("name", "epidemic"));
                playerDeck.add(card, index);
            }
        }

        // Research station in Atlanta
        new AddResearchStation("Atlanta").execute(state);

        // Player with highest population starts
        state.getTurnOrder().setStartingPlayer(startingPlayer);
    }

    private Card getPlayerCardWithRole(Deck<Card> cards, String role, PandemicGameState pp) {
        // Possible to have multiple possible roles separated by ","
        HashSet<String> roles = new HashSet<>();
        if (role.contains(",")) {
            String[] several = role.split(",");
            roles.addAll(Arrays.asList(several));
        } else {
            roles.add(role);
        }
        Deck<Card> subset = new Deck<>("Temp", HIDDEN_TO_ALL);
        for (Card c: cards.getComponents()) {
            if (roles.contains(c.toString())) {
                if (roles.size() == 1) return c;
                subset.add(c);
            }
        }
        if (subset.getSize() > 0) {
            subset.shuffle(pp.getRnd());
            return subset.draw();
        }
        return null;
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of IAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        PandemicGameState pgs = (PandemicGameState) gameState;
        PandemicTurnOrder pto = (PandemicTurnOrder) pgs.getTurnOrder();
        if (pto.reactionsFinished()) {
            gameState.setGamePhase(CoreConstants.DefaultGamePhase.Main);
        }
        if (gameState.getGamePhase() == PandemicGameState.PandemicGamePhase.DiscardReaction)
            return getDiscardActions(pgs);
        else if (gameState.getGamePhase() == PandemicGameState.PandemicGamePhase.RPReaction)
            return getRPactions(pgs);
        else if (gameState.getGamePhase() == CoreConstants.DefaultGamePhase.PlayerReaction)
            return getEventActions(pgs);
        else if (gameState.getGamePhase() == PandemicGameState.PandemicGamePhase.Forecast)
            return getForecastActions(pgs);
        else return getPlayerActions(pgs);
    }

    public PandemicForwardModel copy() {
        PandemicForwardModel retValue = new PandemicForwardModel(copyRoot());
        retValue.decisionPlayerID = decisionPlayerID;
        retValue.decorators = new ArrayList<>(decorators);
        return retValue;
    }

    @Override
    protected void endPlayerTurn(AbstractGameState state) {
        PandemicGameState pgs = (PandemicGameState) state;
        pgs.getTurnOrder().endPlayerTurn(state);
    }

    @Override
    protected void endGame(AbstractGameState gameState) {
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            gameState.setPlayerResult(gameState.getGameStatus(), i);
        }
        if (gameState.getCoreGameParameters().verbose) {
            System.out.println(gameState.getGameStatus());
        }
    }
}
