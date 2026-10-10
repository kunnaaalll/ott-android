# Local Testing Guide (Firebase Emulator Suite)

This guide describes how to run integration tests locally using the Firebase Emulator Suite. 
The integration tests interact with a locally emulated Firestore instance to verify security rules, rate limiting, and data integrity safely.

## Setup

1. **Install Firebase CLI:**
   ```bash
   npm install -g firebase-tools
   ```
2. **Install dependencies:**
   ```bash
   cd functions
   npm install
   ```

## Running the Tests

1. **Start the Emulators:**
   Open a terminal and start the Firebase emulators (Firestore & Auth). This will also build the TypeScript code.
   ```bash
   npm run serve
   ```
   *Keep this terminal window open.*

2. **Run the Integration Tests:**
   Open a second terminal window, navigate to the `functions` directory, and run the dedicated integration test command:
   ```bash
   npm run test:integration
   ```

3. **Run Unit Tests:**
   Unit tests do not require the emulator. You can run them anytime:
   ```bash
   npm run test:unit
   ```

## Troubleshooting

*   **Error: "Firestore emulator is not running!"**
    You forgot to start the emulator, or the emulator crashed. Ensure `npm run serve` is actively running in another terminal.
*   **Port Conflicts:**
    If ports 8080 or 9099 are in use, modify `firebase.json` to assign different ports and update the environment variables in `playback.integration.test.ts` accordingly.
