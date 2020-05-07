package games.pandemic.engine.rules;

import core.AbstractGameState;
import core.actions.AddCardToDeck;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import games.pandemic.PandemicGameState;
import games.pandemic.actions.InfectCity;

import static games.pandemic.PandemicConstants.*;

@SuppressWarnings("unchecked")
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

            Counter infectionCounter = (Counter)((PandemicGameState)gs).getComponent(infectionRateHash);
            int noCardsDrawn = infection_rate[infectionCounter.getValue()];
            Deck<Card> tempDeck = pgs.getTempDeck();
            Deck<Card> infectionDeck = (Deck<Card>) pgs.getComponent(infectionHash);
            Deck<Card> infectionDiscardDeck = (Deck<Card>) pgs.getComponent(infectionDiscardHash);
            DrawCard action = new DrawCard(infectionDeck, tempDeck);
            for (int i = 0; i < noCardsDrawn; i++) {  // Draw infection cards into a new deck
                action.execute(gs);
            }
            for (Card c : tempDeck.getCards()) {  // Check the drawn cards and infect cities
                new InfectCity(max_cubes_per_city, c, n_cubes_infection).execute(gs);
                new AddCardToDeck(c, infectionDiscardDeck).execute(gs);
            }
            pgs.clearTempDeck();
            ((PandemicGameState) gs).setQuietNight(false);
            return true;
        }
        ((PandemicGameState)gs).setEpidemic(false);
        ((PandemicGameState)gs).setNCardsDrawn(0);
        return false;
    }
}
