-- ============================================================
-- Smart Exam Scheduler — Database Schema
-- Run this once to set up your database
-- ============================================================

CREATE DATABASE IF NOT EXISTS exam_scheduler;
USE exam_scheduler;

-- ------------------------------------------------------------
-- Core entity tables
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS teacher (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    department  VARCHAR(100) NOT NULL,
    email       VARCHAR(150) UNIQUE
);

CREATE TABLE IF NOT EXISTS room (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(50)  NOT NULL UNIQUE,
    capacity      INT          NOT NULL CHECK (capacity > 0),
    has_projector BOOLEAN      DEFAULT FALSE,
    building      VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS time_slot (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    exam_date   DATE        NOT NULL,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    CONSTRAINT chk_time CHECK (end_time > start_time),
    UNIQUE KEY uq_slot (exam_date, start_time)
);

CREATE TABLE IF NOT EXISTS student (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    roll_no   VARCHAR(30)  UNIQUE NOT NULL,
    semester  INT          NOT NULL,
    branch    VARCHAR(50)  NOT NULL
);

CREATE TABLE IF NOT EXISTS exam (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    subject_name     VARCHAR(150) NOT NULL,
    subject_code     VARCHAR(20)  NOT NULL UNIQUE,
    duration_minutes INT          NOT NULL DEFAULT 180,
    -- CORE = must schedule first; ELECTIVE = lower priority
    priority         ENUM('CORE','ELECTIVE') NOT NULL DEFAULT 'CORE',
    teacher_id       INT,
    FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE SET NULL
);

-- ------------------------------------------------------------
-- Enrollment: which students sit which exams
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS enrollment (
    student_id INT NOT NULL,
    exam_id    INT NOT NULL,
    PRIMARY KEY (student_id, exam_id),
    FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE,
    FOREIGN KEY (exam_id)    REFERENCES exam(id)    ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- Output table: the generated timetable
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS scheduled_exam (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    exam_id    INT NOT NULL UNIQUE,       -- one slot per exam
    slot_id    INT NOT NULL,
    room_id    INT NOT NULL,
    status     ENUM('SCHEDULED','CONFLICT','UNPLACED') DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (exam_id)  REFERENCES exam(id)      ON DELETE CASCADE,
    FOREIGN KEY (slot_id)  REFERENCES time_slot(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id)  REFERENCES room(id)      ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- Performance indexes (critical for conflict detection joins)
-- ------------------------------------------------------------

CREATE INDEX idx_enrollment_student ON enrollment(student_id);
CREATE INDEX idx_enrollment_exam    ON enrollment(exam_id);
CREATE INDEX idx_scheduled_slot     ON scheduled_exam(slot_id);
CREATE INDEX idx_scheduled_room     ON scheduled_exam(room_id);

-- ------------------------------------------------------------
-- Security, audit, and operations support tables
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS app_user (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(80) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled       BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS role (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS user_role (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS schedule_run (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    started_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at      TIMESTAMP NULL,
    status           ENUM('RUNNING','COMPLETED','FAILED') NOT NULL,
    hard_conflicts   INT NOT NULL DEFAULT 0,
    soft_penalty     INT NOT NULL DEFAULT 0,
    unplaced_exams   INT NOT NULL DEFAULT 0,
    trigger_username VARCHAR(80) NULL
);

CREATE TABLE IF NOT EXISTS schedule_audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_run_id BIGINT NULL,
    action_type     VARCHAR(50) NOT NULL,
    action_details  TEXT,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       VARCHAR(80) NOT NULL,
    actor_username  VARCHAR(80),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (schedule_run_id) REFERENCES schedule_run(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS exam_override (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id         INT NOT NULL,
    old_slot_id     INT NULL,
    old_room_id     INT NULL,
    new_slot_id     INT NOT NULL,
    new_room_id     INT NOT NULL,
    reason          VARCHAR(255) NOT NULL,
    approved_by     VARCHAR(80),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (exam_id) REFERENCES exam(id) ON DELETE CASCADE,
    FOREIGN KEY (old_slot_id) REFERENCES time_slot(id) ON DELETE SET NULL,
    FOREIGN KEY (old_room_id) REFERENCES room(id) ON DELETE SET NULL,
    FOREIGN KEY (new_slot_id) REFERENCES time_slot(id) ON DELETE CASCADE,
    FOREIGN KEY (new_room_id) REFERENCES room(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notification (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    title          VARCHAR(140) NOT NULL,
    message        TEXT NOT NULL,
    is_read        BOOLEAN DEFAULT FALSE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_schedule_run_status ON schedule_run(status);
CREATE INDEX idx_audit_entity ON schedule_audit_log(entity_type, entity_id);
CREATE INDEX idx_notification_user ON notification(user_id, is_read);

-- ------------------------------------------------------------
-- Sample data for testing
-- ------------------------------------------------------------

INSERT INTO teacher (name, department, email) VALUES
('Dr. Rajesh Kumar',  'Computer Science', 'rajesh@college.edu'),
('Prof. Sunita Sharma','Mathematics',      'sunita@college.edu'),
('Dr. Anil Verma',    'Physics',           'anil@college.edu'),
('Prof. Meena Gupta', 'Computer Science', 'meena@college.edu'),
('Dr. Vikram Singh',  'Electronics',      'vikram@college.edu');

INSERT INTO room (name, capacity, has_projector, building) VALUES
('Room A-101', 40,  TRUE,  'Block A'),
('Room A-102', 35,  FALSE, 'Block A'),
('Room B-201', 60,  TRUE,  'Block B'),
('Room B-202', 50,  FALSE, 'Block B'),
('Hall C-001', 120, TRUE,  'Block C');

INSERT INTO time_slot (exam_date, start_time, end_time) VALUES
('2025-05-10', '09:00:00', '12:00:00'),
('2025-05-10', '14:00:00', '17:00:00'),
('2025-05-11', '09:00:00', '12:00:00'),
('2025-05-11', '14:00:00', '17:00:00'),
('2025-05-12', '09:00:00', '12:00:00'),
('2025-05-12', '14:00:00', '17:00:00'),
('2025-05-13', '09:00:00', '12:00:00'),
('2025-05-13', '14:00:00', '17:00:00');

INSERT INTO exam (subject_name, subject_code, duration_minutes, priority, teacher_id) VALUES
('Data Structures',          'CS301', 180, 'CORE',     1),
('Mathematics III',          'MA301', 180, 'CORE',     2),
('Digital Electronics',      'EC301', 180, 'CORE',     5),
('Operating Systems',        'CS302', 180, 'CORE',     4),
('Engineering Physics',      'PH301', 180, 'CORE',     3),
('Web Technologies',         'CS401', 120, 'ELECTIVE', 1),
('Artificial Intelligence',  'CS402', 120, 'ELECTIVE', 4),
('Signal Processing',        'EC401', 120, 'ELECTIVE', 5);

INSERT INTO student (name, roll_no, semester, branch) VALUES
('Arjun Sharma',   '21CS001', 3, 'CSE'),
('Priya Singh',    '21CS002', 3, 'CSE'),
('Rohit Patel',    '21CS003', 3, 'CSE'),
('Kavya Reddy',    '21EC001', 3, 'ECE'),
('Amit Kumar',     '21EC002', 3, 'ECE'),
('Neha Joshi',     '21MA001', 3, 'IT'),
('Saurabh Gupta',  '21CS004', 3, 'CSE'),
('Anjali Mishra',  '21EC003', 3, 'ECE');

-- CSE students: DS, Maths, OS, WebTech, AI
INSERT INTO enrollment VALUES
(1,1),(1,2),(1,4),(1,6),(1,7),
(2,1),(2,2),(2,4),(2,6),(2,7),
(3,1),(3,2),(3,4),(3,6),
(7,1),(7,2),(7,4),(7,7);

-- ECE students: Maths, DigiElec, Physics, SignalProc
INSERT INTO enrollment VALUES
(4,2),(4,3),(4,5),(4,8),
(5,2),(5,3),(5,5),(5,8),
(8,2),(8,3),(8,5),(8,8);

-- IT student
INSERT INTO enrollment VALUES
(6,2),(6,5),(6,6);

INSERT INTO role (role_name) VALUES
('ROLE_ADMIN'),
('ROLE_TEACHER'),
('ROLE_STUDENT');