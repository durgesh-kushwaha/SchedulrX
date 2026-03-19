function ScheduleGrid({ rows, isLoading, errorMessage, title }) {
  if (isLoading) {
    return (
      <section className="surface-block">
        <h3>{title}</h3>
        <div className="surface-empty">Loading schedule data...</div>
      </section>
    );
  }

  if (errorMessage) {
    return (
      <section className="surface-block">
        <h3>{title}</h3>
        <div className="surface-error">{errorMessage}</div>
      </section>
    );
  }

  if (!rows.length) {
    return (
      <section className="surface-block">
        <h3>{title}</h3>
        <div className="surface-empty">No exams match the current filters.</div>
      </section>
    );
  }

  return (
    <section className="surface-block">
      <h3>{title}</h3>
      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Date</th>
              <th>Time</th>
              <th>Subject</th>
              <th>Teacher</th>
              <th>Room</th>
              <th>Status</th>
              <th>Conflict</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={`${row.examId}-${row.subjectCode}-${row.examDate}-${row.startTime}`}>
                <td data-label="ID">{row.examId}</td>
                <td data-label="Date">{row.examDate}</td>
                <td data-label="Time">{row.startTime} - {row.endTime}</td>
                <td data-label="Subject">{row.subjectCode} - {row.subjectName}</td>
                <td data-label="Teacher">{row.teacher}</td>
                <td data-label="Room">{row.room}</td>
                <td data-label="Status">
                  <span className={row.status === "SCHEDULED" ? "pill ok" : "pill bad"}>{row.status}</span>
                </td>
                <td data-label="Conflict">{row.conflictReason || "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export default ScheduleGrid;
