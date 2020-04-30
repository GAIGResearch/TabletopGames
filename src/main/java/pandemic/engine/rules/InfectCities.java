package pandemic.engine.rules;

import actions.AddCardToDeck;
import actions.DrawCard;
import components.Card;
import components.Counter;
import components.IDeck;
import core.GameState;
import pandemic.PandemicGameState;
import pandemic.actions.InfectCity;
import pandemic.engine.Node;

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
    protected boolean run(GameState gs) {
        if (!((PandemicGameState)gs).isQuietNight()) {

            Counter infectionCounter = gs.findCounter("Infection Rate");
            int noCardsDrawn = infection_rate[infectionCounter.getValue()];
            String tempDeckID = gs.tempDeck();
            DrawCard action = new DrawCard("Infections", tempDeckID);
            for (int i = 0; i < noCardsDrawn; i++) {  // Draw infection cards into a new deck
                action.execute(gs);
            }
            IDeck tempDeck = gs.findDeck(tempDeckID);
            for (Card c : tempDeck.getCards()) {  // Check the drawn cards and infect cities
                new InfectCity(max_cubes_per_city, c, n_cubes_infection).execute(gs);
                new AddCardToDeck(c, gs.findDeck("Infection Discard")).execute(gs);
            }
            gs.clearTempDeck();
            ((PandemicGameState) gs).setQuietNight(false);
            return true;
        }
        ((PandemicGameState)gs).setEpidemic(false);
        ((PandemicGameState)gs).setNCardsDrawn(0);
        return false;
    }
}
