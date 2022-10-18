package calculator.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;

import java.io.IOException;

public class CalculatorServer {
    public static void main(String[] args) throws InterruptedException, IOException {
        int port = 50052;

        Server server = ServerBuilder
                .forPort(port)
                .addService(ProtoReflectionService.newInstance())
                .addService(new CalculatorServerImpl())
                .build();

        server.start();

        System.out.println("Server started");
        System.out.println("Listening on port : " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Received shutdown request.");
            server.shutdown();
            System.out.println("Server stopped");
        }));

        server.awaitTermination();
    }
}
