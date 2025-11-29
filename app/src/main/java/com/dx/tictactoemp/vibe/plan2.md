Plan: Sprint 2 Online Play (Firebase)

TL;DR: Sprint 1 is ~95% complete (local mode playable; docs done; DI toggle to Firebase not yet used; tests intentionally deferred). Sprint 2 prework is ~25% (Firebase project/app/SHA1/google-services.json/Auth provider/Firestore ready; no collections yet). We’ll add FirebaseAuthService and FirebaseGameRepository, define Firestore schema and rules, wire repositories to switch to Firebase, and implement transactional move submission for safe, real-time online play.

Percent completion snapshot
- Sprint 1: 95%
  - Done: MVVM + Repo, screens, single-device play, room codes, snackbars, rematch, docs
  - Deferred: Unit tests; runtime repo toggle not used yet
- Sprint 2: ~25% prepared
  - Done: Firebase project, app, SHA-1, google-services.json, Google Auth enabled, Firestore created
  - Pending: Firestore collections/schema, rules, Firebase auth/repo code, ViewModel wiring, offline gating, multi-device tests
- Total project completion: ~65–70%

Firestore data schema (KISS)
- users/{uid}
  - displayName: string
  - photoUrl: string?
  - createdAt: serverTimestamp
- rooms/{roomId}
  - code: string (4 digits)
  - hostUserId: string (uid)
  - participantUserIds: array<string> (max 2)
  - status: "waiting" | "active" | "finished"
  - createdAt: serverTimestamp
  - finishedAt: serverTimestamp?
- games/{roomId}
  - movesString: string (e.g., "X+0,O+4,X+1")
  - nextTurnSymbol: "X" | "O"
  - winnerSymbol: "X" | "O" | null
  - updatedAt: serverTimestamp

Security rules (ready-to-paste)
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isSignedIn() { return request.auth != null; }
    match /users/{userId} {
      allow read: if isSignedIn();
      allow write: if isSignedIn() && request.auth.uid == userId;
    }
    match /rooms/{roomId} {
      allow read: if isSignedIn();
      allow create: if isSignedIn();
      allow update: if isSignedIn() &&
        (request.auth.uid == resource.data.hostUserId ||
         request.auth.uid in resource.data.participantUserIds);
    }
    match /games/{roomId} {
      allow read, write: if isSignedIn() &&
        (request.auth.uid in get(/databases/$(database)/documents/rooms/$(roomId)).data.participantUserIds);
    }
  }
}

Sprint 2 steps

1. Verify configuration
- Ensure Gradle has firebase-bom, firebase-auth, firebase-firestore, and google-services plugin.
- Confirm google-services.json under app/, and package name matches manifest.

2. Auth (Google Sign-In; mandatory)
- Implement FirebaseAuthService:
  - currentUserFlow(): Flow<User?>
  - signInWithGoogle(): start intent, exchange ID token → FirebaseAuth
  - signOut()
  - upsertUser(): write users/{uid} with displayName/photoUrl
- Replace LocalAuth with Firebase AuthRepository for online mode.
- Gate Create/Join behind sign-in; show sign-in prompt when not authenticated.

3. FirebaseGameRepository (Firestore)
- createRoom(hostUid):
  - Generate 4-digit code; check uniqueness via query (retry ≤5).
  - rooms/{roomId}: status=WAITING, hostUserId=uid, participantUserIds=[uid], createdAt.
  - games/{roomId}: movesString="", nextTurnSymbol="X", updatedAt.
- joinRoom(code, userUid):
  - Query room by code; validate WAITING and participants<2.
  - Update participantUserIds += userUid; set status=ACTIVE.
- observeGame(roomId):
  - Listen to games/{roomId} snapshots; map to state; derive board in ViewModel.
- submitMove(roomId, userUid, cell):
  - Transaction:
    - Read rooms/{roomId} and games/{roomId}.
    - Validate userUid ∈ participants.
    - Role = host→X or joiner→O; ensure role == nextTurnSymbol.
    - Ensure cell unused via parse(movesString).
    - Append "symbol+cell"; recompute winner/draw.
    - Update nextTurnSymbol or winnerSymbol; set rooms.status FINISHED + finishedAt if needed.
- resetGame(roomId):
  - Transaction: movesString="", nextTurnSymbol="X", winnerSymbol=null; rooms.status=ACTIVE, finishedAt=null.

4. Repository wiring (local vs online)
- RepositoryProvider:
  - Online mode (signed-in): use Firebase repos.
  - Keep Local repos for demo/testing; default app flow should require sign-in for online play.

5. ViewModel wiring
- AuthViewModel: switch to FirebaseAuthService; expose current user; block room actions until signed-in.
- RoomsViewModel: use FirebaseGameRepository; map errors to snackbars (room not found/full, permission denied).
- GameViewModel: use snapshot state; derive board; disable moves when offline; map transaction failures to snackbars.

6. Offline behavior
- Disable moves when offline; show “Offline – moves disabled” snackbar/banner.
- Detect via Firestore snapshot metadata or ConnectivityManager.

7. Security rules deployment and testing
- Apply rules (test mode off).
- Test read/write permissions via Emulator Suite and two devices.

8. Manual multi-device tests (MVP)
- Device A: sign-in, create room; code shown.
- Device B: sign-in, join room by code.
- Alternate moves; verify real-time sync; detect winner/draw; host rematch resets board.

Risks & mitigations
- 4-digit code collisions: retry ≤5; if exhausted, “Couldn’t create room—try again.”
- Move race conditions: transaction ensures atomicity and validation.
- Offline confusion: disable actions and show banner.
- Auth cancellations: show snackbar and allow retry.
- Rules edge cases: validate with emulator and live devices.

Deliverables
- FirebaseAuthService.kt
- FirebaseGameRepository.kt
- RepositoryProvider toggle update
- Firestore rules file (ready-to-paste)
- README updates (Sprint 2 setup + testing steps)

