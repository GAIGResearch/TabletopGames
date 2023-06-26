package games.wonders7;

import core.interfaces.IGameAttribute;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;
import games.wonders7.cards.Wonder7Card;

import java.util.function.*;



public enum Wonders7Attributes implements IGameAttribute {


    RAW((s, a) -> (s).cardsOfType(Wonder7Card.Wonder7CardType.RawMaterials)),
    MANUFACTURED((s, a) -> (s).cardsOfType(Wonder7Card.Wonder7CardType.ManufacturedGoods)),
    CIVILLIAN((s, a) -> (s).cardsOfType(Wonder7Card.Wonder7CardType.CivilianStructures)),
    SCIENTIFIC((s, a) -> (s).cardsOfType(Wonder7Card.Wonder7CardType.ScientificStructures)),
    COMMERCIAL((s, a) -> (s).cardsOfType(Wonder7Card.Wonder7CardType.CommercialStructures)),
    MILITARY((s, a) -> (s).cardsOfType(Wonder7Card.Wonder7CardType.MilitaryStructures));
    ;


    private final BiFunction<Wonders7GameState, Integer, Object> lambda;

    Wonders7Attributes(BiFunction<Wonders7GameState, Integer, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(AbstractGameState state, int player) {
        return lambda.apply((Wonders7GameState) state, player);
    }

}



