//#protocol
val channelBuilder =
  NettyChannelBuilder
    .forAddress("host", 50051)
    .sslContext(
      GrpcSslContexts.forClient()
        .trustManager(InsecureTrustManagerFactory.INSTANCE)
        .build()
    )
val grpcProtocol = grpc(channelBuilder)
//#protocol
