package calculator.client;

import com.proto.calculator.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CalculatorClient {
    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0) {
            System.out.println("Need at least 1 argument.");
            return;
        }

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 50052)
                .usePlaintext()
                .build();

        switch (args[0]) {
            //Unary
            case "sum":
                doSum(channel);
                break;

            //Server Streaming
            case "primes":
                doPrimes(channel);
                break;

            //Client Streaming
            case "avg":
                doAvg(channel);
                break;

            //Bi Directional Streaming
            case "max":
                doMax(channel);
                break;

            //Returns a Status.INVALID_ARGUMENT if the SqrtRequest.number is negative
            case "sqrt":
                doSqrt(channel);
                break;

            default:
                System.out.println("Keyword invalid: " + args[0]);

        }

        System.out.println("Shutting down");
        channel.shutdown();

    }

    private static void doSqrt(ManagedChannel channel) {
        System.out.println("In the doSqrt");

        CalculatorServiceGrpc.CalculatorServiceBlockingStub stub = CalculatorServiceGrpc.newBlockingStub(channel);
        SqrtResponse response = stub.sqrt(SqrtRequest.newBuilder().setNumber(4).build());
        System.out.println("The sqrt is : " + response.getResult());

        try {
            stub.sqrt(SqrtRequest.newBuilder().setNumber(-1).build());
        } catch (RuntimeException ex) {
            System.out.println("Got exception for sqrt");
            ex.printStackTrace();
        }
    }

    private static void doMax(ManagedChannel channel) throws InterruptedException {
        System.out.println("In the doMax");
        List<Integer> numbers = List.of(1, 50, 10, 4, 200, 67);

        CalculatorServiceGrpc.CalculatorServiceStub stub = CalculatorServiceGrpc.newStub(channel);
        CountDownLatch latch = new CountDownLatch(1);


        StreamObserver<MaxRequest> clientSender = stub.max(new StreamObserver<MaxResponse>() {
            @Override
            public void onNext(MaxResponse response) {
                System.out.println("The max is: " + response.getMax());
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });

        numbers.forEach(num -> {
            clientSender.onNext(MaxRequest.newBuilder().setNumber(num).build());
        });

        clientSender.onCompleted();
        latch.await(3, TimeUnit.SECONDS);
    }

    private static void doAvg(ManagedChannel channel) throws InterruptedException {
        System.out.println("In doAvg");
        CalculatorServiceGrpc.CalculatorServiceStub stub = CalculatorServiceGrpc.newStub(channel);

        List<Integer> numbers = List.of(1, 2, 3);
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<AvgRequest> clientSender = stub.avg(new StreamObserver<AvgResponse>() {
            @Override
            public void onNext(AvgResponse response) {
                System.out.println("The avg response is: " + response.getResult());
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });

        numbers.forEach(num -> clientSender.onNext(AvgRequest.newBuilder().setNumber(num).build()));

        clientSender.onCompleted();
        latch.await(3, TimeUnit.SECONDS);
    }

    private static void doPrimes(ManagedChannel channel) {
        System.out.println("In doPrimes");

        CalculatorServiceGrpc.CalculatorServiceBlockingStub stub = CalculatorServiceGrpc.newBlockingStub(channel);
        stub.primes(PrimeRequest.newBuilder().setNumber(567890).build())
                .forEachRemaining(response -> System.out.println("The prime responses are : " + response.getPrimeFactor()));
    }

    private static void doSum(ManagedChannel channel) {
        System.out.println("In doSum");
        CalculatorServiceGrpc.CalculatorServiceBlockingStub stub = CalculatorServiceGrpc.newBlockingStub(channel);
        SumResponse response = stub.sum(SumRequest.newBuilder().setFirstNumber(1).setSecondNumber(1).build());

        System.out.println("The sum is : " + response.getResult());

    }
}
