package Service;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author st101
 */

import Service.NamingServiceImpl;
import Service.StudentServiceImpl;
import Service.CourseServiceImpl;
import Service.AssessmentServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;


public class MyServer {

    public static void main(String[] args) {

        try {
            // 建立 server（port 50051）
            Server server = ServerBuilder.forPort(50051)

                    // 加入所有 services
                    .addService(new StudentServiceImpl())
                    .addService(new CourseServiceImpl())
                    .addService(new AssessmentServiceImpl())
                    .addService(new NamingServiceImpl())

                    .build();

            // start server
            server.start();
            System.out.println("Server started on port 50051");

            // keep server waiting for client
            server.awaitTermination();

        } catch (Exception e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
