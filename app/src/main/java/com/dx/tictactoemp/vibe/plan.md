_plan_

High-Level Summary:
Two-sprint Jetpack Compose MVP. Sprint 1: single-device local play with in-memory repositories, create/join room UX, host = X, joiner = O, moves encoded "userId+cellIndex" (0–8). Sprint 2: add mandatory Google Sign-In, Firestore (Auth + DB), transactional move validation, offline detection that disables moves, rematch button after finish. Architecture is minimal MVVM + Repository, role derivation on the fly, board derived from movesString, snackbars for all errors.

Architecture Core:
- Activity + NavHost routes: home, createRoom, joinRoom, game/{roomId}
- ViewModels: AuthViewModel (guest or Firebase user), RoomsViewModel (create/join), GameViewModel (state, moves, win/draw/rematch)
- Repository interfaces: GameRepository, AuthRepository
- Implementations: LocalGameRepository (Sprint 1), FirebaseGameRepository (Sprint 2); LocalAuthService, FirebaseAuthService
- Models: User(id, displayName?, photoUrl?, createdAt), Room(id, code, hostUserId, participantUserIds, status, createdAt, finishedAt?), GameState(roomId, movesString, nextTurnUserId, winnerUserId?, updatedAt)
- Utilities: WinChecker, MoveCodec, RoomCodeGenerator (4-digit numeric), NetworkStatusMonitor (Sprint 2)
- Roles: host = X, joiner = O (derived)
- MovesString: "hostUid+0,joinUid+4,..."
- Board: derived each recomposition (List<Char?> size 9)
- Offline: disable move input, show banner/snackbar

Sprint 1 Scope:
- Local only, in-memory
- Screens: Home, CreateRoom, JoinRoom, Game
- Features: Create/join room, alternate turns, win/draw detection, rematch (reset), errors via snackbars
- Rematch = resetGame after FINISHED
- Draw: board full & no winner => status FINISHED (banner "Draw")
- Room persistence ends when app killed (in-memory only)
- Manual QA (no automated tests per your choice)

Sprint 2 Scope:
- Add Firebase (Auth + Firestore)
- Mandatory Google Sign-In
- Room/game persistence in Firestore
- Transactions for moves (validate turn & cell)
- Mark room FINISHED on win/draw or participant leave
- Offline detection: network callback disables moves
- Rematch uses resetGame transaction
- Code collision mitigation: retry up to 5 times
- Security rules restrict writes to participants

Key Repository Methods:
GameRepository:
- createRoom(hostUserId)
- joinRoom(code, userId)
- observeGame(roomId): Flow<GameState>
- submitMove(roomId, userId, cellIndex)
- resetGame(roomId)
- getRoomByCode(code)
- markPlayerLeft(roomId, userId) (Sprint 2)

AuthRepository:
- currentUserFlow()
- signInWithGoogle(...)
- signOut()
- upsertUser(user)

Error Surface (Snackbars):
- Invalid/join code
- Room full
- Not your turn
- Cell taken
- Offline moves disabled
- Sign-in canceled
- Room creation collision (after retries)

Rematch Flow:
- Visible only when FINISHED (win or draw)
- Pressing rematch resets movesString, winnerUserId, sets nextTurn to host
- Room remains ACTIVE

Data Integrity:
- Firestore transactions ensure atomic move updates
- Local repository sequential operations (single thread model for simplicity)

Scalability Hooks (Optional later):
- Cleanup stale finished rooms
- Stats (wins/draws)
- Spectators
- Larger boards (change WinChecker + MoveCodec)

Models (Detailed):
User:
- id: String
- displayName: String?
- photoUrl: String?
- createdAt: Long

Room:
- id: String
- code: String (4-digit numeric "0123")
- hostUserId: String
- participantUserIds: List<String> (size 1 or 2)
- status: WAITING | ACTIVE | FINISHED
- createdAt: Long
- finishedAt: Long? (nullable; set when winner or mid-game exit)

GameState:
- roomId: String
- movesString: String (e.g. "hostUid+0,joinUid+4,hostUid+1")
- nextTurnUserId: String
- winnerUserId: String? (null until win)
- updatedAt: Long
(Derived in ViewModel: board: List<Char?> size 9; turnSymbol: 'X' or 'O')

Utilities:
WinChecker: Predefined winning line indices -> winner or draw.
MoveCodec: parse, append, deriveBoard.
RoomCodeGenerator: random 0000–9999 (pad zeros), Firestore uniqueness with retries.
NetworkStatusMonitor: ConnectivityManager callback -> StateFlow<Boolean> online.

Sprint 1 Detailed Flow:
1. NavHost skeleton
2. Implement models & utilities
3. LocalGameRepository (maps + StateFlow)
4. AuthViewModel (guest user)
5. RoomsViewModel (create/join)
6. GameViewModel (observe + submit + reset + rematch)
7. Screens UI
8. Manual QA scenarios

Sprint 2 Detailed Flow:
1. Firebase project + google-services.json
2. Add deps (auth + firestore) + plugin
3. Google Sign-In enable & integrate
4. FirebaseAuthService & AuthRepository implementation
5. FirebaseGameRepository (create/join/observe/transactional moves)
6. Security rules tighten
7. Offline monitor integrate
8. Rematch transaction
9. Manual multi-device QA
10. Polish & logging hygiene

Security Rules Draft (Firestore):
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
      allow update, delete: if isSignedIn() && (request.auth.uid in resource.data.participantUserIds);
    }
    match /games/{roomId} {
      allow read: if isSignedIn();
      allow write: if isSignedIn() && (request.auth.uid in get(/databases/$(database)/documents/rooms/$(roomId)).data.participantUserIds);
    }
  }
}

Manual QA Checklist (Sprint 1):
- Create room, note code
- Join room
- Alternate moves; win detection
- Illegal move blocked
- Draw scenario
- Rematch after win/draw

Manual QA Checklist (Sprint 2):
- Google sign-in success/cancel
- Room creation uniqueness
- Join from second device
- Turn sync & winner
- Draw
- Rematch
- Offline disable moves

Risks & Mitigations:
- 4-digit collisions: retry up to 5
- Skipped tests: thorough manual checklist
- Offline confusion: clear banner & disabled grid
- Concurrency (moves): Firestore transaction

Future Enhancements (Optional): Stats, spectators, cleanup, larger board size.

End of Plan.

