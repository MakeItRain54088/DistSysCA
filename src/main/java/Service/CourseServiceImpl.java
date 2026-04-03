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
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class CourseServiceImpl extends CourseServiceGrpc.CourseServiceImplBase {

    private List<Course> courses = new ArrayList<>();

    public void createCourse(Course request,
                             StreamObserver<CourseResponse> responseObserver) {

        courses.add(request);

        responseObserver.onNext(
                CourseResponse.newBuilder()
                        .setMessage("Course created")
                        .build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void streamAvailableCourses(Empty request,
                                       StreamObserver<Course> responseObserver) {

        for (Course c : courses) {
            responseObserver.onNext(c);
        }
        responseObserver.onCompleted();
    }

    // fix echo
    @Override
    public StreamObserver<ChatMessage> liveClassChat(
            StreamObserver<ChatMessage> responseObserver) {

        return new StreamObserver<ChatMessage>() {

            @Override
            public void onNext(ChatMessage msg) {

                responseObserver.onNext(
                        ChatMessage.newBuilder()
                                .setSender(msg.getSender())
                                .setMessage(msg.getMessage())
                                .setTimestamp(msg.getTimestamp())
                                .build()
                );
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }
}