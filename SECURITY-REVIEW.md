# Code Review Findings

Review date: 2026-10-21

| # | Severity | File | Lines | Finding | Recommended action |
|---|---|---|---|---|---|
| 1 | Medium | `src/main/java/org/azdev/barber_book/notifications/services/WhatsAppNotificationService.java` | 44-47 | The Evolution API `sendText` request nests the message under `textMessage.text`; the configured endpoint expects the message in its supported top-level request shape. Notifications may be rejected. | Send `number` and `text` at the top level, matching the configured Evolution API version. |
| 2 | Medium | `src/main/resources/db/migration/V16__add_whatsapp_settings_and_professional_phone.sql` | 19 | The RLS policy reads `app.current_tenant`, while `TenantRLSAspect` sets `app.current_tenant_id`. Tenant-scoped access to `whatsapp_settings` may fail or not use the active tenant as intended. | Use the same tenant setting as the application and handle an unset setting safely, consistent with the existing RLS policies. |
| 3 | Medium | `src/main/java/org/azdev/barber_book/services/AppointmentService.java` | 91 | The appointment-created event is published before the surrounding transaction commits and processed asynchronously. A notification could be sent for an appointment whose transaction later rolls back. | Dispatch the notification only after the transaction commits. |
