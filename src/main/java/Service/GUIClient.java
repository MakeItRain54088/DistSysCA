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
import generated.student.*;
import generated.course.*;
import generated.assessment.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class GUIClient extends JFrame {
    private ManagedChannel channel;
    private StudentServiceGrpc.StudentServiceBlockingStub studentStub;
    private StudentServiceGrpc.StudentServiceStub studentAsyncStub;
    private CourseServiceGrpc.CourseServiceBlockingStub courseStub;
    private CourseServiceGrpc.CourseServiceStub courseAsyncStub;
    private AssessmentServiceGrpc.AssessmentServiceBlockingStub assessmentStub;
    private AssessmentServiceGrpc.AssessmentServiceStub assessmentAsyncStub;

    private JTextArea logger;
    private StreamObserver<ChatMessage> chatObserver;

    public GUIClient() {
        initRpcConnection("localhost", 50051);
        setupInterface();
    }

    private void initRpcConnection(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        studentStub = StudentServiceGrpc.newBlockingStub(channel);
        studentAsyncStub = StudentServiceGrpc.newStub(channel);
        courseStub = CourseServiceGrpc.newBlockingStub(channel);
        courseAsyncStub = CourseServiceGrpc.newStub(channel);
        assessmentStub = AssessmentServiceGrpc.newBlockingStub(channel);
        assessmentAsyncStub = AssessmentServiceGrpc.newStub(channel);
    }

    private void setupInterface() {
        setTitle("Distributed Education System");
        setSize(1000, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Student Services", createStudentPanel());
        tabs.addTab("Course Services", createCoursePanel());
        tabs.addTab("Assessment Services", createAssessmentPanel());

        logger = new JTextArea(15, 80);
        logger.setBackground(new Color(33, 33, 33));
        logger.setForeground(new Color(100, 255, 100));
        logger.setFont(new Font("Consolas", Font.PLAIN, 12));
        logger.setEditable(false);

        add(tabs, BorderLayout.CENTER);
        add(new JScrollPane(logger), BorderLayout.SOUTH);
    }

    private JPanel createStudentPanel() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

        // 1. Unary: Register
        JPanel reg = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reg.setBorder(BorderFactory.createTitledBorder("Student Registration"));
        JTextField id = new JTextField(5), name = new JTextField(8), mail = new JTextField(10);
        JButton btn = new JButton("Register");
        btn.addActionListener(e -> {
            try {
                StudentResponse res = studentStub.registerStudent(StudentRequest.newBuilder()
                        .setStudentId(id.getText()).setName(name.getText()).setEmail(mail.getText()).build());
                postLog("Student: " + res.getMessage());
            } catch (Exception ex) { postLog("RPC Failed: " + ex.getMessage()); }
        });
        reg.add(new JLabel("ID:")); reg.add(id); reg.add(new JLabel("Name:")); reg.add(name); reg.add(btn);

        // 2. Server Stream: Attendance
        JPanel att = new JPanel(new FlowLayout(FlowLayout.LEFT));
        att.setBorder(BorderFactory.createTitledBorder("Attendance Records (Server Stream)"));
        JTextField sid = new JTextField(5);
        JButton attBtn = new JButton("Track Attendance");
        attBtn.addActionListener(e -> {
            studentAsyncStub.streamAttendance(StudentId.newBuilder().setStudentId(sid.getText()).build(), new StreamObserver<AttendanceRecord>() {
                @Override public void onNext(AttendanceRecord r) { postLog("Entry: " + r.getDate() + " | Status: " + r.getStatus()); }
                @Override public void onError(Throwable t) { postLog("Error: " + t.getMessage()); }
                @Override public void onCompleted() { postLog("--- Stream Finished ---"); }
            });
        });
        att.add(new JLabel("Student ID:")); att.add(sid); att.add(attBtn);

        // 3. Client Stream: Activity
        JPanel act = new JPanel(new FlowLayout(FlowLayout.LEFT));
        act.setBorder(BorderFactory.createTitledBorder("Activity Monitor (Client Stream)"));
        JTextField dur = new JTextField(3), topic = new JTextField(8);
        JButton send = new JButton("Send Log"), commit = new JButton("Commit All");
        final StreamObserver<ActivityLog>[] stream = new StreamObserver[1];
        send.addActionListener(e -> {
            if (stream[0] == null) {
                stream[0] = studentAsyncStub.uploadLearningActivity(new StreamObserver<UploadSummary>() {
                    @Override public void onNext(UploadSummary s) { postLog("Summary: " + s.getTotalDuration() + "m, " + s.getTotalSessions() + " sessions"); }
                    @Override public void onError(Throwable t) { stream[0] = null; }
                    @Override public void onCompleted() { stream[0] = null; }
                });
            }
            stream[0].onNext(ActivityLog.newBuilder().setStudentId(id.getText()).setDurationMinutes(Integer.parseInt(dur.getText())).setTopic(topic.getText()).build());
            postLog("Uploaded activity: " + topic.getText());
        });
        commit.addActionListener(e -> { if(stream[0] != null) stream[0].onCompleted(); });
        act.add(new JLabel("Mins:")); act.add(dur); act.add(new JLabel("Topic:")); act.add(topic); act.add(send); act.add(commit);

        main.add(reg); main.add(att); main.add(act);
        return main;
    }

    private JPanel createCoursePanel() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

        // 1. Unary: Create
        JPanel cre = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cre.setBorder(BorderFactory.createTitledBorder("Course Management"));
        JTextField cid = new JTextField(5), title = new JTextField(10);
        JButton btn = new JButton("Create Course");
        btn.addActionListener(e -> {
            try {
                CourseResponse r = courseStub.createCourse(CourseRequest.newBuilder()
                        .setCourseId(cid.getText()).setTitle(title.getText()).build());
                postLog("Course Service: " + r.getMessage());
            } catch (Exception ex) { postLog("Error: " + ex.getMessage()); }
        });
        cre.add(new JLabel("ID:")); cre.add(cid); cre.add(new JLabel("Title:")); cre.add(title); cre.add(btn);

        // 2. Server Stream: List
        JPanel listP = new JPanel(new FlowLayout(FlowLayout.LEFT));
        listP.setBorder(BorderFactory.createTitledBorder("Course Discovery (Server Stream)"));
        JButton listBtn = new JButton("Refresh Catalog");
        listBtn.addActionListener(e -> {
            postLog("Fetching course list...");
            courseStub.streamAvailableCourses(Empty.newBuilder().build()).forEachRemaining(c -> 
                postLog("Course: " + c.getTitle() + " (ID: " + c.getCourseId() + ")"));
        });
        listP.add(listBtn);

        // 3. Bi-Di Stream: Chat
        JPanel chat = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chat.setBorder(BorderFactory.createTitledBorder("Live Chat (Bi-Di)"));
        JTextField user = new JTextField(5), msg = new JTextField(15);
        JButton send = new JButton("Post");
        send.addActionListener(e -> {
            if (chatObserver == null) {
                chatObserver = courseAsyncStub.liveClassChat(new StreamObserver<ChatMessage>() {
                    @Override public void onNext(ChatMessage m) { postLog("[Chat] " + m.getSender() + ": " + m.getMessage()); }
                    @Override public void onError(Throwable t) { chatObserver = null; }
                    @Override public void onCompleted() { chatObserver = null; }
                });
            }
            chatObserver.onNext(ChatMessage.newBuilder().setSender(user.getText()).setMessage(msg.getText()).setTimestamp(new Date().toString()).build());
        });
        chat.add(new JLabel("User:")); chat.add(user); chat.add(msg); chat.add(send);

        main.add(cre); main.add(listP); main.add(chat);
        return main;
    }

    private JPanel createAssessmentPanel() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

        // 1. Unary: Submit Exam
        JPanel sub = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sub.setBorder(BorderFactory.createTitledBorder("Submit Exam"));
        JTextField sid = new JTextField(5), cid = new JTextField(5), ans = new JTextField(10);
        JButton btn = new JButton("Submit Answers");
        btn.addActionListener(e -> {
            try {
                GradeResponse res = assessmentStub.submitExam(ExamSubmission.newBuilder()
                        .setStudentId(sid.getText()).setCourseId(cid.getText()).setAnswers(ans.getText()).build());
                postLog("Grade Result: " + res.getGrade() + " | Feedback: " + res.getFeedback());
            } catch (Exception ex) { postLog("Error: " + ex.getMessage()); }
        });
        sub.add(new JLabel("SID:")); sub.add(sid); sub.add(new JLabel("CID:")); sub.add(cid); sub.add(ans); sub.add(btn);

        // 2. Server Stream: Grade Monitoring
        JPanel watch = new JPanel(new FlowLayout(FlowLayout.LEFT));
        watch.setBorder(BorderFactory.createTitledBorder("Grade Monitor (Server Stream)"));
        JTextField gSid = new JTextField(5);
        JButton watchBtn = new JButton("Watch Grades");
        watchBtn.addActionListener(e -> {
            assessmentAsyncStub.streamGrades(AssessmentStudentId.newBuilder().setStudentId(gSid.getText()).build(), new StreamObserver<GradeUpdate>() {
                @Override public void onNext(GradeUpdate g) { postLog("Grade Update: " + g.getCourseId() + " -> " + g.getGrade()); }
                @Override public void onError(Throwable t) { postLog("Error: " + t.getMessage()); }
                @Override public void onCompleted() { postLog("End of report."); }
            });
        });
        watch.add(new JLabel("Student ID:")); watch.add(gSid); watch.add(watchBtn);

        main.add(sub); main.add(watch);
        return main;
    }

    private void postLog(String text) {
        SwingUtilities.invokeLater(() -> logger.append("[" + new Date().toString().substring(11, 19) + "] " + text + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GUIClient().setVisible(true));
    }
}