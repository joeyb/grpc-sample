package org.joeyb.grpc.sample;

import com.google.common.io.Resources;

import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContextBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.util.UUID;

public class ClientApplication {

    private static final String TEST_SERVER_HOST = "foo.test.google.fr";

    /**
     * Main client entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) throws CertificateException,
                                                  IOException,
                                                  InterruptedException,
                                                  URISyntaxException {
        File caCert = new File(Resources.getResource("grpc-certs/ca.pem").toURI());
        File clientCertChain = new File(Resources.getResource("grpc-certs/client.pem").toURI());
        File clientKey = new File(Resources.getResource("grpc-certs/client.key").toURI());

        SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient()
                .keyManager(clientCertChain, clientKey)
                .trustManager(caCert);

        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress("localhost", 10000)
                .negotiationType(NegotiationType.TLS)
                .overrideAuthority(TEST_SERVER_HOST)
                .sslContext(sslContextBuilder.build());

        ManagedChannel channel = channelBuilder.build();

        TestServiceGrpc.TestServiceBlockingStub stub = TestServiceGrpc.newBlockingStub(channel);

        TestResponse response = stub.unary(TestRequest.newBuilder().setMessage(UUID.randomUUID().toString()).build());

        System.out.println("Response message = " + response.getMessage());

        channel.shutdown();
    }
}
