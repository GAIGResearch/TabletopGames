package core.communication;

import core.AbstractGameState;
import core.AbstractPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.List;

public class Blackboard {

    public enum CommHistory {
        LAST_TICK_ONLY, //Default
        ALL
    }

    private int lastTick;
    private int currentSubTick;
    private CommHistory commHistory;

    // First index: player id who received the message.
    // Second list: array list of messages received on the same game tick (find tick by message.getTick()).
    // Third list: all messages on that game tick (find tick by message.getSubtick()).
    private HashMap<Integer, ArrayList<ArrayList<Message>>> register;


    public Blackboard()
    {
        lastTick = -1;
        currentSubTick = 0;
        register = new HashMap<>();
        commHistory = CommHistory.LAST_TICK_ONLY;
    }

    private Blackboard(int tick, int subtick)
    {
        this.lastTick = tick;
        this.currentSubTick = subtick;
    }

    public void post(Message msg, AbstractGameState state) {
        //Add timestamp metadata to the message.
        if(msg.getTick() > lastTick){
            lastTick = msg.getTick();
            currentSubTick = 0;
        }else{
            currentSubTick++;
        }
        msg.setSubtick(currentSubTick);

        //Add to the registry depending on whom this message is for:
        switch (msg.receiver){
            case Player -> registerMessage(msg.to, msg);
            case All ->
            {
                for(int i = 0; i < state.getNPlayers(); i++)
                    if(i != msg.from) // "All" does not send message to itself
                        registerMessage(i, msg);
            }
            case Teammates -> {
                int senderTeam = state.getTeam(msg.from);
                for(int i = 0; i < state.getNPlayers(); i++)
                    if(senderTeam == state.getTeam(i) && i != msg.from) // "Teammates" sends to same team, but not to itself
                        registerMessage(i, msg);
            }

        }
    }

    public void registerMessage(int destPlayer, Message msg){
        if(!register.containsKey(destPlayer)) { // First message ever. Create structures and register it.
            register.put(destPlayer, new ArrayList<>());
            register.get(destPlayer).add(new ArrayList<>());
            register.get(destPlayer).getLast().add(msg);
            return; // we're done
        }

        ArrayList<ArrayList<Message>> messagesForThisPlayer = register.get(destPlayer); //should never be empty.

        Message lastMessage = messagesForThisPlayer.getLast().getLast();
        int lastTickRegistered = lastMessage.getTick();
        if(lastTickRegistered == msg.getTick()) // Same tick, another message. Add it to the list.
            messagesForThisPlayer.getLast().add(msg);
        else { //A new tick.
            messagesForThisPlayer.add(new ArrayList<>());
            messagesForThisPlayer.getLast().add(msg);
        }

        if(commHistory == CommHistory.LAST_TICK_ONLY && messagesForThisPlayer.size() > 1){
            messagesForThisPlayer.removeFirst();
        }
    }


    public void broadcastLast(List<AbstractPlayer> players, AbstractGameState observation) {
        for(AbstractPlayer player : players)
        {
            ArrayList<Message> lastMessages = pollLast(player.getPlayerID());
            if(lastMessages != null && player.parameters.comms != null && lastMessages.getFirst().getTick() == observation.getGameTick())
                player.parameters.comms.listen(observation, player, lastMessages);
        }
    }


    public ArrayList<Message> pollLast(int destPlayer)
    {
        ArrayList<ArrayList<Message>> messagesForThisPlayer = register.get(destPlayer);
        if(messagesForThisPlayer == null) return null;
        return messagesForThisPlayer.getLast();
    }

//    public Blackboard copy() {
//        Blackboard copy = new Blackboard(lastTick, currentSubTick);
//        copy.commHistory = commHistory;
//
//        copy.register = new HashMap<>();
//        for(Integer playerId : register.keySet())
//        {
//            copy.register.put(playerId, new ArrayList<>());
//            for(ArrayList<Message> tickMsgs : register.get(playerId))
//            {
//                copy.register.get(playerId).add(new ArrayList<>());
//                for(Message m : tickMsgs)
//                    copy.register.get(playerId).getLast().add(m.copy());
//            }
//        }
//
//        return copy;
//    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Blackboard that = (Blackboard) o;
        return lastTick == that.lastTick && currentSubTick == that.currentSubTick && commHistory == that.commHistory && Objects.equals(register, that.register);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastTick, currentSubTick, commHistory, register);
    }

    public void setCommHistory(CommHistory commHistory) {
        this.commHistory = commHistory;
    }
}
