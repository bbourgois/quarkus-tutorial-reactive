package com.bbo.service;

import com.bbo.model.Beer;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.annotations.Blocking;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.ApplicationScoped;
import javax.json.bind.JsonbBuilder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class BeerGenerator {
    private final Random random = new Random();
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
    @Retry(maxRetries = 10, delay = 1, delayUnit = ChronoUnit.SECONDS)
    public Multi<List<String>> group(Multi<String> stream) {
        int i = random.nextInt(10);
        System.out.println("Show retry  for random number "+i);
        if (i > 1) {
            throw new RuntimeException("not working");
        }
        return stream.skip().first(Duration.ofMillis(10)).group().intoLists().of(5);
    }


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
       // System.out.println(msg);
        return msg;
    }
}
