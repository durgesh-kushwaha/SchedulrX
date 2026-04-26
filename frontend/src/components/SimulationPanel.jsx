import { useState } from "react";

function parseIds(raw) {
  if (!raw.trim()) {
    return [];
  }
  return raw
    .split(",")
    .map((item) => Number(item.trim()))
    .filter((id) => Number.isInteger(id) && id > 0);
}

function parseDates(raw) {
  if (!raw.trim()) {
    return [];
  }
  return raw
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function SimulationPanel({ onRun, data, isPending, errorMessage }) {
  const [alternatives, setAlternatives] = useState("3");
  const [minGapMinutes, setMinGapMinutes] = useState("120");
  const [strategy, setStrategy] = useState("HYBRID");
  const [blockedRoomIds, setBlockedRoomIds] = useState("");
  const [blockedSlotIds, setBlockedSlotIds] = useState("");
  const [blockedDates, setBlockedDates] = useState("");
  const [localError, setLocalError] = useState("");

  const submit = (event) => {
    event.preventDefault();

    const alt = Number(alternatives);
    const gap = Number(minGapMinutes);

    if (!Number.isInteger(alt) || alt < 1 || alt > 10) {
      setLocalError("Alternatives must be between 1 and 10");
      return;
    }

    if (!Number.isInteger(gap) || gap < 0) {
      setLocalError("Minimum gap must be 0 or higher");
      return;
    }

    setLocalError("");

    onRun({
      alternatives: alt,
      minGapMinutes: gap,
      strategy,
      blockedRoomIds: parseIds(blockedRoomIds),
      blockedSlotIds: parseIds(blockedSlotIds),
      blockedDates: parseDates(blockedDates),
    });
  };

  return (
    <section className="surface-block">
      <h3>What-If Simulation Lab</h3>
      <p>Generate multiple feasible alternatives without changing the live schedule.</p>

      <form className="simulation-form" onSubmit={submit}>
        <label>
          Alternatives
          <input
            type="number"
            min="1"
            max="10"
            value={alternatives}
            onChange={(event) => setAlternatives(event.target.value)}
          />
        </label>

        <label>
          Min Gap (minutes)
          <input
            type="number"
            min="0"
            value={minGapMinutes}
            onChange={(event) => setMinGapMinutes(event.target.value)}
          />
        </label>

        <label>
          Strategy
          <select value={strategy} onChange={(event) => setStrategy(event.target.value)}>
            <option value="HYBRID">HYBRID (greedy + repair + optimize)</option>
            <option value="GREEDY_ONLY">GREEDY_ONLY</option>
          </select>
        </label>

        <label>
          Blocked Room IDs
          <input
            value={blockedRoomIds}
            onChange={(event) => setBlockedRoomIds(event.target.value)}
            placeholder="e.g. 2,5,8"
          />
        </label>

        <label>
          Blocked Slot IDs
          <input
            value={blockedSlotIds}
            onChange={(event) => setBlockedSlotIds(event.target.value)}
            placeholder="e.g. 3,7"
          />
        </label>

        <label>
          Blocked Dates
          <input
            value={blockedDates}
            onChange={(event) => setBlockedDates(event.target.value)}
            placeholder="yyyy-mm-dd, yyyy-mm-dd"
          />
        </label>

        <button className="cta-btn" type="submit" disabled={isPending}>
          {isPending ? "Running Simulation" : "Run Simulation"}
        </button>
      </form>

      {(localError || errorMessage) && <div className="surface-error">{localError || errorMessage}</div>}

      {data && (
        <div className="simulation-output">
          <div className="simulation-meta">
            <span>Requested by: {data.requestedBy}</span>
            <span>Strategy: {data.strategy}</span>
            <span>Min gap: {data.minGapMinutes} min</span>
            <span>
              Alternatives: {data.generatedAlternatives}/{data.requestedAlternatives}
            </span>
          </div>

          {!data.alternatives?.length && (
            <div className="surface-empty">No alternative schedules could be generated for this scenario.</div>
          )}

          {(data.alternatives ?? []).map((alt) => (
            <article key={alt.rank} className="simulation-card">
              <header>
                <h4>Alternative #{alt.rank}</h4>
                <div className="simulation-kpis">
                  <span>Scheduled: {alt.scheduledExams}</span>
                  <span>Unplaced: {alt.unplacedExams}</span>
                  <span>Soft Penalty: {alt.softPenaltyScore}</span>
                  <span>Runtime: {alt.runtimeMs}ms</span>
                </div>
              </header>

              <div className="table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Time</th>
                      <th>Subject</th>
                      <th>Room</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(alt.rows ?? []).slice(0, 12).map((row) => (
                      <tr key={`${alt.rank}-${row.examId}-${row.examDate}-${row.startTime}`}>
                        <td data-label="Date">{row.examDate}</td>
                        <td data-label="Time">{row.startTime} - {row.endTime}</td>
                        <td data-label="Subject">{row.subjectCode} - {row.subjectName}</td>
                        <td data-label="Room">{row.room}</td>
                        <td data-label="Status">{row.status}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {(alt.rows?.length ?? 0) > 12 && (
                <small>Showing 12 of {alt.rows.length} rows for quick comparison.</small>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export default SimulationPanel;
