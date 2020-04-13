package explodingkittens;

import content.Property;
import explodingkittens.ExplodingKittenCard;
import utilities.Hash;

public class ExplodingKittensCardTypeProperty extends Property {

    public ExplodingKittenCard value;

    public ExplodingKittensCardTypeProperty(ExplodingKittenCard card_type)
    {
        this.hashString = card_type.toString();
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.value = card_type;
    }

    public ExplodingKittensCardTypeProperty(String key, int value)
    {
        this.hashString = key;
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.value = ExplodingKittenCard.valueOf(key);;
    }

    public ExplodingKittensCardTypeProperty(String key, int hashkey, ExplodingKittenCard value)
    {
        this.hashString = key;
        this.hashKey = hashkey;
        this.value = ExplodingKittenCard.valueOf(key);;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public boolean equals(Object other)
    {
        if (other instanceof ExplodingKittensCardTypeProperty)
            return value == ((ExplodingKittensCardTypeProperty)(other)).value;
        return false;
    }

    public Property copy()
    {
        return new ExplodingKittensCardTypeProperty(hashString, hashKey, value);
    }

}
