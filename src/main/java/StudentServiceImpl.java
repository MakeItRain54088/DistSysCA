/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import generated.student.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

public class StudentServiceImpl extends StudentServiceGrpc.StudentServiceImplBase {

    // List for student data
    private List<StudentRequest> studentList = new ArrayList<>();
    //  attendance
    private List<AttendanceRecord> attendanceList = new ArrayList<>();


    // 1. Register Student (Unary RPC)
    @Override
    public void registerStudent(StudentRequest request, StreamObserver<StudentResponse> responseObserver) {

        // check to avoid empty value
        if (request.getStudentId().isEmpty() || request.getName().isEmpty() || request.getEmail().isEmpty()) {

            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("All fields must be filled")
                            .asRuntimeException()
            );
            return;
        }

        // check if student already exist
        for (StudentRequest s : studentList) {
            if (s.getStudentId().equals(request.getStudentId())) {

                responseObserver.onError(
                        Status.ALREADY_EXISTS
                                .withDescription("Student already exists")
                                .asRuntimeException()
                );
                return;
            }
        }

        // add new student
        studentList.add(request);

        StudentResponse response = StudentResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Student registered successfully")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // 2. Stream Attendance (Server Streaming)
    @Override
    public void streamAttendance(StudentId request, StreamObserver<AttendanceRecord> responseObserver) {

        String studentId = request.getStudentId();

        boolean found = false;

        // check if student exist
        for (StudentRequest s : studentList) {
            if (s.getStudentId().equals(studentId)) {
                found = true;
                break;
            }
        }

        // not exist > error
        if (!found) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Student not found")
                            .asRuntimeException()
            );
            return;
        }

        // demo data to avoid empty
        if (attendanceList.isEmpty()) {
            attendanceList.add(
                    AttendanceRecord.newBuilder()
                            .setDate("2026-03-01")
                            .setStatus("Present")
                            .build()
            );

            attendanceList.add(
                    AttendanceRecord.newBuilder()
                            .setDate("2026-03-02")
                            .setStatus("Absent")
                            .build()
            );
        }
        
        // send attendance （stream）
        for (AttendanceRecord record : attendanceList) {
            responseObserver.onNext(record);
        }

        responseObserver.onCompleted();
    }

    // 3. Upload Learning Activity (Client Streaming)
    @Override
    public StreamObserver<ActivityLog> uploadLearningActivity(
            StreamObserver<UploadSummary> responseObserver) {
        // a reciever
        return new StreamObserver<ActivityLog>() {

            int totalDuration = 0;
            int totalSessions = 0;

            @Override
            public void onNext(ActivityLog activity) {

                // check
                if (activity.getStudentId().isEmpty()) {
                    responseObserver.onError(
                            Status.INVALID_ARGUMENT
                                    .withDescription("Student ID missing")
                                    .asRuntimeException()
                    );
                    return;
                }

                // accumulate learning time
                totalDuration += activity.getDurationMinutes();
                totalSessions++;
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Error from client: " + t.getMessage());
            }

            @Override
            public void onCompleted() {

                // return all the result
                UploadSummary summary = UploadSummary.newBuilder()
                        .setTotalDuration(totalDuration)
                        .setTotalSessions(totalSessions)
                        .build();

                responseObserver.onNext(summary);
                responseObserver.onCompleted();
            }
        };
    }
}