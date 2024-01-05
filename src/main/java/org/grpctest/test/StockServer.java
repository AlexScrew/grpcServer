package org.grpctest.test;

import com.baeldung.grpc.streaming.Stock;
import com.baeldung.grpc.streaming.StockQuote;
import com.baeldung.grpc.streaming.StockQuoteProviderGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class StockServer {
    @Value("${stock.server.port}")
    private int port;
    private Server server;

    @EventListener(ApplicationReadyEvent.class)
    private void run() throws IOException, InterruptedException {
        server = ServerBuilder.forPort(port)
                .addService(new StockService())
                .build();
        start();
        if (server != null) {
            server.awaitTermination();
        }
    }


    private void start() throws IOException {
        server.start();
        log.info("Server started, listening on " + port);
        Runtime.getRuntime()
                .addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        System.err.println("shutting down server");
                        try {
                            StockServer.this.stop();
                        } catch (InterruptedException e) {
                            e.printStackTrace(System.err);
                        }
                        System.err.println("server shutted down");
                    }
                });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown()
                    .awaitTermination(30, TimeUnit.SECONDS);
        }
    }


//    @GRpcService
    private static class StockService extends StockQuoteProviderGrpc.StockQuoteProviderImplBase {

        StockService() {
        }

        @Override
        public void serverSideStreamingGetListStockQuotes(Stock request, StreamObserver<StockQuote> responseObserver) {

            for (int i = 1; i <= 5; i++) {

                StockQuote stockQuote = StockQuote.newBuilder()
                        .setPrice(fetchStockPriceBid(request))
                        .setOfferNumber(i)
                        .setDescription("Price for stock:" + request.getTickerSymbol())
                        .build();
                responseObserver.onNext(stockQuote);
            }
            responseObserver.onCompleted();
        }

        @Override
        public StreamObserver<Stock> clientSideStreamingGetStatisticsOfStocks(final StreamObserver<StockQuote> responseObserver) {
            return new StreamObserver<Stock>() {
                int count;
                double price = 0.0;
                StringBuffer sb = new StringBuffer();

                @Override
                public void onNext(Stock stock) {
                    count++;
                    price = +fetchStockPriceBid(stock);
                    sb.append(":")
                            .append(stock.getTickerSymbol());
                }

                @Override
                public void onCompleted() {
                    responseObserver.onNext(StockQuote.newBuilder()
                            .setPrice(price / count)
                            .setDescription("Statistics-" + sb.toString())
                            .build());
                    responseObserver.onCompleted();
                }

                @Override
                public void onError(Throwable t) {
                    log.warn("error:{}", t.getMessage());
                }
            };
        }

        @Override
        public StreamObserver<Stock> bidirectionalStreamingGetListsStockQuotes(final StreamObserver<StockQuote> responseObserver) {
            return new StreamObserver<Stock>() {
                @Override
                public void onNext(Stock request) {

                    for (int i = 1; i <= 5; i++) {

                        StockQuote stockQuote = StockQuote.newBuilder()
                                .setPrice(fetchStockPriceBid(request))
                                .setOfferNumber(i)
                                .setDescription("Price for stock:" + request.getTickerSymbol())
                                .build();
                        responseObserver.onNext(stockQuote);
                    }
                }

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }

                @Override
                public void onError(Throwable t) {
                    log.warn("error:{}", t.getMessage());
                }
            };
        }
    }

    private static double fetchStockPriceBid(Stock stock) {

        return stock.getTickerSymbol()
                .length()
                + ThreadLocalRandom.current()
                .nextDouble(-0.1d, 0.1d);
    }
}