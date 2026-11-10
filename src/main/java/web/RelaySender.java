package web;

import java.nio.ByteBuffer;

/**
 * Sends a session's messages to its browser's current connection, which changes when the browser reconnects. Messages
 * are dropped while there is none.
 */
class RelaySender implements Sender {

    private volatile Sender connection;

    RelaySender(Sender connection) {
        this.connection = connection;
    }

    /**
     * Sends to this connection from now on. Returns the connection it replaces, or null.
     */
    synchronized Sender attach(Sender connection) {
        Sender previous = this.connection;
        this.connection = connection;
        return previous;
    }

    /**
     * Stops sending to the connection, if it is the current one, and returns whether it was. A replaced connection
     * may close after its replacement is attached.
     */
    synchronized boolean detach(Sender connection) {
        if (this.connection != connection) return false;
        this.connection = null;
        return true;
    }

    boolean isAttached() {
        return connection != null;
    }

    @Override
    public void sendText(String json) {
        Sender c = connection;
        if (c != null) c.sendText(json);
    }

    @Override
    public void sendBinary(ByteBuffer data) {
        Sender c = connection;
        if (c != null) c.sendBinary(data);
    }

    @Override
    public boolean isOpen() {
        Sender c = connection;
        return c != null && c.isOpen();
    }

    @Override
    public void close() {
        Sender c = connection;
        if (c != null) c.close();
    }
}
