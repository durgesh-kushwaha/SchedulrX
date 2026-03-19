function AuditPanel({ records, isLoading, errorMessage }) {
  if (isLoading) {
    return (
      <section className="surface-block">
        <h3>Audit Trail</h3>
        <div className="surface-empty">Loading audit history...</div>
      </section>
    );
  }

  if (errorMessage) {
    return (
      <section className="surface-block">
        <h3>Audit Trail</h3>
        <div className="surface-error">{errorMessage}</div>
      </section>
    );
  }

  return (
    <section className="surface-block">
      <h3>Audit Trail</h3>
      {!records.length && <div className="surface-empty">No audit records available.</div>}
      {!!records.length && (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Action</th>
                <th>Entity</th>
                <th>Actor</th>
                <th>Time</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              {records.map((record) => (
                <tr key={record.id}>
                  <td data-label="Action">{record.actionType || "-"}</td>
                  <td data-label="Entity">{record.entityType || "-"}</td>
                  <td data-label="Actor">{record.actorUsername || "-"}</td>
                  <td data-label="Time">
                    {record.createdAt ? new Date(record.createdAt).toLocaleString() : "-"}
                  </td>
                  <td data-label="Details">{record.actionDetails || "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default AuditPanel;
