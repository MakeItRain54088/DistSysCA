package Service;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author st101
 */
import generated.assessment.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class AssessmentServiceImpl extends AssessmentServiceGrpc.AssessmentServiceImplBase {

    // store all the score
    private List<GradeUpdate> gradeList = new ArrayList<>();


    // 1. Unary RPC - submitExam
    @Override
    public void submitExam(ExamSubmission request,
                           StreamObserver<GradeResponse> responseObserver) {

        // check empty
        if (request.getStudentId().isEmpty() ||
            request.getCourseId().isEmpty() ||
            request.getAnswers().isEmpty()) {

            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("All fields must be filled")
                            .asRuntimeException()
            );
            return;
        }

        //grading
        double grade = request.getAnswers().length() % 100;

        //feedback
        String feedback;
        if (grade >= 70) {
            feedback = "Good job";
        } else if (grade >= 50) {
            feedback = "Pass";
        } else {
            feedback = "Need improvement";
        }

        // store the grade
        gradeList.add(
                GradeUpdate.newBuilder()
                        .setCourseId(request.getCourseId())
                        .setGrade(grade)
                        .build()
        );

        // return result
        GradeResponse response = GradeResponse.newBuilder()
                .setGrade(grade)
                .setFeedback(feedback)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    // 2. Server Streaming - streamGrades
    @Override
    public void streamGrades(AssessmentStudentId request,
                             StreamObserver<GradeUpdate> responseObserver) {

        String studentId = request.getStudentId();

        // demo data
        if (gradeList.isEmpty()) {

            gradeList.add(
                    GradeUpdate.newBuilder()
                            .setCourseId("C101")
                            .setGrade(75)
                            .build()
            );

            gradeList.add(
                    GradeUpdate.newBuilder()
                            .setCourseId("C102")
                            .setGrade(60)
                            .build()
            );
        }

        // send data（streaming）
        for (GradeUpdate g : gradeList) {
            responseObserver.onNext(g);
        }

        responseObserver.onCompleted();
    }
}