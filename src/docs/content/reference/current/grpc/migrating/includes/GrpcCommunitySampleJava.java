//#protocol
NettyChannelBuilder channelBuilder =
  NettyChannelBuilder
    .forAddress("host", 50051)
    .sslContext(
      GrpcSslContexts.forClient()
        .trustManager(InsecureTrustManagerFactory.INSTANCE)
        .build()
    );
GrpcProtocolBuilder grpcProtocol = grpc(channelBuilder);
//#protocol
