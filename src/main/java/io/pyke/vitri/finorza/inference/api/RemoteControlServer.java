package io.pyke.vitri.finorza.inference.api;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;

public class RemoteControlServer {
	private static RemoteControlServer INSTANCE;
	private static final int PORT = 24351;

	public static RemoteControlServer getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new RemoteControlServer();
		}
		return INSTANCE;
	}

	public Server start() throws IOException {
		return ServerBuilder.forPort(PORT).addService(new RemoteControlService())
			.addService(ProtoReflectionService.newInstance()).build().start();
	}
}
