package com.bbo.service;

import com.bbo.model.Beer;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.annotations.Blocking;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.JsonbBuilder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class BeerGenerator {

    @RestClient
    BeerService beerService;

    @Outgoing("beers")
    Multi<String> beers() {
        List<Beer> beers = beerService.getBeers(10);
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                .onOverflow().drop()
                .map(tick -> beers.get(ThreadLocalRandom.current().nextInt(0, beers.size())))
                .map(JsonbBuilder.create()::toJson);
    }

    @Incoming("beers")
    @Outgoing("groups")
    public Multi<List<String>> skipGroup(Multi<String> stream) {
        return stream.skip().first(Duration.ofMillis(10)).group().intoLists().of(5);
    }

/*
    @Incoming("groups")
    @Outgoing("messages")
    public String processGroup(List<String> list) {
        return String.join(",", list.toString());
    }
*/

    @Incoming("groups")
    @Outgoing("messages")
    @Blocking
    public String processGroup(List<String> list) {
        try {
            Thread.sleep(1000);
            System.out.println(LocalDateTime.now().toLocalTime());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return String.join(",", list.toString());
    }


    @Incoming("messages")
    public String print(String msg) {
        System.out.println(msg);
        return msg;
    }
}
