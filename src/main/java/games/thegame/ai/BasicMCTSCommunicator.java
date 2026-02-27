package games.thegame.ai;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.communication.Message;
import games.root.actions.choosers.ChooseNumber;
import games.thegame.TheGameGS;
import games.thegame.TheGameParameters;
import players.basicHueristicMCTS.BasicHeuristicMCTSPlayer;
import players.basicHueristicMCTS.BasicHeurTreeNode;
import players.comms.IPlayerCommunicator;

import java.util.*;
import java.util.stream.Collectors;

public class BasicMCTSCommunicator implements IPlayerCommunicator {

    private SelectRowIntent ownIntent;
    private SelectRowIntent otherPlayerIntent;

    @Override
    public Message sendMessage(Game game, AbstractGameState state, List<AbstractAction> availableActions, AbstractPlayer emitter) {

        BasicHeuristicMCTSPlayer player = (BasicHeuristicMCTSPlayer) emitter;
        AbstractAction action = player._getAction(state, availableActions);
        TheGameParameters params = (TheGameParameters) state.getGameParameters();

        int playerToMove = state.getCurrentPlayer();
        int emitterId = emitter.getPlayerID();


        if(action.toString().contains("Select row"))
        {
            ownIntent = new SelectRowIntent();

            //All possible actions:
            for(int i = 0; i < params.numAscendingRows + params.numDescendingRows; ++i)
                ownIntent.cantRows.add(i);

            HashMap<Integer, Double> qVals = new HashMap<>();
            for(AbstractAction aa : player.root.children.keySet())
            {

                int row = -1;
                if(aa instanceof ChooseNumber cn)
                {
                    row = ((ChooseNumber) aa).number;
                } //else Otherwise is DoNothing - Pass, so row=-1.
                BasicHeurTreeNode tn = player.root.children.get(aa);
                double q = tn.totValue / tn.nVisits;
                qVals.put(row, q);
                ownIntent.cantRows.remove((Integer) row);
            }

                // Given the qVals for each action, sort the actions (rows to select) by descending order.
            ownIntent.intendedRows = qVals.entrySet()
                    .stream()
                    .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(ArrayList::new));



            return new Message(emitter.getPlayerID(), -1, Message.Receiver.All, ownIntent);

//            }else{
//                // I'm not the player to make a move. The player that moves sent me an intent, I should indicate
//                // how that looks to me.
//                int theirPreferredRow = intent.intendedRows.getFirst();
//
//
//            }


        }


        return new Message(emitter.getPlayerID(), -1, Message.Receiver.All, action);
    }

    @Override
    public Message sendMessage(Game game, AbstractGameState state, AbstractAction action, AbstractPlayer emitter) {
        return null;
    }

    public void listen(AbstractGameState state, AbstractPlayer receiver, List<Message> messages){

        int playerID = receiver.getPlayerID();
        int currentPlayer = state.getCurrentPlayer();

        for(Message m : messages) {
            System.out.println("[" + receiver.getName() + "(" + receiver.getPlayerID() + ")] I've received a message (" +
                    m.getTick() + ":" + m.getSubtick()
                    + ") from player " +
                    m.from + ": " + m.msg);

            if(m.msg instanceof SelectRowIntent i)
            {
                this.otherPlayerIntent = i;
                if(playerID != currentPlayer) {
                    // Someone is telling me what they want to play
                    // Only listening here, shouldn't consume time dealing with it. Use sendMessage to search.

                }else{
                    //Someone is telling them their intention for me to play.
                    if(Objects.equals(ownIntent.intendedRows.getFirst(), otherPlayerIntent.intendedRows.getFirst()))
                    {
                        //We want the same row
                        System.out.println("MATCH! Both players want to play the same row: " + ownIntent.intendedRows.getFirst());
                    }else{
                        System.out.println("Mismatch: current player wants to play " + ownIntent.intendedRows.getFirst() +
                                " but other player prefers " + otherPlayerIntent.intendedRows.getFirst());
                    }


                }


                return; // Assumption: just one message to be dealt with
            }

        }


    }

}
