# MittiMitra Firebase Functions

## Implemented callable endpoints
- `getSoilAdvisory`
- `getPlantDiagnosis`
- `getCropSchedule`
- `checkDuplicateAccount`
- `linkAccountIdentity`
- `getFarmerChatResponse`

All responses use the envelope shape:

```json
{
  "status": "success|error",
  "code": "STRING_CODE",
  "message": "Human readable message",
  "data": {},
  "traceId": "uuid"
}
```

## Required environment variables
Set before deploy:

```bash
firebase functions:config:set ai.groq_api_key="YOUR_GROQ_KEY"
```

Or set runtime env var directly in your deployment pipeline as:

- `GROQ_API_KEY`

## Deploy

```bash
cd functions
npm install
cd ..
firebase deploy --only functions,firestore:rules
```

## Security behavior
- Auth required for all callable endpoints.
- Basic per-user per-endpoint rate limiting (`60 calls/hour`).
- Structured errors with `traceId` for debugging.
