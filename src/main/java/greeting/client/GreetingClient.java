package greeting.client;

import com.proto.greeting.GreetingRequest;
import com.proto.greeting.GreetingResponse;
import com.proto.greeting.GreetingServiceGrpc;
import io.grpc.*;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GreetingClient {

    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0) {
            System.out.println("Need at least one argument.");
            return;
        }

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        switch (args[0]) {
            case "greet":
                doGreet(channel);
                break;

            case "greet_many_times":
                doGreetManyTimes(channel);
                break;

            case "long_greet":
                doLongGreet(channel);
                break;

            case "greet_everyone":
                doGreetEveryone(channel);
                break;

            case "greet_with_deadline":
                doGreetWithDeadline(channel);
                break;

            default:
                System.out.println("Keyword invalid: " + args[0]);

        }
        System.out.println("Shutting down");
        channel.shutdown();
    }

    private static void doGreetWithDeadline(ManagedChannel channel) {
        System.out.println("Entered doGreetWithDeadline");

        GreetingServiceGrpc.GreetingServiceBlockingStub stub = GreetingServiceGrpc.newBlockingStub(channel);
        GreetingResponse response = stub
                .withDeadline(Deadline.after(4, TimeUnit.SECONDS))
                .greetWithDeadline(GreetingRequest.newBuilder().setFirstName("Foo").build());

        System.out.println("Greeting with deadline: " + response.getResult());

        try {
            stub.withDeadline(Deadline.after(1, TimeUnit.SECONDS))
                    .greetWithDeadline(GreetingRequest.newBuilder().setFirstName("Foo").build());
        } catch (StatusRuntimeException ex) {
            if (ex.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                System.out.println("Deadline has exceeded.");

            } else {
                System.out.println("Got an exception with doGreetWithDeadline: ");
                ex.printStackTrace();
            }
        }
    }

    private static void doGreetEveryone(ManagedChannel channel) throws InterruptedException {
        System.out.println("Entered doGreetEveryone");

        List<String> names = List.of("Foo", "Bar", "John", "Smith");

        GreetingServiceGrpc.GreetingServiceStub stub = GreetingServiceGrpc.newStub(channel);
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<GreetingRequest> senderClient = stub.greetEveryone(new StreamObserver<GreetingResponse>() {
            @Override
            public void onNext(GreetingResponse response) {
                System.out.println("The result is: " + response.getResult());
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });

        names.forEach(name -> senderClient.onNext(GreetingRequest.newBuilder().setFirstName(name).build()));

        senderClient.onCompleted();
        latch.await(3, TimeUnit.SECONDS);
    }

    private static void doLongGreet(ManagedChannel channel) throws InterruptedException {
        //Client Streaming
        System.out.println("Entered doLongGreet");

        //Asynchronous stub
        GreetingServiceGrpc.GreetingServiceStub stub = GreetingServiceGrpc.newStub(channel);
        List<String> names = List.of("Foo", "Bar", "John", "Smith");
        //Why not count 0.
        //We are in an async situation. Response from server can come at any time.
        //We use this latch to wait for the response from server;
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<GreetingRequest> clientLongGreetSender = stub.longGreet(new StreamObserver<GreetingResponse>() {
            @Override
            public void onNext(GreetingResponse response) {
                //Just print the response you get from the server.
                System.out.println("The result is: " + response.getResult());
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                //Make the latch to zero to quit the program.
                latch.countDown();
            }
        });

        names.forEach(name -> clientLongGreetSender.onNext(GreetingRequest.newBuilder().setFirstName(name).build()));

        clientLongGreetSender.onCompleted();
        latch.await(3, TimeUnit.SECONDS);
    }

    private static void doGreetManyTimes(ManagedChannel channel) {
        //Server Streaming
        System.out.println("Entered doGreetManyTimes");
        GreetingServiceGrpc.GreetingServiceBlockingStub stub = GreetingServiceGrpc.newBlockingStub(channel);

        stub.greetManyTimes(GreetingRequest.newBuilder().setFirstName("Foo").build()).forEachRemaining(response -> {
            System.out.println("Greeting " + response.getResult());
        });
    }

    private static void doGreet(ManagedChannel channel) {
        System.out.println("Entered doGreet");
        GreetingServiceGrpc.GreetingServiceBlockingStub stub = GreetingServiceGrpc.newBlockingStub(channel);

        //Client calling the method on the server.
        GreetingResponse response = stub.greet(GreetingRequest.newBuilder().setFirstName("Foo").build());

        System.out.println("Greeting " + response.getResult());
    }
}
