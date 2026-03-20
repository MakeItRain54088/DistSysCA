package Service;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author st101
 */
import generated.course.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class CourseServiceImpl extends CourseServiceGrpc.CourseServiceImplBase {

    // store the course
    private List<Course> courseList = new ArrayList<>();

    // store the client
    private List<StreamObserver<ChatMessage>> clients = new ArrayList<>();


    // 1. Unary RPC - createCourse
    @Override
    public void createCourse(CourseRequest request, StreamObserver<CourseResponse> responseObserver) {

        // check empty
        if (request.getCourseId().isEmpty() || request.getTitle().isEmpty() || request.getInstructor().isEmpty()) {

            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("All fields must be filled")
                            .asRuntimeException()
            );
            return;
        }

        // check if exist
        for (Course c : courseList) {
            if (c.getCourseId().equals(request.getCourseId())) {

                responseObserver.onError(
                        Status.ALREADY_EXISTS
                                .withDescription("Course already exists")
                                .asRuntimeException()
                );
                return;
            }
        }

        // create course
        Course newCourse = Course.newBuilder()
                .setCourseId(request.getCourseId())
                .setTitle(request.getTitle())
                .setInstructor(request.getInstructor())
                .build();

        courseList.add(newCourse);

        CourseResponse response = CourseResponse.newBuilder()
                .setMessage("Course created successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    // 2. Server Streaming - streamAvailableCourses
    @Override
    public void streamAvailableCourses(Empty request, StreamObserver<Course> responseObserver) {

        // demo for no data
        if (courseList.isEmpty()) {

            courseList.add(
                    Course.newBuilder()
                            .setCourseId("C101")
                            .setTitle("Distributed Systems")
                            .setInstructor("Dr. Smith")
                            .build()
            );

            courseList.add(
                    Course.newBuilder()
                            .setCourseId("C102")
                            .setTitle("Cloud Computing")
                            .setInstructor("Dr. Lee")
                            .build()
            );
        }

        // send the data
        for (Course c : courseList) {
            responseObserver.onNext(c);
        }

        responseObserver.onCompleted();
    }


    // 3. Bidirectional Streaming - liveClassChat
    @Override
    public StreamObserver<ChatMessage> liveClassChat(StreamObserver<ChatMessage> responseObserver) {

        // add new client
        clients.add(responseObserver);

        return new StreamObserver<ChatMessage>() {

            @Override
            public void onNext(ChatMessage message) {

                // receive client's message  > broadcasst
                for (StreamObserver<ChatMessage> client : clients) {
                    client.onNext(message);
                }
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Client disconnected: " + t.getMessage());
                clients.remove(responseObserver);
            }

            @Override
            public void onCompleted() {
                //client leave
                clients.remove(responseObserver);
                responseObserver.onCompleted();
            }
        };
    }
}