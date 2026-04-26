import { useEffect, useMemo, useState } from "react";

const EMPTY_DATASET = {
  config: {
    institutionName: "",
    termName: "",
    minGapMinutes: "120",
    maxExamsPerDay: "2",
  },
  teachers: [],
  rooms: [],
  slots: [],
  students: [],
  exams: [],
};

function parseNullableInt(value) {
  const raw = String(value ?? "").trim();
  if (!raw) {
    return null;
  }
  const number = Number(raw);
  return Number.isFinite(number) ? Math.trunc(number) : null;
}

function parseIdCsv(raw) {
  return String(raw ?? "")
    .split(",")
    .map((item) => Number(item.trim()))
    .filter((value) => Number.isInteger(value) && value > 0);
}

function toCsv(values) {
  return (values ?? []).join(", ");
}

function createTeacherRow() {
  return { id: "", name: "", department: "", email: "", unavailableSlotIdsText: "" };
}

function createRoomRow() {
  return {
    id: "",
    name: "",
    capacity: "",
    hasProjector: false,
    hasComputers: false,
    building: "",
    seatingType: "",
  };
}

function createSlotRow() {
  return { id: "", label: "", examDate: "", startTime: "", endTime: "" };
}

function createStudentRow() {
  return {
    id: "",
    name: "",
    rollNo: "",
    semester: "",
    branch: "",
    extraTimeMinutes: "",
    specialNeedsNotes: "",
  };
}

function createExamRow() {
  return {
    id: "",
    subjectName: "",
    subjectCode: "",
    durationMinutes: "",
    priority: "CORE",
    teacherId: "",
    department: "",
    examType: "THEORY",
    requiresProjector: false,
    requiresComputers: false,
    preferredSession: "MORNING",
    difficultyLevel: "3",
    studentIdsText: "",
  };
}

function toDraft(dataset) {
  const safe = dataset ?? EMPTY_DATASET;
  return {
    config: {
      institutionName: safe.config?.institutionName ?? "",
      termName: safe.config?.termName ?? "",
      minGapMinutes: safe.config?.minGapMinutes != null ? String(safe.config.minGapMinutes) : "120",
      maxExamsPerDay: safe.config?.maxExamsPerDay != null ? String(safe.config.maxExamsPerDay) : "2",
    },
    teachers: (safe.teachers ?? []).map((teacher) => ({
      id: teacher.id != null ? String(teacher.id) : "",
      name: teacher.name ?? "",
      department: teacher.department ?? "",
      email: teacher.email ?? "",
      unavailableSlotIdsText: toCsv(teacher.unavailableSlotIds),
    })),
    rooms: (safe.rooms ?? []).map((room) => ({
      id: room.id != null ? String(room.id) : "",
      name: room.name ?? "",
      capacity: room.capacity != null ? String(room.capacity) : "",
      hasProjector: !!room.hasProjector,
      hasComputers: !!room.hasComputers,
      building: room.building ?? "",
      seatingType: room.seatingType ?? "",
    })),
    slots: (safe.slots ?? []).map((slot) => ({
      id: slot.id != null ? String(slot.id) : "",
      label: slot.label ?? "",
      examDate: slot.examDate ?? "",
      startTime: slot.startTime ?? "",
      endTime: slot.endTime ?? "",
    })),
    students: (safe.students ?? []).map((student) => ({
      id: student.id != null ? String(student.id) : "",
      name: student.name ?? "",
      rollNo: student.rollNo ?? "",
      semester: student.semester != null ? String(student.semester) : "",
      branch: student.branch ?? "",
      extraTimeMinutes: student.extraTimeMinutes != null ? String(student.extraTimeMinutes) : "",
      specialNeedsNotes: student.specialNeedsNotes ?? "",
    })),
    exams: (safe.exams ?? []).map((exam) => ({
      id: exam.id != null ? String(exam.id) : "",
      subjectName: exam.subjectName ?? "",
      subjectCode: exam.subjectCode ?? "",
      durationMinutes: exam.durationMinutes != null ? String(exam.durationMinutes) : "",
      priority: exam.priority ?? "CORE",
      teacherId: exam.teacherId != null ? String(exam.teacherId) : "",
      department: exam.department ?? "",
      examType: exam.examType ?? "THEORY",
      requiresProjector: !!exam.requiresProjector,
      requiresComputers: !!exam.requiresComputers,
      preferredSession: exam.preferredSession ?? "MORNING",
      difficultyLevel: exam.difficultyLevel != null ? String(exam.difficultyLevel) : "3",
      studentIdsText: toCsv(exam.studentIds),
    })),
  };
}

function toPayload(draft) {
  const teachers = draft.teachers.map((teacher) => ({
    id: parseNullableInt(teacher.id),
    name: teacher.name.trim(),
    department: teacher.department.trim(),
    email: teacher.email.trim(),
    unavailableSlotIds: parseIdCsv(teacher.unavailableSlotIdsText),
  }));

  const rooms = draft.rooms.map((room) => ({
    id: parseNullableInt(room.id),
    name: room.name.trim(),
    capacity: parseNullableInt(room.capacity),
    hasProjector: !!room.hasProjector,
    hasComputers: !!room.hasComputers,
    building: room.building.trim(),
    seatingType: room.seatingType.trim(),
  }));

  const slots = draft.slots.map((slot) => ({
    id: parseNullableInt(slot.id),
    label: slot.label.trim(),
    examDate: slot.examDate.trim(),
    startTime: slot.startTime.trim(),
    endTime: slot.endTime.trim(),
  }));

  const students = draft.students.map((student) => ({
    id: parseNullableInt(student.id),
    name: student.name.trim(),
    rollNo: student.rollNo.trim(),
    semester: parseNullableInt(student.semester),
    branch: student.branch.trim(),
    extraTimeMinutes: parseNullableInt(student.extraTimeMinutes),
    specialNeedsNotes: student.specialNeedsNotes.trim(),
  }));

  const exams = draft.exams.map((exam) => ({
    id: parseNullableInt(exam.id),
    subjectName: exam.subjectName.trim(),
    subjectCode: exam.subjectCode.trim(),
    durationMinutes: parseNullableInt(exam.durationMinutes),
    priority: exam.priority,
    teacherId: parseNullableInt(exam.teacherId),
    department: exam.department.trim(),
    examType: exam.examType,
    requiresProjector: !!exam.requiresProjector,
    requiresComputers: !!exam.requiresComputers,
    preferredSession: exam.preferredSession,
    difficultyLevel: parseNullableInt(exam.difficultyLevel),
    studentIds: parseIdCsv(exam.studentIdsText),
  }));

  return {
    config: {
      institutionName: draft.config.institutionName.trim(),
      termName: draft.config.termName.trim(),
      minGapMinutes: parseNullableInt(draft.config.minGapMinutes),
      maxExamsPerDay: parseNullableInt(draft.config.maxExamsPerDay),
    },
    teachers,
    rooms,
    slots,
    students,
    exams,
  };
}

function RowEditor({ label, rows, columns, onAdd, onRemove, onChange, emptyMessage }) {
  return (
    <section className="surface-block planner-section">
      <div className="planner-section-head">
        <div>
          <h3>{label}</h3>
          <p>{emptyMessage}</p>
        </div>
        <button type="button" className="ghost-btn" onClick={onAdd}>Add Row</button>
      </div>

      {!rows.length && <div className="surface-empty">No rows yet.</div>}

      {!!rows.length && (
        <div className="table-wrap planner-table-wrap">
          <table className="data-table planner-table">
            <thead>
              <tr>
                {columns.map((column) => <th key={column.key}>{column.label}</th>)}
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row, index) => (
                <tr key={`row-${index}`}>
                  {columns.map((column) => (
                    <td key={column.key} data-label={column.label}>
                      {column.type === "select" && (
                        <select
                          value={row[column.key]}
                          onChange={(event) => onChange(index, column.key, event.target.value)}
                        >
                          {column.options.map((option) => (
                            <option key={option.value} value={option.value}>{option.label}</option>
                          ))}
                        </select>
                      )}
                      {column.type === "checkbox" && (
                        <input
                          type="checkbox"
                          checked={!!row[column.key]}
                          onChange={(event) => onChange(index, column.key, event.target.checked)}
                        />
                      )}
                      {(!column.type || column.type === "text" || column.type === "number") && (
                        <input
                          type={column.type === "number" ? "number" : "text"}
                          value={row[column.key]}
                          placeholder={column.placeholder ?? ""}
                          onChange={(event) => onChange(index, column.key, event.target.value)}
                        />
                      )}
                    </td>
                  ))}
                  <td data-label="Action">
                    <button type="button" className="ghost-btn" onClick={() => onRemove(index)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function SetupStudio({
  datasetResponse,
  templateResponse,
  onLoadTemplate,
  onSave,
  onGenerate,
  isLoading,
  isSaving,
  isGenerating,
  errorMessage,
  saveError,
  templateError,
  generateError,
}) {
  const [draft, setDraft] = useState(EMPTY_DATASET);
  const [dirty, setDirty] = useState(false);
  const [notice, setNotice] = useState("");

  useEffect(() => {
    if (datasetResponse?.dataset) {
      setDraft(toDraft(datasetResponse.dataset));
      setDirty(false);
    }
  }, [datasetResponse]);

  useEffect(() => {
    if (templateResponse?.dataset) {
      setDraft(toDraft(templateResponse.dataset));
      setDirty(true);
      setNotice("Starter template loaded into the editor. Save it to use it for generation.");
    }
  }, [templateResponse]);

  const readiness = datasetResponse?.readiness;
  const payloadPreview = useMemo(() => toPayload(draft), [draft]);

  const updateCollection = (key, index, field, value) => {
    setDraft((current) => ({
      ...current,
      [key]: current[key].map((row, rowIndex) => (rowIndex === index ? { ...row, [field]: value } : row)),
    }));
    setDirty(true);
  };

  const addRow = (key, factory) => {
    setDraft((current) => ({ ...current, [key]: [...current[key], factory()] }));
    setDirty(true);
  };

  const removeRow = (key, index) => {
    setDraft((current) => ({ ...current, [key]: current[key].filter((_, rowIndex) => rowIndex !== index) }));
    setDirty(true);
  };

  const saveDataset = () => {
    setNotice("");
    onSave(payloadPreview);
  };

  if (isLoading && !datasetResponse) {
    return (
      <section className="surface-block">
        <h3>Planning Studio</h3>
        <div className="surface-empty">Loading planning dataset...</div>
      </section>
    );
  }

  return (
    <div className="planner-layout">
      <section className="surface-block planner-intro">
        <div className="planner-intro-copy">
          <h3>Planning Studio</h3>
          <p>
            This is the missing step before schedule generation. The engine only works on the
            teachers, rooms, slots, students, and exams configured here.
          </p>
        </div>
        <div className="planner-actions">
          <button type="button" className="ghost-btn" onClick={onLoadTemplate}>Load Starter Data</button>
          <button type="button" className="cta-btn" disabled={isSaving} onClick={saveDataset}>
            {isSaving ? "Saving Dataset" : "Save Planning Dataset"}
          </button>
          <button
            type="button"
            className="cta-btn"
            disabled={isGenerating || !readiness?.ready || dirty}
            onClick={onGenerate}
          >
            {isGenerating ? "Generating" : dirty ? "Save First" : "Generate From Saved Data"}
          </button>
        </div>
      </section>

      {(notice || errorMessage || saveError || templateError || generateError) && (
        <div className={errorMessage || saveError || templateError || generateError ? "surface-error" : "live-banner"}>
          {errorMessage || saveError || templateError || generateError || notice}
        </div>
      )}

      <section className="surface-block planner-summary">
        <div className="planner-status">
          <div>
            <p className="hero-tag">Readiness</p>
            <h3>{readiness?.ready ? "Ready to Generate" : "Setup Required"}</h3>
          </div>
          <span className={readiness?.ready ? "pill ok" : "pill bad"}>
            {readiness?.ready ? "READY" : "BLOCKED"}
          </span>
        </div>

        <div className="metric-row">
          <article>
            <h4>Teachers</h4>
            <strong>{readiness?.teacherCount ?? 0}</strong>
          </article>
          <article>
            <h4>Rooms</h4>
            <strong>{readiness?.roomCount ?? 0}</strong>
          </article>
          <article>
            <h4>Slots</h4>
            <strong>{readiness?.slotCount ?? 0}</strong>
          </article>
          <article>
            <h4>Students</h4>
            <strong>{readiness?.studentCount ?? 0}</strong>
          </article>
          <article>
            <h4>Exams</h4>
            <strong>{readiness?.examCount ?? 0}</strong>
          </article>
          <article>
            <h4>Enrollments</h4>
            <strong>{readiness?.enrollmentCount ?? 0}</strong>
          </article>
        </div>

        <div className="planner-issues">
          <div>
            <h4>Blocking Issues</h4>
            {readiness?.blockingIssues?.length ? (
              <ul className="mini-list">
                {readiness.blockingIssues.map((issue) => <li key={issue}>{issue}</li>)}
              </ul>
            ) : (
              <div className="surface-empty">No blocking issues detected.</div>
            )}
          </div>
          <div>
            <h4>Advisory Notes</h4>
            {readiness?.advisoryNotes?.length ? (
              <ul className="mini-list">
                {readiness.advisoryNotes.map((note) => <li key={note}>{note}</li>)}
              </ul>
            ) : (
              <div className="surface-empty">No extra advisories right now.</div>
            )}
          </div>
        </div>
      </section>

      <section className="surface-block planner-config">
        <h3>Scheduling Rules</h3>
        <div className="planner-config-grid">
          <label>
            Institution Name
            <input
              value={draft.config.institutionName}
              onChange={(event) => {
                setDraft((current) => ({
                  ...current,
                  config: { ...current.config, institutionName: event.target.value },
                }));
                setDirty(true);
              }}
            />
          </label>
          <label>
            Term
            <input
              value={draft.config.termName}
              onChange={(event) => {
                setDraft((current) => ({
                  ...current,
                  config: { ...current.config, termName: event.target.value },
                }));
                setDirty(true);
              }}
            />
          </label>
          <label>
            Minimum Gap (minutes)
            <input
              type="number"
              min="0"
              value={draft.config.minGapMinutes}
              onChange={(event) => {
                setDraft((current) => ({
                  ...current,
                  config: { ...current.config, minGapMinutes: event.target.value },
                }));
                setDirty(true);
              }}
            />
          </label>
          <label>
            Max Exams Per Day
            <input
              type="number"
              min="1"
              value={draft.config.maxExamsPerDay}
              onChange={(event) => {
                setDraft((current) => ({
                  ...current,
                  config: { ...current.config, maxExamsPerDay: event.target.value },
                }));
                setDirty(true);
              }}
            />
          </label>
        </div>
      </section>

      <RowEditor
        label="Time Slots"
        rows={draft.slots}
        emptyMessage="Define the allowed exam windows first. Duration checks use these values directly."
        columns={[
          { key: "id", label: "ID", type: "number" },
          { key: "label", label: "Label" },
          { key: "examDate", label: "Date", placeholder: "2026-05-10" },
          { key: "startTime", label: "Start", placeholder: "09:00" },
          { key: "endTime", label: "End", placeholder: "12:00" },
        ]}
        onAdd={() => addRow("slots", createSlotRow)}
        onRemove={(index) => removeRow("slots", index)}
        onChange={(index, field, value) => updateCollection("slots", index, field, value)}
      />

      <RowEditor
        label="Teachers"
        rows={draft.teachers}
        emptyMessage="Teacher availability feeds the hard conflict rules. Use unavailable slot IDs to block specific windows."
        columns={[
          { key: "id", label: "ID", type: "number" },
          { key: "name", label: "Name" },
          { key: "department", label: "Department" },
          { key: "email", label: "Email" },
          { key: "unavailableSlotIdsText", label: "Unavailable Slot IDs", placeholder: "2, 6" },
        ]}
        onAdd={() => addRow("teachers", createTeacherRow)}
        onRemove={(index) => removeRow("teachers", index)}
        onChange={(index, field, value) => updateCollection("teachers", index, field, value)}
      />

      <RowEditor
        label="Rooms"
        rows={draft.rooms}
        emptyMessage="Room capacity and equipment directly affect hard-constraint feasibility."
        columns={[
          { key: "id", label: "ID", type: "number" },
          { key: "name", label: "Name" },
          { key: "capacity", label: "Capacity", type: "number" },
          { key: "building", label: "Building" },
          { key: "seatingType", label: "Seating Type" },
          { key: "hasProjector", label: "Projector", type: "checkbox" },
          { key: "hasComputers", label: "Computers", type: "checkbox" },
        ]}
        onAdd={() => addRow("rooms", createRoomRow)}
        onRemove={(index) => removeRow("rooms", index)}
        onChange={(index, field, value) => updateCollection("rooms", index, field, value)}
      />

      <RowEditor
        label="Students"
        rows={draft.students}
        emptyMessage="Student registrations are what make conflict detection real instead of random."
        columns={[
          { key: "id", label: "ID", type: "number" },
          { key: "name", label: "Name" },
          { key: "rollNo", label: "Roll No" },
          { key: "semester", label: "Semester", type: "number" },
          { key: "branch", label: "Branch" },
          { key: "extraTimeMinutes", label: "Extra Time", type: "number" },
          { key: "specialNeedsNotes", label: "Needs / Notes" },
        ]}
        onAdd={() => addRow("students", createStudentRow)}
        onRemove={(index) => removeRow("students", index)}
        onChange={(index, field, value) => updateCollection("students", index, field, value)}
      />

      <RowEditor
        label="Exams"
        rows={draft.exams}
        emptyMessage="Attach each exam to a teacher and list the enrolled student IDs so the solver can avoid overlaps."
        columns={[
          { key: "id", label: "ID", type: "number" },
          { key: "subjectCode", label: "Code" },
          { key: "subjectName", label: "Subject" },
          { key: "durationMinutes", label: "Duration", type: "number" },
          {
            key: "priority",
            label: "Priority",
            type: "select",
            options: [
              { value: "CORE", label: "CORE" },
              { value: "ELECTIVE", label: "ELECTIVE" },
            ],
          },
          { key: "teacherId", label: "Teacher ID", type: "number" },
          { key: "department", label: "Department" },
          {
            key: "examType",
            label: "Type",
            type: "select",
            options: [
              { value: "THEORY", label: "THEORY" },
              { value: "LAB", label: "LAB" },
              { value: "ONLINE", label: "ONLINE" },
            ],
          },
          {
            key: "preferredSession",
            label: "Preferred Session",
            type: "select",
            options: [
              { value: "MORNING", label: "MORNING" },
              { value: "AFTERNOON", label: "AFTERNOON" },
              { value: "EVENING", label: "EVENING" },
            ],
          },
          { key: "difficultyLevel", label: "Difficulty", type: "number" },
          { key: "requiresProjector", label: "Projector", type: "checkbox" },
          { key: "requiresComputers", label: "Computers", type: "checkbox" },
          { key: "studentIdsText", label: "Student IDs", placeholder: "1, 2, 3" },
        ]}
        onAdd={() => addRow("exams", createExamRow)}
        onRemove={(index) => removeRow("exams", index)}
        onChange={(index, field, value) => updateCollection("exams", index, field, value)}
      />
    </div>
  );
}

export default SetupStudio;
