package core.communication;

public class NullMessage extends Message{
    public NullMessage(){
        super(-1, -1, Receiver.None, "");
    }

    @Override
    public boolean isNull() {
        return true;
    }
}
