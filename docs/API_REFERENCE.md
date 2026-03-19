# API Reference (Final)

## Base URL
`/api/v1`

## Auth

### Login
- Method: `POST`
- Path: `/auth/login`
- Body:
```json
{
  "username": "admin",
  "password": "admin123"
}
```
- Response:
```json
{
  "token": "<jwt>",
  "username": "admin",
  "roles": ["ROLE_ADMIN"]
}
```

## Schedules

### Generate Schedule (Admin)
- Method: `POST`
- Path: `/schedules/generate`

### List Schedule (All Roles)
- Method: `GET`
- Path: `/schedules`
- Query Params: `status`, `teacher`, `subject`, `page`, `size`
- Response:
```json
{
  "items": [
    {
      "examId": 1,
      "subjectCode": "CS301",
      "subjectName": "Data Structures",
      "teacher": "Dr. Rajesh Kumar",
      "examDate": "2026-05-10",
      "startTime": "09:00",
      "endTime": "12:00",
      "room": "Room B-201",
      "status": "SCHEDULED",
      "conflictReason": ""
    }
  ],
  "page": 0,
  "size": 50,
  "total": 8
}
```

### Manual Override (Admin)
- Method: `POST`
- Path: `/schedules/override`
- Body:
```json
{
  "examId": 1,
  "newSlotId": 2,
  "newRoomId": 3,
  "reason": "Room maintenance"
}
```

### Export CSV
- Method: `GET`
- Path: `/schedules/export/csv`

### Export PDF
- Method: `GET`
- Path: `/schedules/export/pdf`

## Operations

### Audit Logs (Admin)
- Method: `GET`
- Path: `/audit-logs`
- Query Params: `page`, `size`

### Analytics Overview (Admin, Teacher)
- Method: `GET`
- Path: `/analytics/overview`

### Notifications (All Roles)
- Method: `GET`
- Path: `/notifications`
- Query Params: `page`, `size`

### Mark Notification Read
- Method: `PATCH`
- Path: `/notifications/{id}/read`

## Real-Time Events

### Schedule Event Stream
- Method: `GET`
- Path: `/events/schedules`
- Notes: Server-Sent Events stream. Pass token in `Authorization: Bearer ...` or query parameter `token`.

## Error Format

```json
{
  "code": "CONSTRAINT_VIOLATION",
  "message": "Teacher 'X' is already assigned to another exam in slot ...",
  "timestamp": "2026-03-17T12:00:00Z"
}
```
