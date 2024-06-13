package extra;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public class BasicDiscordTest {

    public static void main(String[] args) {
        String discordToken = "TOKEN"; //TODO: Read from file that is not in repo

        DiscordClient client = DiscordClient.create(discordToken);

//        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) -> Mono.empty());
//
//        login.block();

//        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) ->
//                gateway.on(ReadyEvent.class, event ->
//                        Mono.fromRunnable(() -> {
//                            final User self = event.getSelf();
//                            System.out.printf("Logged in as %s#%s%n", self.getUsername(), self.getDiscriminator());
//                        })));

        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) ->
                gateway.on(MessageCreateEvent.class, event -> {
                    Message message = event.getMessage();

                    if (message.getContent().equalsIgnoreCase("!ping")) {
                        return message.getChannel()
                                .flatMap(channel -> channel.createMessage("pong!"));
                    }

                    return Mono.empty();
                }));

//        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) ->
//                gateway.on(MessageCreateEvent.class, event -> {
//                    Message message = event.getMessage();
//
//                    if (message.getContent().equalsIgnoreCase("!ping")) {
//                        return message.getChannel()
//                                .flatMap(channel -> channel.createMessage("pong!"));
//                    }
//
//                    return Mono.empty();
//                }));

        login.block();
    }



   /*  public static void main(String[] args) {
        //String discordToken = System.getenv("DISCORD_TOKEN");
        String discordToken = "TOKEN";

        // Create a new Discord client that logs in directly
        GatewayDiscordClient client = DiscordClient.create(discordToken).login().block();

        if (client == null) {
            System.out.println("Failed to login");
            return;
        }

        // then set it to listen for messages
        client.on(MessageCreateEvent.class)
                .flatMap(BasicDiscordTest::receiveMessage)
                .subscribe();

        // Block the program to keep it running
        client.onDisconnect().block();

        int  a= 0;
    }

    private static Publisher<String> receiveMessage(MessageCreateEvent event) {
        Message message = event.getMessage();
        String channelName = ((TextChannel) message.getChannel().block()).getName();
        String content = message.getContent();
        System.out.println("Received message: " + content + " in channel " + channelName);

        //return Mono.just(message.getChannel().flatMap(channel -> channel.createMessage("pong!")));
        return Mono.just("Point!");
        //return Mono.just(content);
    }*/


}