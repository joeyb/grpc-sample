package org.joeyb.grpc.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcServerRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestServiceImplTests {

    @Rule
    public final GrpcServerRule grpcServerRule = new GrpcServerRule();

    private int serverStreamingResponseCount;
    private TestServiceGrpc.TestServiceStub stub;

    @Before
    public void setUpGrpcServer() {
        serverStreamingResponseCount = ThreadLocalRandom.current().nextInt(10, 20);

        grpcServerRule.getServiceRegistry().addService(new TestServiceImpl(serverStreamingResponseCount));

        stub = TestServiceGrpc.newStub(grpcServerRule.getChannel());
    }

    @Test
    public void biDirectionalStreaming() throws InterruptedException {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        final int requestCount = ThreadLocalRandom.current().nextInt(10, 20);

        List<TestRequest> requests = IntStream.range(0, requestCount)
                .mapToObj(i -> TestRequest.newBuilder().setMessage(UUID.randomUUID().toString()).build())
                .collect(Collectors.toList());

        StreamObserver<TestRequest> requestObserver = stub.biDirectionalStreaming(
                new StreamObserver<TestResponse>() {

                    private int responseCount = 0;

                    @Override
                    public void onNext(TestResponse value) {
                        assertThat(responseCount).isLessThan(requestCount);
                        assertThat(value.getMessage()).contains(requests.get(responseCount).getMessage());

                        responseCount++;
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail("Unexpected error", t);
                    }

                    @Override
                    public void onCompleted() {
                        assertThat(responseCount).isEqualTo(requestCount);
                        completionLatch.countDown();
                    }
                });

        requests.forEach(requestObserver::onNext);
        requestObserver.onCompleted();

        if (!completionLatch.await(10, TimeUnit.SECONDS)) {
            fail("Request did not complete on time.");
        }
    }

    @Test
    public void biDirectionalStreamingError() throws InterruptedException {
        final CountDownLatch errorLatch = new CountDownLatch(1);
        final int requestCount = ThreadLocalRandom.current().nextInt(10, 20);

        List<TestRequest> requests = IntStream.range(0, requestCount)
                .mapToObj(i -> TestRequest.newBuilder().setMessage(UUID.randomUUID().toString()).build())
                .collect(Collectors.toList());

        StreamObserver<TestRequest> requestObserver = stub.biDirectionalStreamingError(
                new StreamObserver<TestResponse>() {

                    @Override
                    public void onNext(TestResponse value) {
                        fail("Unexpected response");
                    }

                    @Override
                    public void onError(Throwable t) {
                        requests.forEach(r -> assertThat(t.getMessage()).contains(r.getMessage()));
                        errorLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        fail("Unexpected completion");
                    }
                });

        requests.forEach(requestObserver::onNext);
        requestObserver.onCompleted();

        if (!errorLatch.await(10, TimeUnit.SECONDS)) {
            fail("Request did not complete on time.");
        }
    }

    @Test
    public void clientStreaming() throws InterruptedException {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        final int requestCount = ThreadLocalRandom.current().nextInt(10, 20);

        List<TestRequest> requests = IntStream.range(0, requestCount)
                .mapToObj(i -> TestRequest.newBuilder().setMessage(UUID.randomUUID().toString()).build())
                .collect(Collectors.toList());

        StreamObserver<TestRequest> requestObserver = stub.clientStreaming(
                new StreamObserver<TestResponse>() {

                    private int responseCount = 0;

                    @Override
                    public void onNext(TestResponse value) {
                        assertThat(responseCount).isLessThan(requestCount);

                        requests.forEach(r -> assertThat(value.getMessage()).contains(r.getMessage()));

                        responseCount++;
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail("Unexpected error", t);
                    }

                    @Override
                    public void onCompleted() {
                        assertThat(responseCount).isEqualTo(1);
                        completionLatch.countDown();
                    }
                });

        requests.forEach(requestObserver::onNext);
        requestObserver.onCompleted();

        if (!completionLatch.await(10, TimeUnit.SECONDS)) {
            fail("Request did not complete on time.");
        }
    }

    @Test
    public void clientStreamingError() throws InterruptedException {
        final CountDownLatch errorLatch = new CountDownLatch(1);
        final int requestCount = ThreadLocalRandom.current().nextInt(10, 20);

        List<TestRequest> requests = IntStream.range(0, requestCount)
                .mapToObj(i -> TestRequest.newBuilder().setMessage(UUID.randomUUID().toString()).build())
                .collect(Collectors.toList());

        StreamObserver<TestRequest> requestObserver = stub.clientStreamingError(
                new StreamObserver<TestResponse>() {

                    @Override
                    public void onNext(TestResponse value) {
                        fail("Unexpected response");
                    }

                    @Override
                    public void onError(Throwable t) {
                        requests.forEach(r -> assertThat(t.getMessage()).contains(r.getMessage()));
                        errorLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        fail("Unexpected completion");
                    }
                });

        requests.forEach(requestObserver::onNext);
        requestObserver.onCompleted();

        if (!errorLatch.await(10, TimeUnit.SECONDS)) {
            fail("Request did not complete on time.");
        }
    }

    @Test
    public void serverStreaming() throws InterruptedException {
        final CountDownLatch completionLatch = new CountDownLatch(1);

        TestRequest request = TestRequest.newBuilder().setMessage(UUID.randomUUID().toString()).build();

        stub.serverStreaming(
                request,
                new StreamObserver<TestResponse>() {

                    private int responseCount = 0;

                    @Override
                    public void onNext(TestResponse value) {
                        assertThat(responseCount).isLessThan(serverStreamingResponseCount);
                        assertThat(value.getMessage()).contains(request.getMessage());

                        responseCount++;
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail("Unexpected error", t);
                    }

                    @Override
                    public void onCompleted() {
                        assertThat(responseCount).isEqualTo(serverStreamingResponseCount);
                        completionLatch.countDown();
                    }
                });

        if (!completionLatch.await(10, TimeUnit.SECONDS)) {
            fail("Request did not complete on time.");
        }
    }

    @Test
    public void serverStreamingError() throws InterruptedException {
        final CountDownLatch errorLatch = new CountDownLatch(1);

        TestRequest request = TestRequest.newBuilder().setMessage(UUID.randomUUID().toString()).build();

        stub.serverStreamingError(
                request,
                new StreamObserver<TestResponse>() {
                    @Override
                    public void onNext(TestResponse value) {
                        fail("Unexpected response");
                    }

                    @Override
                    public void onError(Throwable t) {
                        assertThat(t.getMessage()).contains(request.getMessage());
                        errorLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        fail("Unexpected completion");
                    }
                });

        if (!errorLatch.await(10, TimeUnit.SECONDS)) {
            fail("Request did not complete on time.");
        }
    }

    @Test
    public void unary() throws InterruptedException {
        final CountDownLatch completionLatch = new CountDownLatch(1);

        TestRequest request = TestRequest.newBuilder().setMessage(UUID.randomUUID().toString()).build();

        stub.unary(
                request,
                new StreamObserver<TestResponse>() {

                    private int responseCount = 0;

                    @Override
                    public void onNext(TestResponse value) {
                        assertThat(responseCount).isLessThan(1);
                        assertThat(value.getMessage()).contains(request.getMessage());

                        responseCount++;
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail("Unexpected error", t);
                    }

                    @Override
                    public void onCompleted() {
                        assertThat(responseCount).isEqualTo(1);
                        completionLatch.countDown();
                    }
                });

        if (!completionLatch.await(10, TimeUnit.SECONDS)) {
            fail("Request did not complete on time.");
        }
    }

    @Test
    public void unaryError() throws InterruptedException {
        final CountDownLatch errorLatch = new CountDownLatch(1);

        TestRequest request = TestRequest.newBuilder().setMessage(UUID.randomUUID().toString()).build();

        stub.unaryError(
                request,
                new StreamObserver<TestResponse>() {
                    @Override
                    public void onNext(TestResponse value) {
                        fail("Unexpected response");
                    }

                    @Override
                    public void onError(Throwable t) {
                        assertThat(t.getMessage()).contains(request.getMessage());
                        errorLatch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        fail("Unexpected completion");
                    }
                });

        if (!errorLatch.await(10, TimeUnit.SECONDS)) {
            fail("Request did not complete on time.");
        }
    }
}
