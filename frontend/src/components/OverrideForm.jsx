import { useState } from "react";

function OverrideForm({ onSubmit, isPending, errorMessage }) {
  const [examId, setExamId] = useState("");
  const [newSlotId, setNewSlotId] = useState("");
  const [newRoomId, setNewRoomId] = useState("");
  const [reason, setReason] = useState("");
  const [localError, setLocalError] = useState("");

  const submit = (event) => {
    event.preventDefault();
    if (!examId || !newSlotId || !newRoomId || !reason.trim()) {
      setLocalError("All fields are required");
      return;
    }
    setLocalError("");
    onSubmit({
      examId: Number(examId),
      newSlotId: Number(newSlotId),
      newRoomId: Number(newRoomId),
      reason: reason.trim(),
    });
  };

  return (
    <section className="surface-block">
      <h3>Manual Override Workspace</h3>
      <p>Reassign an exam to a new slot and room with constraint validation.</p>

      <form className="override-form" onSubmit={submit}>
        <label>
          Exam ID
          <input type="number" min="1" value={examId} onChange={(e) => setExamId(e.target.value)} />
        </label>
        <label>
          New Slot ID
          <input type="number" min="1" value={newSlotId} onChange={(e) => setNewSlotId(e.target.value)} />
        </label>
        <label>
          New Room ID
          <input type="number" min="1" value={newRoomId} onChange={(e) => setNewRoomId(e.target.value)} />
        </label>
        <label className="wide">
          Reason
          <input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Reason for reassignment" />
        </label>

        <button className="cta-btn" type="submit" disabled={isPending}>
          {isPending ? "Applying" : "Apply Override"}
        </button>
      </form>

      {(localError || errorMessage) && <div className="surface-error">{localError || errorMessage}</div>}
    </section>
  );
}

export default OverrideForm;
