package games.root_final.cards;

import core.components.Card;
import games.dominion.cards.DominionCard;
import games.root_final.RootParameters;
import games.root_final.components.Item;

import java.util.List;

public class EyrieRulers extends Card {
    public enum CardType{
        Despot,
        Commander,
        Charismatic,
        Builder
    }
    public final CardType ruler;
    public final boolean vizierRecruit;
    public final boolean vizierMove;
    public final boolean vizierBattle;

    public final boolean vizierBuild;

    public EyrieRulers(CardType ruler, boolean vizierRecruit, boolean vizierMove, boolean vizierBattle, boolean vizierBuild){
        super(ruler.toString());
        this.ruler = ruler;
        this.vizierRecruit = vizierRecruit;
        this.vizierMove = vizierMove;
        this.vizierBattle = vizierBattle;
        this.vizierBuild = vizierBuild;
    }

    public EyrieRulers(CardType ruler, boolean vizierRecruit, boolean vizierMove, boolean vizierBattle, boolean vizierBuild, int componentID){
        super(ruler.toString(), componentID);
        this.ruler = ruler;
        this.vizierRecruit = vizierRecruit;
        this.vizierMove = vizierMove;
        this.vizierBattle = vizierBattle;
        this.vizierBuild = vizierBuild;
    }

    @Override
    public Card copy(){
        return new EyrieRulers(ruler, vizierRecruit, vizierMove, vizierBattle, vizierBuild, componentID);
    }

    @Override
    public String toString(){
        return ruler.name();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EyrieRulers) {
            EyrieRulers other = (EyrieRulers) obj;
            return other.ruler == ruler;
        }
        return false;
    }
}
