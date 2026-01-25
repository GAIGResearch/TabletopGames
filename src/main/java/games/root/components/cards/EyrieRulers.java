package games.root.components.cards;

import core.components.Card;

import java.util.Objects;

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

    @Override
    public EyrieRulers copy(){
        return this;  // All final
    }

    @Override
    public String toString(){
        return ruler.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EyrieRulers that = (EyrieRulers) o;
        return vizierRecruit == that.vizierRecruit && vizierMove == that.vizierMove && vizierBattle == that.vizierBattle && vizierBuild == that.vizierBuild && ruler == that.ruler;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ruler, vizierRecruit, vizierMove, vizierBattle, vizierBuild);
    }
}
