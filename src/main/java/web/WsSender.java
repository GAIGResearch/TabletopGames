package web;

import io.javalin.websocket.WsContext;

import java.nio.ByteBuffer;

/**
 * Sends to one WebSocket. The game, frame streamer and Swing threads all send, and a Jetty session must not be sent
 * to concurrently, so sending is synchronised.
 */
class WsSender implements Sender {

    private final WsContext ctx;

    WsSender(WsContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public synchronized void sendText(String json) {
        if (isOpen()) ctx.send(json);
    }

    @Override
    public synchronized void sendBinary(ByteBuffer data) {
        if (isOpen()) ctx.send(data);
    }

    @Override
    public synchronized void close() {
        if (isOpen()) ctx.closeSession();
    }

    @Override
    public boolean isOpen() {
        return ctx.session.isOpen();
    }
}
