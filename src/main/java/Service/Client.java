package Service;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author st101
 */

import generated.student.*;
import generated.course.*;
import generated.assessment.*;
import generated.naming.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws Exception {

        // 1. Connect to Naming Service
        ManagedChannel namingChannel = ManagedChannelBuilder
                .forAddress("localhost", 50050)
                .usePlaintext()
                .build();

        NamingServiceGrpc.NamingServiceBlockingStub namingStub =
                NamingServiceGrpc.newBlockingStub(namingChannel);

        // 2. Discover Services
        ServiceInfo studentInfo = namingStub.discoverService(
                ServiceRequest.newBuilder().setServiceName("StudentService").build()
        );

        ServiceInfo courseInfo = namingStub.discoverService(
                ServiceRequest.newBuilder().setServiceName("CourseService").build()
        );

        ServiceInfo assessmentInfo = namingStub.discoverService(
                ServiceRequest.newBuilder().setServiceName("AssessmentService").build()
        );

        // 3. Connect to each service
        ManagedChannel studentChannel = ManagedChannelBuilder
                .forAddress(studentInfo.getHost(), studentInfo.getPort())
                .usePlaintext()
                .build();

        ManagedChannel courseChannel = ManagedChannelBuilder
                .forAddress(courseInfo.getHost(), courseInfo.getPort())
                .usePlaintext()
                .build();

        ManagedChannel assessmentChannel = ManagedChannelBuilder
                .forAddress(assessmentInfo.getHost(), assessmentInfo.getPort())
                .usePlaintext()
                .build();

        // 4. Create stubs (with deadline)
        StudentServiceGrpc.StudentServiceBlockingStub studentStub =
                StudentServiceGrpc.newBlockingStub(studentChannel)
                        .withDeadlineAfter(3, TimeUnit.SECONDS);

        CourseServiceGrpc.CourseServiceBlockingStub courseStub =
                CourseServiceGrpc.newBlockingStub(courseChannel)
                        .withDeadlineAfter(3, TimeUnit.SECONDS);

        CourseServiceGrpc.CourseServiceStub asyncCourseStub =
                CourseServiceGrpc.newStub(courseChannel);

        AssessmentServiceGrpc.AssessmentServiceBlockingStub assessmentStub =
                AssessmentServiceGrpc.newBlockingStub(assessmentChannel)
                        .withDeadlineAfter(3, TimeUnit.SECONDS);

        // 5. Student Service
        System.out.println("=== Register Student ===");

        StudentRequest student = StudentRequest.newBuilder()
                .setStudentId("S1")
                .setName("Alice")
                .setEmail("alice@email.com")
                .build();

        StudentResponse res = studentStub.registerStudent(student);
        System.out.println(res.getMessage());

        System.out.println("\n=== Attendance ===");

        studentStub.streamAttendance(
                StudentId.newBuilder().setStudentId("S1").build()
        ).forEachRemaining(a ->
                System.out.println(a.getDate() + " - " + a.getStatus())
        );

        // 6. Course Service
        System.out.println("\n=== Courses ===");

        courseStub.streamAvailableCourses(Empty.newBuilder().build())
                .forEachRemaining(c ->
                        System.out.println(c.getCourseId() + " - " + c.getTitle())
                );

        // 7. Assessment Service
        System.out.println("\n=== Submit Exam ===");

        GradeResponse grade = assessmentStub.submitExam(
                ExamSubmission.newBuilder()
                        .setStudentId("S1")
                        .setCourseId("C101")
                        .setAnswers("Sample answers")
                        .build()
        );

        System.out.println("Grade: " + grade.getGrade());
        System.out.println("Feedback: " + grade.getFeedback());

        // 8. Bidirectional Chat
        System.out.println("\n=== Chat (type exit) ===");

        StreamObserver<ChatMessage> chat =
                asyncCourseStub.liveClassChat(new StreamObserver<ChatMessage>() {

                    @Override
                    public void onNext(ChatMessage msg) {
                        System.out.println(msg.getSender() + ": " + msg.getMessage());
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Chat error: " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("Chat ended");
                    }
                });

        Scanner scanner = new Scanner(System.in);

        while (true) {
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                chat.onCompleted();
                break;
            }

            chat.onNext(
                    ChatMessage.newBuilder()
                            .setSender("Client")
                            .setMessage(input)
                            .setTimestamp(String.valueOf(System.currentTimeMillis()))
                            .build()
            );
        }

        // 9. Shutdown
        studentChannel.shutdown();
        courseChannel.shutdown();
        assessmentChannel.shutdown();
        namingChannel.shutdown();
    }
}