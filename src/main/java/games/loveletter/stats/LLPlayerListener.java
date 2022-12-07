package games.loveletter.stats;
import core.AbstractGameState;
import core.actions.AbstractAction;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.GameListener;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.BaronAction;
import games.loveletter.actions.GuardAction;
import games.loveletter.actions.PrinceAction;
import games.loveletter.actions.PrincessAction;
import utilities.Utils;
public class LLPlayerListener extends GameListener {

    IStatisticLogger[] loggerArray;

    String winningCards;
    String losingCards;

    public LLPlayerListener(IStatisticLogger[] loggerArray, IStatisticLogger aggregate) {
        super(aggregate, new AbstractMetric[]{});
        this.loggerArray = loggerArray;
        winningCards = null;
        losingCards = null;
    }

    private String processCards(String token, int playerID)
    {
        StringBuilder ss = new StringBuilder();

        if(token != null)
            ss.append(token).append(",");
        else
            ss.append("NA,");

        return ss.toString();
    }


    @Override
    public void onEvent(Event event) {

        if(event.type == Event.GameEvent.GAME_OVER) {
            //TODO: This needs work.
            for (int i = 0; i < event.state.getNPlayers(); i++) {
//                Map<String, Object> data = Arrays.stream(LLPlayerAttributes.values())
//                        .collect(Collectors.toMap(IGameMetric::name, attr -> attr.get(this, event)));
                String wins = processCards(winningCards, i);
//                data.put("WINS_REASON", wins);
                String losses = processCards(losingCards, i);
//                data.put("LOSE_REASON", losses);
//                loggerArray[i].record(data);
//                loggers.get(event.type).record(data);
            }
            winningCards = null;
            losingCards = null;
        }else if(event.type == Event.GameEvent.ACTION_TAKEN){
            processAction(event.state, event.action);
        }
    }


    public void processAction(AbstractGameState state, AbstractAction action) {

        //TODO: I'm not sure this is called.
        if((state.getGameStatus() == Utils.GameResult.GAME_END))
        {
            winningCards = "NA";
            losingCards = "NA";

            LoveLetterGameState llgs = ((LoveLetterGameState) state);
            int currentPlayerID = llgs.getCurrentPlayer();
            boolean drawPileEmpty = llgs.getDrawPile().getSize() == 0;
            String lastAction = llgs.getHistoryAsText().get(llgs.getHistoryAsText().size()-1);
            String[] tokens = lastAction.split(" ");
            int whoPlayedIt = Integer.parseInt(tokens[1]);

            boolean playAndWin = llgs.getPlayerResults()[whoPlayedIt] == Utils.GameResult.WIN;

            if(action instanceof PrincessAction)
            {
                losingCards = ("Princess");
                winningCards  = ("Princess (opp)");

            }else if(drawPileEmpty)
            {
                if(action instanceof GuardAction) {
                    int opponentID = ((GuardAction) action).getOpponentID();
                    boolean wonByCard = llgs.getPlayerHandCards().get(opponentID).getSize() == 0;
                    if (wonByCard) {
                        winningCards  = "Guard";
                        losingCards = "Guard (opp)";
                    }else{
                        getShowdownWin(llgs, whoPlayedIt, action);
                    }
                }else if(action instanceof BaronAction) {
                    int opponentID = ((BaronAction) action).getOpponentID();
                    boolean wonByCard = llgs.getPlayerHandCards().get(opponentID).getSize() == 0;
                    if (wonByCard) {
                        winningCards = ("Baron");
                        losingCards  = ("Baron (opp)");
                    }else{
                        boolean lostByCard = llgs.getPlayerHandCards().get(whoPlayedIt).getSize() == 0;
                        if(lostByCard)
                        {
                            losingCards = ("Baron");
                            winningCards  = ("Baron (opp)");
                        }else
                        {
                            getShowdownWin(llgs, whoPlayedIt, action);
                        }
                    }
                }else {
                    getShowdownWin(llgs, whoPlayedIt, action);
                }
            }else if(action instanceof BaronAction)
            {
                if(playAndWin)
                {
                    winningCards  = ("Baron");
                    losingCards  = ("Baron (opp)");
                }else
                {
                    losingCards  = ("Baron");
                    winningCards  = ("Baron (opp)");
                }

            }else if(action instanceof GuardAction) {
                winningCards  = ("Guard");
                losingCards  = ("Guard (opp)");
            }else if(action instanceof PrinceAction)
            {
                if(llgs.getPlayerResults()[currentPlayerID] == Utils.GameResult.WIN) { //made the opponent discard princess.
                    winningCards = ("Prince");
                    losingCards = "Prince (opp)";
                }else{
                    losingCards =  "Prince";
                    winningCards = "Prince (opp)";
                }
            }else{
                winningCards  = (llgs.getHistoryAsText().get(llgs.getHistoryAsText().size()-1));
            }
        }
    }

    private void getShowdownWin(LoveLetterGameState llgs, int whoPlayedLast, AbstractAction action)
    {
        for(int i = 0; i < llgs.getPlayerResults().length; ++i)
        {
            int numCardsPlayerHand = llgs.getPlayerHandCards().get(i).getSize();
            if(llgs.getPlayerResults()[i] == Utils.GameResult.WIN && numCardsPlayerHand>0)
                winningCards = (llgs.getPlayerHandCards().get(i).get(0).cardType + " (end)");
            else if (llgs.getPlayerResults()[i] == Utils.GameResult.LOSE  && numCardsPlayerHand>0)
                losingCards = (llgs.getPlayerHandCards().get(i).get(0).cardType + " (end)");
            else{
                //This is a draw in the showdown.
                winningCards = "Tie-breaker";
                losingCards = "Tie-breaker";
                return;
            }
        }

    }

    @Override
    public void allGamesFinished() {
        for (IStatisticLogger log : loggerArray) {
            log.processDataAndFinish();
        }
        super.allGamesFinished();
    }
}
