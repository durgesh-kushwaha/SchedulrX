import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import NotificationsPanel from "./NotificationsPanel";

const notifications = [
  {
    id: 1,
    title: "Schedule Generated",
    message: "A new schedule was generated.",
    createdAt: "2026-04-09T05:00:00.000Z",
    isRead: false,
  },
];

describe("NotificationsPanel", () => {
  it("shows remove button for admins and triggers callback", () => {
    const onDelete = vi.fn();
    const onMarkRead = vi.fn();

    render(
      <NotificationsPanel
        notifications={notifications}
        onMarkRead={onMarkRead}
        onDelete={onDelete}
        canDelete
        isActionPending={false}
        isLoading={false}
        errorMessage=""
      />
    );

    fireEvent.click(screen.getByRole("button", { name: "Remove" }));

    expect(onDelete).toHaveBeenCalledTimes(1);
    expect(onDelete).toHaveBeenCalledWith(1);
  });

  it("hides remove button for non-admins", () => {
    render(
      <NotificationsPanel
        notifications={notifications}
        onMarkRead={vi.fn()}
        onDelete={vi.fn()}
        canDelete={false}
        isActionPending={false}
        isLoading={false}
        errorMessage=""
      />
    );

    expect(screen.queryByRole("button", { name: "Remove" })).toBeNull();
  });

  it("disables action buttons while request is pending", () => {
    render(
      <NotificationsPanel
        notifications={notifications}
        onMarkRead={vi.fn()}
        onDelete={vi.fn()}
        canDelete
        isActionPending
        isLoading={false}
        errorMessage=""
      />
    );

    expect(screen.getByRole("button", { name: "Mark Read" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Remove" })).toBeDisabled();
  });
});