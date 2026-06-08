# HandProj

Simple Android Jetpack Compose app for triggering a timed, repeating gate sequence.

## What it does

- Single screen with:
  - Cellular number input
  - `Duration:` input (minutes)
  - `Period` input (minutes)
  - `Open Gate` button
  - `Close Gate` button
  - Status display (`Running` / `Stopped ...`)
- `Open Gate` validates inputs and starts a repeating sequence.
- Each cycle opens the phone dialer with the provided number (`ACTION_DIAL`).
- Sequence stops when:
  - duration expires, or
  - `Close Gate` is pressed.

> Note: On modern Android, unattended repeated direct calls are restricted. This app uses dialer launch as a practical fallback.

## Build

```bash
./gradlew :app:assembleDebug
```

## Test

```bash
./gradlew :app:testDebugUnitTest
```
