function AnalyticsPanel({ data, isLoading, errorMessage }) {
  if (isLoading) {
    return (
      <section className="surface-block">
        <h3>Analytics</h3>
        <div className="surface-empty">Loading analytics...</div>
      </section>
    );
  }

  if (errorMessage) {
    return (
      <section className="surface-block">
        <h3>Analytics</h3>
        <div className="surface-error">{errorMessage}</div>
      </section>
    );
  }

  if (!data) {
    return (
      <section className="surface-block">
        <h3>Analytics</h3>
        <div className="surface-empty">No analytics available.</div>
      </section>
    );
  }

  return (
    <section className="surface-block">
      <h3>Analytics Overview</h3>
      <div className="metric-row">
        <article>
          <h4>Total Exams</h4>
          <strong>{data.totalExams}</strong>
        </article>
        <article>
          <h4>Scheduled</h4>
          <strong>{data.scheduledExams}</strong>
        </article>
        <article>
          <h4>Unplaced</h4>
          <strong>{data.unplacedExams}</strong>
        </article>
      </div>

      <div className="analytics-grid">
        <div>
          <h4>Room Utilization</h4>
          <ul className="mini-list">
            {(data.roomUtilization ?? []).map((x) => (
              <li key={x.name}>{x.name}: {x.count}</li>
            ))}
          </ul>
        </div>
        <div>
          <h4>Teacher Load</h4>
          <ul className="mini-list">
            {(data.teacherLoad ?? []).map((x) => (
              <li key={x.name}>{x.name}: {x.count}</li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

export default AnalyticsPanel;
