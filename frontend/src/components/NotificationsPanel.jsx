function NotificationsPanel({
  notifications,
  onMarkRead,
  onDelete,
  canDelete,
  isActionPending,
  isLoading,
  errorMessage,
}) {
  if (isLoading) {
    return (
      <section className="surface-block">
        <h3>Notifications</h3>
        <div className="surface-empty">Loading notifications...</div>
      </section>
    );
  }

  if (errorMessage) {
    return (
      <section className="surface-block">
        <h3>Notifications</h3>
        <div className="surface-error">{errorMessage}</div>
      </section>
    );
  }

  return (
    <section className="surface-block">
      <h3>Notifications</h3>
      {!notifications.length && <div className="surface-empty">No notifications available.</div>}
      <div className="notification-list">
        {notifications.map((n) => (
          <article key={n.id} className={n.isRead ? "notification read" : "notification"}>
            <div>
              <h4 className="notification-title">{n.title}</h4>
              <p>{n.message}</p>
              <small>{new Date(n.createdAt).toLocaleString()}</small>
            </div>
            <div className="notification-actions">
              {!n.isRead && (
                <button className="ghost-btn" disabled={isActionPending} onClick={() => onMarkRead(n.id)}>
                  Mark Read
                </button>
              )}
              {canDelete && (
                <button className="ghost-btn" disabled={isActionPending} onClick={() => onDelete(n.id)}>
                  Remove
                </button>
              )}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

export default NotificationsPanel;
