package content;

import explodingkittens.ExplodingKittenCard;
import explodingkittens.ExplodingKittenCards;
import utilities.Hash;

public class ExplodingKittensCardType extends Property {

    public ExplodingKittenCard value;

    public ExplodingKittensCardType(ExplodingKittenCard card_type)
    {
        this.hashString = "";
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.value = card_type;
    }

    public ExplodingKittensCardType(String key, int value)
    {
        this.hashString = key;
        this.hashKey = Hash.GetInstance().hash(hashString);
        this.value = value;
    }

    public ExplodingKittensCardType(String key, int hashKey, int value)
    {
        this.hashString = key;
        this.hashKey = hashKey;
        this.value = value;
    }


    @Override
    public String toString() {
        return ""+value;
    }

    public boolean equals(Object other)
    {
        if(other instanceof PropertyInt)
            return value == ((PropertyInt)(other)).value;
        return false;
    }

    public Property copy()
    {
        return new PropertyInt(hashString, hashKey, value);
    }


}
