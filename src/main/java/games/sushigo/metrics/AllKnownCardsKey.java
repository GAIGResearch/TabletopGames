package games.sushigo.metrics;

import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;
import core.interfaces.IStateKey;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;

import java.util.ArrayList;
import java.util.List;

public class AllKnownCardsKey implements IStateKey {
    @Override
    public Object getKey(AbstractGameState state, int playerId) {
        SGGameState sg = (SGGameState) state;
        List<Deck<SGCard>> played = new ArrayList<>();
        List<Deck<SGCard>> hands = new ArrayList<>();

        // only the ones whose hands are known
        for(int i = 0; i < sg.getNPlayers(); i++) {
            played.add(sg.getPlayedCards().get(i));
            if(sg.isHandKnown(playerId,i)) {
                hands.add(sg.getPlayerHands().get(i));
            }
        }
   //     System.out.println("Total number of hands: " + hands.size() + ", played cards: " + played.size());

        played.addAll(hands);
        List<Integer> sortedList = new ArrayList<>();
        for(Deck<SGCard> deck : played) {
            List<Integer> cardNames = new ArrayList<>(deck.stream().map(x->x.type.ordinal()).toList());
            cardNames.sort(Integer::compare);

            sortedList.addAll(cardNames);
        }

        return sortedList;
    }
}
