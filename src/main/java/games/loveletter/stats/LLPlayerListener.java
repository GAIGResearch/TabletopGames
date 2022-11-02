package games.loveletter.stats;
import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IGameAttribute;
import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.BaronAction;
import games.loveletter.actions.GuardAction;
import games.loveletter.actions.PrinceAction;
import games.loveletter.actions.PrincessAction;
import games.loveletter.cards.LoveLetterCard;
import utilities.Utils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class LLPlayerListener implements IGameListener {

    IStatisticLogger[] logger;
    IStatisticLogger aggregate;

    String winningCards;
    String losingCards;

    public LLPlayerListener(IStatisticLogger[] logger, IStatisticLogger aggregate) {
        this.logger = logger;
        this.aggregate = aggregate;
        winningCards = null;
        losingCards = null;
    }
    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == CoreConstants.GameEvents.GAME_OVER) {
            AbstractGameState state = game.getGameState();
            for (int i = 0; i < state.getNPlayers(); i++) {
                final int playerID = i;
                Map<String, Object> data = Arrays.stream(LLPlayerListener.LLPlayerAttributes.values())
                        .collect(Collectors.toMap(IGameAttribute::name, attr -> attr.get(state, playerID)));

                String wins = processCards(winningCards, playerID);
                data.put("WINS_REASON", wins);

                String losses = processCards(losingCards, playerID);
                data.put("LOSE_REASON", losses);

                logger[i].record(data);
                aggregate.record(data);
            }

            winningCards = null;
            losingCards = null;
        }
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
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
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
        for (IStatisticLogger log : logger) {
            log.processDataAndFinish();
        }
        aggregate.processDataAndFinish();
    }


    public enum LLPlayerAttributes implements IGameAttribute {
        RESULT((s, p) -> s.getPlayerResults()[p].value),

        ACTIONS_PLAYED((s, p) -> {
            Deck<LoveLetterCard> played = s.getPlayerDiscardCards().get(p);

            StringBuilder ss = new StringBuilder();

            for (LoveLetterCard card : played.getComponents()) {
                ss.append(card.cardType).append(",");
            }
            if (ss.toString().equals("")) return ss.toString();
            ss.append("]");
            return ss.toString().replace(",]", "");
        }),

        ACTIONS_PLAYED_WIN((s, p) -> {
            StringBuilder ss = new StringBuilder();
            if(s.getPlayerResults()[p] == Utils.GameResult.WIN) {

                String lastHistory = s.getHistoryAsText().get(((ArrayList) s.getHistoryAsText()).size()-1);

                Deck<LoveLetterCard> played = s.getPlayerDiscardCards().get(p);
                if(played.getSize() == 0)
                {
                    //Won by play of the opponent.
                    ss.append("");
                }

                for (LoveLetterCard card : played.getComponents()) {
                    ss.append(card.cardType).append(",");
                }
                if (ss.toString().equals("")) return ss.toString();
                ss.append("]");
            }
            return ss.toString().replace(",]", "");
        });

        private final BiFunction<LoveLetterGameState, Integer, Object> lambda_sp;

        LLPlayerAttributes(BiFunction<LoveLetterGameState, Integer, Object> lambda) {
            this.lambda_sp = lambda;
        }

        public Object get(AbstractGameState state, int player) {
            return lambda_sp.apply((LoveLetterGameState) state, player);
        }

    }



}
