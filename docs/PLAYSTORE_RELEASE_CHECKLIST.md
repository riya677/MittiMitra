# MittiMitra Play Store Release Checklist

## 1) Compliance and trust
- [ ] Publish privacy policy URL and in-app link.
- [ ] Complete Google Play Data Safety form aligned with real data collection/processing.
- [ ] Ensure Aadhaar/Kisan IDs are masked in UI and never logged.
- [ ] Verify Firebase rules are deployed (`firestore.rules`).
- [ ] Enable Play Integrity and App Check in production.

## 2) Release build readiness
- [ ] Configure signing (`keystore.properties` + CI secrets).
- [ ] Build signed `.aab` from release pipeline.
- [ ] Validate target SDK/API level compliance (targetSdk 36 already configured).
- [ ] Run `lintDebug`, `testDebugUnitTest`, and `bundleRelease` successfully in CI.

## 3) Testing tracks
- [ ] Internal testing upload and smoke test all primary flows.
- [ ] Closed testing with real farmer personas and low-connectivity scenarios.
- [ ] Resolve all Pre-launch report crashes/ANRs.

## 4) Store listing assets
- [ ] App icon (512x512), feature graphic (1024x500), screenshots phone/tablet.
- [ ] Short description + full description localized for priority languages.
- [ ] Support email, website, and privacy policy links.

## 5) Final go/no-go gates
- [ ] Auth, duplicate account handling, and profile updates validated.
- [ ] Soil scan, plant diagnosis, crop schedule, and task reminders validated.
- [ ] Multi-user same-device data isolation validated.
- [ ] Notification permissions validated on Android 13+.
