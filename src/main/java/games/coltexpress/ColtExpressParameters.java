package games.coltexpress;

import core.GameParameters;
import games.coltexpress.cards.CharacterType;
import games.coltexpress.cards.ColtExpressCard;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class ColtExpressParameters extends GameParameters {

    HashMap<ColtExpressCard.CardType, Integer> cardCounts = new HashMap<ColtExpressCard.CardType, Integer>() {{
        put(ColtExpressCard.CardType.MoveSideways, 2);
        put(ColtExpressCard.CardType.MoveUp, 2);
        put(ColtExpressCard.CardType.Punch, 1);
        put(ColtExpressCard.CardType.MoveMarshal, 1);
        put(ColtExpressCard.CardType.Shoot, 2);
        put(ColtExpressCard.CardType.CollectMoney, 2);
    }};

    HashSet<CharacterType> characters;

    public ColtExpressParameters() {
        characters = new HashSet<CharacterType>();
        Collections.addAll(characters, CharacterType.values());
    }

    public CharacterType pickRandomCharacterType(){
        int size = characters.size();
        int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
        int i = 0;
        for(CharacterType obj : characters) {
            if (i == item){
                characters.remove(obj);
                return obj;
            }
            i++;
        }
        return null;
    }

    public void pickCharacterType(CharacterType characterType){
        characters.remove(characterType);
    }
}
