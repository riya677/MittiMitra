# MittiMitra World-Class Release Status

## 1) What changed immediately
- Client-side Groq/HuggingFace key paths removed from app logic.
- AI calls migrated to Firebase callable contract client (`BackendFunctionsClient`).
- Duplicate account checks moved to backend callable flow.
- Critical data isolation fixes applied for soil/docs/history/notifications.
- Notification posting now guarded for Android 13+ permission.

## 2) What now has stronger attention
- Added `domain` repository interfaces:
  - `PredictionRepository`
  - `UserProfileRepository`
  - `TaskRepository`
- Added `data` implementations for backend and Room task flow.
- Migrated major screens to `BaseActivity` for consistent language/theme behavior.

## 3) Prediction improvements implemented
- Structured response envelope and model data contracts for advisory/diagnosis/schedule/chat.
- Confidence and uncertainty rendering integrated in UI.
- Parser-safe fallback behavior implemented in backend and app.

## 4) New feature added
- `Farm Task Planner` added with:
  - New entities: `Field`, `FarmTask`, `TaskReminder`, `TaskLog`
  - New DAOs and migration `v7 -> v8`
  - New screen: `FarmTaskPlannerActivity`
  - New adapter/layouts/dialog for task CRUD-lite flow
  - Reminder integration in `NotificationWorker`
  - Auto-suggested tasks from crop schedule, plant diagnosis, and soil advisory

## 5) Play Store readiness added
- CI workflow added: `.github/workflows/android-ci.yml`
- Firebase Functions backend scaffold added under `functions/`
- Release checklist added: `Docs/PLAYSTORE_RELEASE_CHECKLIST.md`

## Remaining manual production steps
- Deploy Firebase Functions and set runtime secrets (`GROQ_API_KEY`).
- Configure release signing credentials in CI.
- Complete Play Console listing assets and Data Safety declaration.
- Run internal + closed track test cycles and resolve pre-launch issues.
