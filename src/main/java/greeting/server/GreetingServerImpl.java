package greeting.server;

import com.proto.greeting.GreetingRequest;
import com.proto.greeting.GreetingResponse;
import com.proto.greeting.GreetingServiceGrpc;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class GreetingServerImpl extends GreetingServiceGrpc.GreetingServiceImplBase {

    @Override
    public void greet(GreetingRequest request, StreamObserver<GreetingResponse> responseObserver) {
        responseObserver.onNext(GreetingResponse.newBuilder().setResult("Hello " + request.getFirstName()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void greetManyTimes(GreetingRequest request,
                               StreamObserver<GreetingResponse> responseObserver) {

        //Send 10 responses back.
        GreetingResponse response = GreetingResponse.newBuilder().setResult("Hello " + request.getFirstName()).build();
        IntStream.range(0, 10).forEach(i -> responseObserver.onNext(response));
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<GreetingRequest> longGreet(
            StreamObserver<GreetingResponse> responseObserver) {

        StringBuilder sb = new StringBuilder();

        return new StreamObserver<>() {
            @Override
            public void onNext(GreetingRequest request) {
                sb.append("Hello ");
                sb.append(request.getFirstName());
                sb.append("!\n");
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(GreetingResponse.newBuilder().setResult(sb.toString()).build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<GreetingRequest> greetEveryone(
            StreamObserver<GreetingResponse> responseObserver) {


        return new StreamObserver<GreetingRequest>() {
            @Override
            public void onNext(GreetingRequest request) {
                responseObserver.onNext(GreetingResponse.newBuilder().setResult("Hello " + request.getFirstName()).build());
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


    @Override
    public void greetWithDeadline(GreetingRequest request,
                                  StreamObserver<GreetingResponse> responseObserver) {
        //Check if the request is cancelled.
        Context context = Context.current();

        IntStream.range(0, 3).forEach(i -> {
            if (context.isCancelled()) {
                return;
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                responseObserver.onError(e);
            }
        });

        responseObserver.onNext(GreetingResponse.newBuilder().setResult("Hello "+ request.getFirstName()).build());
        responseObserver.onCompleted();
    }
}
