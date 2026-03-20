package Service;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author st101
 */
import generated.naming.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class NamingServiceImpl extends NamingServiceGrpc.NamingServiceImplBase {

    // save all registered service
    private List<ServiceInfo> serviceList = new ArrayList<>();


    // 1. Register Service (Unary RPC)
    @Override
    public void registerService(ServiceInfo request,
                                StreamObserver<RegisterResponse> responseObserver) {

        // check empy
        if (request.getServiceName().isEmpty() ||
            request.getHost().isEmpty() ||
            request.getPort() == 0) {

            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Invalid service information")
                            .asRuntimeException()
            );
            return;
        }

        //check if exist
        for (ServiceInfo s : serviceList) {
            if (s.getServiceName().equals(request.getServiceName())) {

                responseObserver.onError(
                        Status.ALREADY_EXISTS
                                .withDescription("Service already registered")
                                .asRuntimeException()
                );
                return;
            }
        }

        // add service
        serviceList.add(request);

        RegisterResponse response = RegisterResponse.newBuilder()
                .setMessage("Service registered successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    // 2. Discover Service (Unary RPC)
    @Override
    public void discoverService(ServiceRequest request,
                                StreamObserver<ServiceInfo> responseObserver) {

        String name = request.getServiceName();

        // find service
        for (ServiceInfo s : serviceList) {
            if (s.getServiceName().equals(name)) {

                responseObserver.onNext(s);
                responseObserver.onCompleted();
                return;
            }
        }

        // find no service
        responseObserver.onError(
                Status.NOT_FOUND
                        .withDescription("Service not found")
                        .asRuntimeException()
        );
    }
}
