package games.root.components.cards;

import core.components.Card;
import games.root.components.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VagabondCharacter extends Card {

    public enum CardType {
        Thief,
        Ranger,
        Tinker,
    }

    public final CardType characterType;
    private final List<Item.ItemType> startsWith;

    public VagabondCharacter(CardType character, List<Item.ItemType> startsWith){
        super(character.toString());
        this.characterType = character;
        this.startsWith = startsWith;
    }

    public VagabondCharacter(CardType character, List<Item.ItemType> startsWith, int componentID){
        super(character.toString(), componentID);
        this.characterType = character;
        this.startsWith = startsWith;
    }

    public boolean startsWith(Item.ItemType itemType) {
        return startsWith.contains(itemType);
    }

    @Override
    public VagabondCharacter copy(){
        return new VagabondCharacter(characterType, new ArrayList<>(startsWith), getComponentID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        VagabondCharacter that = (VagabondCharacter) o;
        return characterType == that.characterType && Objects.equals(startsWith, that.startsWith);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), characterType, startsWith);
    }

    @Override
    public String toString(){
        return characterType.name();
    }
}
