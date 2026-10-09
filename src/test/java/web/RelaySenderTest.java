package web;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class RelaySenderTest {

    static class Recorder implements Sender {
        final List<String> texts = new ArrayList<>();
        boolean closed;

        @Override
        public void sendText(String json) {
            texts.add(json);
        }

        @Override
        public void sendBinary(ByteBuffer data) {
        }

        @Override
        public boolean isOpen() {
            return !closed;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Test
    public void messagesGoToTheCurrentConnectionAndAreDroppedWithoutOne() {
        Recorder first = new Recorder(), second = new Recorder();
        RelaySender relay = new RelaySender(first);
        relay.sendText("a");
        assertTrue(relay.detach(first));
        assertFalse(relay.isOpen());
        relay.sendText("lost");
        assertNull(relay.attach(second));
        relay.sendText("b");
        assertEquals(List.of("a"), first.texts);
        assertEquals(List.of("b"), second.texts);
    }

    @Test
    public void aReplacedConnectionClosingDoesNotDetachItsReplacement() {
        Recorder first = new Recorder(), second = new Recorder();
        RelaySender relay = new RelaySender(first);
        assertSame(first, relay.attach(second));
        assertFalse(relay.detach(first));
        assertTrue(relay.isAttached());
        relay.sendText("c");
        assertEquals(List.of("c"), second.texts);
    }
}
