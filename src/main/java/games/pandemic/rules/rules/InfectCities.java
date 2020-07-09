package games.pandemic.rules.rules;

import core.AbstractGameState;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
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

    @Override
    protected boolean run(AbstractGameState gs) {
        PandemicGameState pgs = (PandemicGameState)gs;
        if (!pgs.isQuietNight()) {
            // Infecting with top cards of infection deck if it's not a quiet night
            Counter infectionCounter = (Counter)((PandemicGameState)gs).getComponent(infectionRateHash);
            int noCardsDrawn = infection_rate[infectionCounter.getValue()];
            Deck<Card> infectionDeck = (Deck<Card>) pgs.getComponent(infectionHash);
            Deck<Card> infectionDiscardDeck = (Deck<Card>) pgs.getComponent(infectionDiscardHash);
            for (int c = 0; c < noCardsDrawn; c++) {  // Check the drawn cards and infect cities
                new InfectCity(infectionDeck.getComponentID(), infectionDiscardDeck.getComponentID(), 0,
                        max_cubes_per_city, n_cubes_infection).execute(gs);
            }
            return true;
        }
        // No more quiet night, no more epidemic
        ((PandemicGameState) gs).setQuietNight(false);
        ((PandemicGameState)gs).setEpidemic(false);
        ((PandemicGameState)gs).setNCardsDrawn(0);
        return false;
    }
}
