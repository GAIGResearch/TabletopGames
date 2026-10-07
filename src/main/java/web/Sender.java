package web;

import java.nio.ByteBuffer;

/**
 * Where a session's messages to the browser go.
 */
public interface Sender {

    void sendText(String json);

    void sendBinary(ByteBuffer data);

    boolean isOpen();

    void close();
}
