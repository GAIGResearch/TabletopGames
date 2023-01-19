package games.pandemic.rules.rules;

import core.AbstractGameStateWithTurnOrder;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.rules.Node;
import core.rules.nodetypes.RuleNode;
import games.pandemic.PandemicGameState;
import games.pandemic.actions.InfectCity;

import static games.pandemic.PandemicConstants.*;

public class InfectCities extends RuleNode {
    int max_cubes_per_city;
    int n_cubes_infection;
    int[] infection_rate;

    public InfectCities(int[] infection_rate, int max_cubes_per_city, int n_cubes_infection) {
        super();
        this.infection_rate = infection_rate;
        this.max_cubes_per_city = max_cubes_per_city;
        this.n_cubes_infection = n_cubes_infection;
    }

    /**
     * Copy constructor
     * @param infectCities - Node to be copied
     */
    public InfectCities(InfectCities infectCities) {
        super(infectCities);
        this.max_cubes_per_city = infectCities.max_cubes_per_city;
        this.n_cubes_infection = infectCities.n_cubes_infection;
        this.infection_rate = infectCities.infection_rate;
    }

    @Override
    protected boolean run(AbstractGameStateWithTurnOrder gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        if (!pgs.isQuietNight()) {
            // Infecting with top cards of infection deck if it's not a quiet night
            Counter infectionCounter = (Counter)((PandemicGameState)gs).getComponent(infectionRateHash);
            int nCardsToDraw = infection_rate[infectionCounter.getValue()];
            Deck<Card> infectionDeck = (Deck<Card>) pgs.getComponent(infectionHash);

            Deck<Card> infectionDiscardDeck = (Deck<Card>) pgs.getComponent(infectionDiscardHash);
            for (int c = 0; c < nCardsToDraw; c++) {  // Check the drawn cards and infect cities
                if (infectionDeck.getSize() <= 0) return false;
                new InfectCity(infectionDeck.getComponentID(), infectionDiscardDeck.getComponentID(), 0,
                        max_cubes_per_city, n_cubes_infection).execute(gs);
            }
        }
        // No more quiet night, no more epidemic
        ((PandemicGameState) gs).setQuietNight(false);
        ((PandemicGameState)gs).setEpidemic(false);
        ((PandemicGameState)gs).setNCardsDrawn(0);
        return true;
    }

    @Override
    protected Node _copy() {
        return new InfectCities(this);
    }
}
