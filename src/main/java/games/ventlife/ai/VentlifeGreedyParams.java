package games.ventlife.ai;

import core.AbstractPlayer;
import core.interfaces.IStateHeuristic;
import players.PlayerConstants;
import players.PlayerParameters;

/**
 * Parameters of VentlifeGreedyPlayer. The budget is in milliseconds per decision.
 */
public class VentlifeGreedyParams extends PlayerParameters {

    IStateHeuristic heuristic = new VentlifeHeuristic();
    // how many of the best tile placements (by their one-step value) to play out to the end of the turn
    int tileLookahead = 10;
    // whether the play-outs go on through the other players' next turns, and how many of their seafloor placements
    // each considers (as well as every plateau placement)
    boolean opponentReplies = false;
    int replySeafloorSample = 20;
    // rounds of replies played out (the first by the other players only), and how many first creature placements
    // are tried after each tile
    int replyRounds = 1;
    int creatureAlternatives = 1;
    // how many first creature placements are played out (with opponentReplies) when choosing one; 1 for greedy
    int creatureLookahead = 1;

    public VentlifeGreedyParams() {
        addTunableParameter("heuristic", IStateHeuristic.class, new VentlifeHeuristic());
        addTunableParameter("tileLookahead", 10);
        addTunableParameter("opponentReplies", false);
        addTunableParameter("replySeafloorSample", 20);
        addTunableParameter("replyRounds", 1);
        addTunableParameter("creatureAlternatives", 1);
        addTunableParameter("creatureLookahead", 1);
        setParameterValue("budgetType", PlayerConstants.BUDGET_TIME);
        setParameterValue("budget", 64);
    }

    @Override
    public void _reset() {
        super._reset();
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        if (heuristic == null)
            heuristic = new VentlifeHeuristic();
        tileLookahead = (int) getParameterValue("tileLookahead");
        opponentReplies = (boolean) getParameterValue("opponentReplies");
        replySeafloorSample = (int) getParameterValue("replySeafloorSample");
        replyRounds = (int) getParameterValue("replyRounds");
        creatureAlternatives = (int) getParameterValue("creatureAlternatives");
        creatureLookahead = (int) getParameterValue("creatureLookahead");
    }

    @Override
    protected VentlifeGreedyParams _copy() {
        return new VentlifeGreedyParams();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof VentlifeGreedyParams;
    }

    @Override
    public AbstractPlayer instantiate() {
        return new VentlifeGreedyPlayer(this);
    }
}
