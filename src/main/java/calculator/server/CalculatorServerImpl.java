package calculator.server;

import com.proto.calculator.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class CalculatorServerImpl extends CalculatorServiceGrpc.CalculatorServiceImplBase {
    //Unary
    @Override
    public void sum(SumRequest request,
                    StreamObserver<SumResponse> responseObserver) {
        int result = request.getFirstNumber() + request.getSecondNumber();
        responseObserver.onNext(SumResponse.newBuilder().setResult(result).build());
        responseObserver.onCompleted();
    }

    //Server Streaming
    @Override
    public void primes(PrimeRequest request,
                       StreamObserver<PrimeResponse> responseObserver) {
        int number = request.getNumber();
        int divisor = 2;

        while (number > 1) {
            //When we have a prime factor.
            if (number % divisor == 0) {
                number = number / divisor;
                //Return this response(prime factor) to the client
                responseObserver.onNext(PrimeResponse.newBuilder().setPrimeFactor(divisor).build());
            } else {
                //Don't have a prime factor increment the divisor.
                ++divisor;
            }

        }

        //Number less than 1. End
        responseObserver.onCompleted();
    }

    //Client Streaming
    @Override
    public StreamObserver<AvgRequest> avg(StreamObserver<AvgResponse> responseObserver) {
        return new StreamObserver<>() {
            double sum = 0;
            int count = 0;

            @Override
            public void onNext(AvgRequest request) {
                int number = request.getNumber();
                sum += number;
                ++count;
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                double avg = (sum / count);
                responseObserver.onNext(AvgResponse.newBuilder().setResult(avg).build());
                responseObserver.onCompleted();
            }
        };
    }

    //Bi Directional Streaming
    @Override
    public StreamObserver<MaxRequest> max(
            StreamObserver<MaxResponse> responseObserver) {

        return new StreamObserver<MaxRequest>() {
            int max = 0;

            @Override
            public void onNext(MaxRequest request) {
                if (request.getNumber() > max) {
                    max = request.getNumber();
                }
                responseObserver.onNext(MaxResponse.newBuilder().setMax(max).build());
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    //Returns a Status.INVALID_ARGUMENT if the SqrtRequest.number is negative
    @Override
    public void sqrt(SqrtRequest request, StreamObserver<SqrtResponse> responseObserver) {
        int number = request.getNumber();

        if (number < 0) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("The number cannot be negative")
                            .asRuntimeException());

            return;
        }

        int result = (int) Math.sqrt(number);
        responseObserver.onNext(SqrtResponse.newBuilder().setResult(result).build());
        responseObserver.onCompleted();
    }

}
