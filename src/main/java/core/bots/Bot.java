package core.bots;

import core.AbstractGameState;
import core.Game;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.TextChannelCreateSpec;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import static evaluation.metrics.Event.GameEvent.ACTION_CHOSEN;

public class Bot implements IGameListener
{

    static GatewayDiscordClient client;

    static Guild server;

    private final static long serverID = 1250812734799347813L;

    private final int CONNECT_WAIT_TIME = 10000;

    private Game game; // Game being played.

    private TextChannel botChannel; //Main discord channel where this bot writes to.

    public Bot()
    {
        try (FileReader reader = new FileReader(".env")) {
            String discordToken = new BufferedReader(reader).readLine();
            client = DiscordClient.create(discordToken).login().block();

            client.on(ReadyEvent.class)
                    .flatMap(Bot::receiveMessage)
                    .subscribe();

            long now = System.currentTimeMillis();
            long stop = now + CONNECT_WAIT_TIME;
            while (server == null && System.currentTimeMillis() < stop);

            if(server == null)
            {
                // Default to snowflake id
                server = client.getGuildById(Snowflake.of(serverID)).block();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Publisher<String> receiveMessage(ReadyEvent event) {
        // Note this assumes we're in one server only. If not, we need Showflake ID to know which one I want.
        // This messafe should've been received CONNECT_TIME ms after constructor is called. If not, constructor
        // defaults to finding the server with the "serverID" provided.
        if(server == null)
//            server = client.getGuilds().cache().blockFirst();
                server = client.getGuildById(Snowflake.of(serverID)).block();
        return Mono.empty();
    }


    public void init(Game game, int nPlayersPerGame, Set<String> playerNames)
    {
        this.game = game;
        Calendar c = Calendar.getInstance();
        String gameName = game.getGameType().name();

        String channelName = gameName + "_" + c.get(Calendar.DAY_OF_MONTH) + "_" + (c.get(Calendar.MONTH)+1) + "_" + c.get(Calendar.YEAR) ;
        TextChannelCreateSpec spec = TextChannelCreateSpec.builder()
                .name(channelName)
                .build();
        botChannel = server.createTextChannel(spec).block();

        StringBuilder output = new StringBuilder("Welcome of this " + nPlayersPerGame + "-player game of " + gameName + ". Players are: \n");
        for(String name : playerNames)
            output.append("-").append(name).append("\n");

        botChannel.createMessage(output.toString()).block();
    }


    @Override
    public void onEvent(Event event) {
        if(event.type == ACTION_CHOSEN)
        {
            AbstractGameState state = event.state;
            String player = game.getPlayers().get(state.getCurrentPlayer()).toString();
            StringBuilder sb = new StringBuilder("[Round: " + state.getRoundCounter() + ", Turn: " + state.getTurnCounter() +"]\n");
            sb.append(player).append(" plays ").append(event.action);
            botChannel.createMessage(sb.toString()).block();
        }
    }

    @Override
    public void report() {

    }

    @Override
    public void setGame(Game game) {

    }

    @Override
    public Game getGame() {
        return null;
    }
}
