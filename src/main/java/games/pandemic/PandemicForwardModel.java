package games.pandemic;

import core.actions.*;
import core.components.*;
import core.properties.*;
import core.*;
import games.pandemic.actions.*;
import games.pandemic.engine.*;
import games.pandemic.engine.conditions.*;
import games.pandemic.engine.gameOver.*;
import games.pandemic.engine.rules.*;
import utilities.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static games.pandemic.PandemicConstants.*;
import static games.pandemic.actions.MovePlayer.placePlayer;
import static core.CoreConstants.nameHash;
import static core.CoreConstants.playerHandHash;

public class PandemicForwardModel extends AbstractForwardModel {

    // Rule executed last, rule to be executed next, and first rule to be executed in a turn (root)
    Node lastRule, nextRule, root;

    /**
     * Constructor. Creates the rules for the game and sets up the game rule graph.
     * @param pp - parameters for the game.
     * @param nPlayers - number of players in the game.
     */
    public PandemicForwardModel(PandemicParameters pp, int nPlayers) {

        // Game over conditions
        GameOverCondition infectLose = new GameOverInfection();
        GameOverCondition outbreakLose = new GameOverOutbreak(pp.lose_max_outbreak);
        GameOverCondition drawCardsLose = new GameOverDrawCards();
        GameOverCondition win = new GameOverDiseasesCured();

        // Rules
        RuleNode infectCities = new InfectCities(pp.infection_rate, pp.max_cubes_per_city, pp.n_cubes_infection);
        RuleNode forceDiscardReaction = new ForceDiscardReaction();
        RuleNode epidemic2 = new EpidemicIntensify(new Random(pp.getGameSeed()));
        RuleNode forceRPreaction = new ForceRPReaction();
        RuleNode epidemic1 = new EpidemicInfect(pp.max_cubes_per_city, pp.n_cubes_epidemic);
        RuleNode drawCards = new DrawCards();
        RuleNode playerAction = new PlayerAction(pp.n_initial_disease_cubes);
        RuleNode playerActionInterrupt1 = new PlayerAction(pp.n_initial_disease_cubes);
        RuleNode playerActionInterrupt2 = new PlayerAction(pp.n_initial_disease_cubes);
        RuleNode playerActionInterrupt3 = new PlayerAction(pp.n_initial_disease_cubes);

        // Conditions
        ConditionNode playerHandOverCapacity1 = new PlayerHandOverCapacity();
        ConditionNode playerHandOverCapacity2 = new PlayerHandOverCapacity();
        ConditionNode playerHasRPCard = new HasRPCard();
        ConditionNode enoughDraws = new EnoughDraws(pp.n_cards_draw);
        ConditionNode firstEpidemic = new IsEpidemic();
        ConditionNode enoughActions = new ActionsPerTurnPlayed(pp.n_actions_per_turn);

        // Set up game over conditions in all rules
        playerAction.addGameOverCondition(win);  // Can win after playing an action, but not reactions
        drawCards.addGameOverCondition(drawCardsLose);
        epidemic2.addGameOverCondition(infectLose);
        epidemic2.addGameOverCondition(outbreakLose);
        infectCities.addGameOverCondition(infectLose);
        infectCities.addGameOverCondition(outbreakLose);

        // Putting it all together to set up game turn flow
        // possible future work: Nodes passing parameters to others
        root = playerAction;
        // Player hand may end up over capacity after give/take card actions, ideally this should receive parameter from other rule
        playerAction.setNext(playerHandOverCapacity1);
        playerHandOverCapacity1.setParent(playerAction);
        playerHandOverCapacity1.setYesNo(playerActionInterrupt3, enoughActions);
        playerActionInterrupt3.setNext(enoughActions);
        enoughActions.setYesNo(drawCards, playerAction);  // Loop
        drawCards.setNext(firstEpidemic);
        firstEpidemic.setYesNo(epidemic1, enoughDraws);
        epidemic1.setNext(playerHasRPCard);  // Only 1 of these cards in the game, so only need to ask 1 player for reaction
        playerHasRPCard.setYesNo(forceRPreaction, epidemic2);
        forceRPreaction.setNext(playerActionInterrupt1);
        playerActionInterrupt1.setNext(epidemic2);
        epidemic2.setNext(enoughDraws);  // Loop
        enoughDraws.setYesNo(playerHandOverCapacity2, drawCards);  // Only asks current player for reaction. Loop
        playerHandOverCapacity2.setYesNo(forceDiscardReaction, infectCities);
        forceDiscardReaction.setNext(playerActionInterrupt2);
        playerActionInterrupt2.setNext(infectCities);
//        infectCities.setNext(null);  // End of turn

        // Player reactions for playing events at the end of turn, one for each player
        RuleNode forceAllPlayersEventReaction = new ForceAllEventReaction();
        infectCities.setNext(forceAllPlayersEventReaction);  // End of turn, event reactions coming next
        RuleNode[] eventActionInterrupt = new PlayerAction[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            eventActionInterrupt[i] = new PlayerAction(pp.n_initial_disease_cubes);
        }
        for (int i = 0; i < nPlayers-1; i++) {  // Last one will be null, turn over
            eventActionInterrupt[i].setNext(eventActionInterrupt[i+1]);
        }
        forceAllPlayersEventReaction.setNext(eventActionInterrupt[0]);
        eventActionInterrupt[nPlayers-1].setNext(null);  // End of turn

        // Next rule to execute is root
        nextRule = root;

        // draw tree from root
//        new GameFlowDiagram(root);
    }

    /**
     * Applies the given action to the game state and executes any other game rules.
     * @param currentState - current game state, to be modified by the action.
     * @param action - action requested to be played by a player.
     */
    @Override
    public void next(AbstractGameState currentState, AbstractAction action) {
        PandemicGameState pgs = (PandemicGameState)currentState;

        do {
            if (nextRule.requireAction()) {
                if (action != null) {
                    nextRule.setAction(action);
                    action = null;
                } else {
                    return;  // Wait for action to be sent to execute this rule requiring action
                }
            }
            lastRule = nextRule;
            nextRule = nextRule.execute(currentState);
        } while (nextRule != null);

        nextRule = lastRule.getNext();  // go back to parent, skip it and go to next rule
        if (nextRule == null) {
            // if still null, end of turn:
            nextRule = root;
            pgs.nextPlayer();
        }
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
    public void setup(AbstractGameState firstState) {
        PandemicGameState state = (PandemicGameState) firstState;
        PandemicParameters pp = (PandemicParameters)state.getGameParameters();
        PandemicData _data = (PandemicData)state.getData();

        state.tempDeck = new Deck<>("Temp Deck");
        state.areas = new HashMap<>();

        // For each player, initialize their own areas: they get a player hand and a player card
        int capacity = pp.max_cards_per_player;
        for (int i = 0; i < state.getNPlayers(); i++) {
            Area playerArea = new Area(i, "Player Area");
            Deck<Card> playerHand = new Deck<>("Player Hand");
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
        state.world = _data.findBoard("cities");
        gameArea.putComponent(pandemicBoardHash, state.world);

        // Initialize game state variables
        state.setNCardsDrawn(0);
        state.researchStationLocations = new ArrayList<>();

        // Set up the counters and sync with game parameters
        Counter infection_rate = _data.findCounter("Infection Rate");
        infection_rate.setMaximum(pp.infection_rate.length);
        infection_rate.setValue(0);
        Counter outbreaks = _data.findCounter("Outbreaks");
        outbreaks.setMaximum(pp.lose_max_outbreak);
        outbreaks.setValue(0);
        Counter researchStations = _data.findCounter("Research Stations");
        researchStations.setMaximum(pp.n_research_stations);
        researchStations.setValue(pp.n_research_stations);
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
            diseaseCubeCounter.setMaximum(pp.n_initial_disease_cubes);
            diseaseCubeCounter.setValue(0);
            gameArea.putComponent(hash, diseaseCubeCounter);
        }

        // Set up decks
        Deck<Card> playerDeck = new Deck<>("Player Deck"); // contains city & event cards
        playerDeck.add(_data.findDeck("Cities"));
        playerDeck.add(_data.findDeck("Events"));
        Deck<Card> playerRoles = _data.findDeck("Player Roles");
        Deck<Card> infectionDeck =  _data.findDeck("Infections");
        Deck<Card> infectionDiscard =  new Deck<>("Infection Discard");

        gameArea.putComponent(PandemicConstants.playerDeckHash, playerDeck);
        gameArea.putComponent(PandemicConstants.playerDeckDiscardHash, new Deck<>("Player Deck Discard"));
        gameArea.putComponent(PandemicConstants.infectionDiscardHash, infectionDiscard);
        gameArea.putComponent(PandemicConstants.plannerDeckHash, new Deck<>("Planner Deck")); // deck to store extra card for the contingency planner
        gameArea.putComponent(PandemicConstants.infectionHash, infectionDeck);
        gameArea.putComponent(PandemicConstants.playerRolesHash, playerRoles);

        // Infection
        infectionDeck.shuffle(rnd);
        int nCards = pp.n_infection_cards_setup;
        int nTimes = pp.n_infections_setup;
        for (int j = 0; j < nTimes; j++) {
            for (int i = 0; i < nCards; i++) {
                // Place matching color (nTimes - j) cubes and place on matching city
                new InfectCity(infectionDeck.getComponentID(), infectionDiscard.getComponentID(), 0, pp.max_cubes_per_city, nTimes - j).execute(state);
            }
        }

        // Give players cards
        int nCardsPlayer = pp.n_cards_per_player.get(state.getNPlayers());
        playerRoles.shuffle();
        long maxPop = 0;
        int startingPlayer = -1;

        for (int i = 0; i < state.getNPlayers(); i++) {
            // Draw a player card
            Card c = playerRoles.draw();
            c.setOwnerId(i);

            // Give the card to this player
            Area playerArea = state.getArea(i);
            playerArea.putComponent(playerCardHash, c);

            // Also add this player in Atlanta
            placePlayer(state, "Atlanta", i);

            // Set up player hands
            Deck<Card> playerHandDeck = (Deck<Card>) playerArea.getComponent(playerHandHash);

            playerDeck.shuffle(rnd);
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
        playerDeck.shuffle(rnd);
        int noCards = playerDeck.getSize();
        int noEpidemicCards = pp.n_epidemic_cards;
        int range = noCards / noEpidemicCards;
        for (int i = 0; i < noEpidemicCards; i++) {
            int index = i * range + i + rnd.nextInt(range);

            Card card = new Card("Epidemic");
            card.setProperty(nameHash, new PropertyString("epidemic"));
            playerDeck.add(card, index);

        }

        // Research station in Atlanta
        new AddResearchStation("Atlanta").execute(state);

        // Player with highest population starts
        state.getTurnOrder().setStartingPlayer(startingPlayer);
    }

    /*
    @Override
    public ForwardModel copy() {
        PandemicForwardModel fm = new PandemicForwardModel(pp);
        fm.rnd = new Random();
        fm.pp = (PandemicParameters)pp.copy();
        return fm;
    }
     */
}
