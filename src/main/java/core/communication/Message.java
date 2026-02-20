package core.communication;

public class Message
{

    public enum Receiver{
        None,
        Player,
        All,
        Teammates
    }


    public int from; //player id
    public int to; //player id; ignored if Receiver != Player.
    public Receiver receiver;
    public final Object msg; //Actual message
    private int tick;
    private int subtick;

    public Message(int from, int to, Receiver rec, Object message)
    {
        this.from = from;
        this.to = to;
        this.receiver = rec;
        this.msg = message;
    }

    public void setTick(int tick) {
        this.tick = tick;
    }

    public int getTick() {
        return tick;
    }

    public void setSubtick(int subtick) {
        this.subtick = subtick;
    }

    public int getSubtick() {
        return subtick;
    }

    public boolean isNull() {return false;}


    public Message copy() {
        Message copy = new Message(from, to, receiver, msg);
        copy.tick = tick;
        copy.subtick = subtick;
        return copy;
    }

}
