package games.wonders7.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.actions.OneShotExtendedAction;
import games.wonders7.Wonders7ForwardModel;
import games.wonders7.Wonders7GameState;
import games.wonders7.cards.Wonder7Card;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class BuildFromDiscard extends OneShotExtendedAction {

    Wonders7ForwardModel fm = new Wonders7ForwardModel();
    public BuildFromDiscard(int player) {
        super("BuildFromDiscard", player,  state -> {
                    Wonders7GameState wgs = (Wonders7GameState) state;
                    List<Wonder7Card.CardType> available = wgs.getDiscardPile().stream()
                            .map(c -> c.cardType)
                            .distinct().collect(toList());
                    List<Wonder7Card.CardType> hasBuilt = wgs.getPlayedCards(player).stream()
                            .map(c -> c.cardType)
                            .distinct().toList();
                    available.removeAll(hasBuilt);
                    List<AbstractAction> actions = available.stream().map(c -> new PlayCard(player, c, true, true)).collect(toList());
                    if (actions.isEmpty()) {
                        actions.add(new DoNothing());
                    }
                    return actions;
                }
        );
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        super._afterAction(state, action);
        Wonders7GameState wgs = (Wonders7GameState) state;
        // we complete the bits that would have been done at the end of the Age in the FM._afterAction
        fm.rotateHands(wgs);
        fm.checkAgeEnd(wgs);
    }

    @Override
    public BuildFromDiscard copy() {
        BuildFromDiscard retValue = new BuildFromDiscard(player);
        retValue.executed = executed;
        return retValue;
    }

}
