package org.joeyb.grpc.sample;

import com.google.common.io.Resources;

import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.service.ProtoReflectionService;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ThreadLocalRandom;

public class ServerApplication {

    /**
     * Main server entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
        File caCert = new File(Resources.getResource("grpc-certs/ca.pem").toURI());
        File serverCertChain = new File(Resources.getResource("grpc-certs/server1.pem").toURI());
        File serverKey = new File(Resources.getResource("grpc-certs/server1.key").toURI());

        SslContextBuilder sslContextBuilder = GrpcSslContexts.forServer(serverCertChain, serverKey)
                .trustManager(caCert)
                .clientAuth(ClientAuth.REQUIRE);

        final NettyServerBuilder serverBuilder = NettyServerBuilder.forPort(10000)
                .sslContext(sslContextBuilder.build())
                .addService(ProtoReflectionService.getInstance())
                .addService(new TestServiceImpl(ThreadLocalRandom.current().nextInt(10, 20)));

        Server server = serverBuilder.build();

        server.start();

        System.out.println("Server running on port " + server.getPort());

        server.awaitTermination();
    }
}
