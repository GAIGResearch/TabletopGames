package games.root_final.cards;

import core.components.Card;
import games.root_final.components.Item;

import java.util.List;
import java.util.Objects;

public class VagabondCharacters extends Card {
    public enum CardType{
        Thief,
        Ranger,
        Tinker,
    }
    public final CardType character;
    public final List<Item.ItemType> startsWith;

    public VagabondCharacters(CardType character, List<Item.ItemType> startsWith){
        super(character.toString());
        this.character = character;
        this.startsWith = startsWith;
    }

    @Override
    public VagabondCharacters copy(){
        return this;
    }

    @Override
    public String toString(){
        return character.name();
    }
}
