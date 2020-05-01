package games.pandemic;

import core.actions.*;
import core.components.*;
import core.content.*;
import core.*;
import games.pandemic.actions.*;
import games.pandemic.engine.*;
import games.pandemic.engine.conditions.*;
import games.pandemic.engine.gameOver.*;
import games.pandemic.engine.rules.*;
import utilities.Hash;

import java.util.Random;

import static games.pandemic.Constants.*;
import static games.pandemic.actions.MovePlayer.placePlayer;

@SuppressWarnings("unchecked")
public class PandemicForwardModel extends ForwardModel {

    // Random generator for this game.
    protected Random rnd;
    private PandemicParameters pp;

    // Rule executed last, rule to be executed next, and first rule to be executed in a turn (root)
    Node lastRule, nextRule, root;

    public PandemicForwardModel(PandemicParameters pp) {
        rnd = new Random(pp.game_seed);
        this.pp = pp;

        // Game over conditions
        GameOverCondition infectLose = new GameOverInfection();
        GameOverCondition outbreakLose = new GameOverOutbreak(pp.lose_max_outbreak);
        GameOverCondition drawCardsLose = new GameOverDrawCards();
        GameOverCondition win = new GameOverDiseasesCured();

        // Rules
        RuleNode infectCities = new InfectCities(pp.infection_rate, pp.max_cubes_per_city, pp.n_cubes_infection);
        RuleNode forceDiscardReaction = new ForceDiscardReaction();
        RuleNode playerActionInterrupt2 = new PlayerAction(pp.n_initial_disease_cubes);
        RuleNode epidemic2 = new EpidemicIntensify(rnd);
        RuleNode forceRPreaction = new ForceRPReaction();
        RuleNode playerActionInterrupt1 = new PlayerAction(pp.n_initial_disease_cubes);
        RuleNode epidemic1 = new EpidemicInfect(pp.max_cubes_per_city, pp.n_cubes_epidemic);
        RuleNode drawCards = new DrawCards();
        RuleNode playerAction = new PlayerAction(pp.n_initial_disease_cubes);

        // Conditions
        ConditionNode playerHandOverCapacity = new PlayerHandOverCapacity();
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
        root = playerAction;
        playerAction.setNext(enoughActions);
        enoughActions.setYes(drawCards);
        enoughActions.setNo(playerAction);  // Loop
        drawCards.setNext(firstEpidemic);
        firstEpidemic.setYes(epidemic1);
        firstEpidemic.setNo(enoughDraws);
        epidemic1.setNext(playerHasRPCard);
        playerHasRPCard.setYes(forceRPreaction);
        playerHasRPCard.setNo(epidemic2);
        forceRPreaction.setNext(playerActionInterrupt1);
        playerActionInterrupt1.setNext(playerHasRPCard);  // Loop
        epidemic2.setNext(enoughDraws);  // Loop
        enoughDraws.setYes(playerHandOverCapacity);
        enoughDraws.setNo(drawCards); // Loop
        playerHandOverCapacity.setYes(forceDiscardReaction);
        playerHandOverCapacity.setNo(infectCities);
        forceDiscardReaction.setNext(playerActionInterrupt2);
        playerActionInterrupt2.setNext(infectCities);
        infectCities.setNext(null);  // End of turn

        // Next rule to execute is root
        nextRule = root;

        // draw tree from root TODO
    }

    @Override
    public void next(AbstractGameState currentState, IAction action) {
        PandemicGameState pgs = (PandemicGameState)currentState;

        do {
            if (nextRule.requireAction() && action != null) {
                nextRule.setAction(action);
                action = null;
            }
            lastRule = nextRule;
            nextRule = nextRule.execute(currentState);
        } while (nextRule != null);

        nextRule = lastRule.getNext();  // go back to parent, skip it and go to next rule
        if (nextRule == null) {
            // if still null, end of turn:
            pgs.roundStep = 0;
            nextRule = root;
            pgs.nextPlayer();

        }
    }

    public void setup(AbstractGameState firstState) {
        PandemicGameState state = (PandemicGameState) firstState;

        // 1 research station in Atlanta
        new AddResearchStation("Atlanta").execute(state);

        // init counters
        Counter outbreaksCounter = (Counter) state.getComponent(outbreaksHash);
        outbreaksCounter.setValue(0);

        Counter rStationCounter = (Counter) state.getComponent(researchStationHash);
        rStationCounter.setValue(pp.n_research_stations);

        Counter infectionRateCounter = (Counter) state.getComponent(infectionRateHash);
        infectionRateCounter.setValue(0);

        for (String color : Constants.colors) {
            Counter diseaseCounter = (Counter) state.getComponent(Hash.GetInstance().hash("Disease " + color));
            diseaseCounter.setValue(0);
        }

        // infection
        Deck<Card> infectionDeck =  (Deck<Card>) state.getComponent(infectionHash);
        Deck<Card> infectionDiscard =  (Deck<Card>) state.getComponent(infectionDiscardHash);
        infectionDeck.shuffle(rnd);
        int nCards = pp.n_infection_cards_setup;
        int nTimes = pp.n_infections_setup;
        for (int j = 0; j < nTimes; j++) {
            for (int i = 0; i < nCards; i++) {
                Card c = infectionDeck.draw();

                // Place matching color (nTimes - j) cubes and place on matching city
                new InfectCity(pp.max_cubes_per_city, c, nTimes - j).execute(state);

                // Discard card
                new DrawCard(infectionDeck, infectionDiscard).execute(state);
            }
        }

        // give players cards;
        Deck<Card> playerRoles =  (Deck<Card>) state.getComponent(playerRolesHash);
        Deck<Card> playerDeck =  (Deck<Card>) state.getComponent(playerDeckHash);
        int nCardsPlayer = pp.n_cards_per_player.get(state.getNPlayers());
        playerRoles.shuffle();
        long maxPop = 0;
        int startingPlayer = -1;

        for (int i = 0; i < state.getNPlayers(); i++) {
            // Draw a player card
            Card c = playerRoles.draw();

            // Give the card to this player
            Area playerArea = state.getArea(i);
            playerArea.setComponent(playerCardHash, c);

            // Also add this player in Atlanta
            placePlayer(state, "Atlanta", i);

            // Give players cards
            IDeck<Card> playerHandDeck = (IDeck<Card>) playerArea.getComponent(Constants.playerHandHash);

            playerDeck.shuffle(rnd);
            for (int j = 0; j < nCardsPlayer; j++) {
                new DrawCard(playerDeck, playerHandDeck).execute(state);
            }

            for (Card card: playerHandDeck.getCards()) {
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
        int noCards = playerDeck.getCards().size();
        int noEpidemicCards = pp.n_epidemic_cards;
        int range = noCards / noEpidemicCards;
        for (int i = 0; i < noEpidemicCards; i++) {
            int index = i * range + i + rnd.nextInt(range);

            Card card = new Card();
            card.setProperty(Hash.GetInstance().hash("name"), new PropertyString("epidemic"));
            new AddCardToDeck(card, playerDeck, index).execute(state);

        }

        // Player with highest population starts
        state.setActivePlayer(startingPlayer);
    }

    /*


    @Override
    public ForwardModel copy() {
        PandemicForwardModel fm = new PandemicForwardModel(pp);
        fm.rnd = rnd; //TODO: revisit this, we may not want the same random generator.
        fm.pp = (PandemicParameters)pp.copy();
        return fm;
    }
     */
}
