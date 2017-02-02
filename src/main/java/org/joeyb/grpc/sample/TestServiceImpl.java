package org.joeyb.grpc.sample;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class TestServiceImpl extends TestServiceGrpc.TestServiceImplBase {

    private final int serverStreamingResponseCount;

    public TestServiceImpl(int serverStreamingResponseCount) {
        this.serverStreamingResponseCount = serverStreamingResponseCount;
    }

    @Override
    public StreamObserver<TestRequest> biDirectionalStreaming(StreamObserver<TestResponse> responseObserver) {
        return new StreamObserver<TestRequest>() {
            @Override
            public void onNext(TestRequest value) {
                responseObserver.onNext(createResponse(value));
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
    public StreamObserver<TestRequest> biDirectionalStreamingError(StreamObserver<TestResponse> responseObserver) {
        return new ErrorResponseStreamObserver(responseObserver);
    }

    @Override
    public StreamObserver<TestRequest> clientStreaming(StreamObserver<TestResponse> responseObserver) {
        return new StreamObserver<TestRequest>() {

            private final LinkedList<TestRequest> requests = new LinkedList<>();

            @Override
            public void onNext(TestRequest value) {
                requests.add(value);
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(createResponse(requests));
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<TestRequest> clientStreamingError(StreamObserver<TestResponse> responseObserver) {
        return new ErrorResponseStreamObserver(responseObserver);
    }

    @Override
    public void serverStreaming(TestRequest request, StreamObserver<TestResponse> responseObserver) {
        for (int i = 0; i < serverStreamingResponseCount; i++) {
            responseObserver.onNext(createResponse(request));
        }

        responseObserver.onCompleted();
    }

    @Override
    public void serverStreamingError(TestRequest request, StreamObserver<TestResponse> responseObserver) {
        responseObserver.onError(createError(request));
    }

    @Override
    public void unary(TestRequest request, StreamObserver<TestResponse> responseObserver) {
        responseObserver.onNext(createResponse(request));
        responseObserver.onCompleted();
    }

    @Override
    public void unaryError(TestRequest request, StreamObserver<TestResponse> responseObserver) {
        responseObserver.onError(createError(request));
    }

    private static Throwable createError(TestRequest request) {
        return new StatusException(Status.UNKNOWN.withDescription("Error for " + request.getMessage()));
    }

    private static Throwable createError(Collection<TestRequest> requests) {
        String joinedMessages = requests.stream().map(TestRequest::getMessage).collect(Collectors.joining(", "));

        return new StatusException(Status.UNKNOWN.withDescription("Error for " + joinedMessages));
    }

    private static TestResponse createResponse(TestRequest request) {
        return TestResponse.newBuilder()
                .setMessage("Received " + request.getMessage())
                .build();
    }

    private static TestResponse createResponse(Collection<TestRequest> requests) {
        String joinedMessages = requests.stream().map(TestRequest::getMessage).collect(Collectors.joining(", "));

        return TestResponse.newBuilder()
                .setMessage("Received " + joinedMessages)
                .build();
    }

    private static class ErrorResponseStreamObserver implements StreamObserver<TestRequest> {

        private final LinkedList<TestRequest> requests;
        private final StreamObserver<TestResponse> responseObserver;

        private ErrorResponseStreamObserver(StreamObserver<TestResponse> responseObserver) {
            this.requests = new LinkedList<>();
            this.responseObserver = responseObserver;
        }

        @Override
        public void onNext(TestRequest value) {
            requests.add(value);
        }

        @Override
        public void onError(Throwable t) {
            responseObserver.onError(t);
        }

        @Override
        public void onCompleted() {
            responseObserver.onError(createError(requests));
        }
    }
}
