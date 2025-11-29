_wireframes_

UX Flow Map:
[App Launch]
  -> [HomeScreen]
       - Button: Create Room -> [CreateRoomScreen]
       - Button: Join Room -> [JoinRoomScreen]
[CreateRoomScreen]
  - Generates room & code (status WAITING)
  - Shows code
  - "Enter Game" navigates to GameScreen
[JoinRoomScreen]
  - Enter 4-digit code
  - Success -> GameScreen
  - Failure -> Snackbar error
[GameScreen]
  - Room code + board + turn indicator
  - Moves alternate X/O
  - Win or Draw -> FINISHED state + Rematch button
  - Rematch resets board
  - Back returns to Home

State Transitions:
WAITING -> ACTIVE -> FINISHED -> (Rematch) -> ACTIVE
FINISHED conditions: winner found OR draw OR participant left (Sprint 2)

Screen Wireframes:

HomeScreen:
+------------------------------------------------+
|                 Tic Tac Toe                    |
|                                                |
|  Display Name: [ AndroidUser123____ ]          |
|                                                |
|        [ Create Room ]                         |
|        [ Join Room   ]                         |
|                                                |
|  Snackbar area (errors/info)                   |
+------------------------------------------------+

CreateRoomScreen (Waiting for second player):
+------------------------------------------------+
| < Back            Room Code: 0274              |
|------------------------------------------------|
| Host: You (X)                                  |
| Status: Waiting for player...                  |
|                                                |
| Share this code: 0 2 7 4                       |
|                                                |
| [ Enter Game ]                                 |
|                                                |
| Snackbar area                                  |
+------------------------------------------------+

JoinRoomScreen:
+------------------------------------------------+
| < Back                 Join a Room             |
|------------------------------------------------|
| Enter 4-digit room code:                       |
| [ 0 ][ 2 ][ 7 ][ 4 ]                           |
|                                                |
| [ Join ]                                       |
|                                                |
| Snackbar: errors (Room not found / full)       |
+------------------------------------------------+

GameScreen (Active):
+------------------------------------------------+
| < Back         Code: 0274        You: X        |
|------------------------------------------------|
| Turn: X (You)                                  |
|                                                |
|   +---+---+---+                                |
|   | 0 | 1 | 2 |                                |
|   +---+---+---+                                |
|   | 3 | 4 | 5 |                                |
|   +---+---+---+                                |
|   | 6 | 7 | 8 |                                |
|   +---+---+---+                                |
|                                                |
| [ Rematch ] (hidden until FINISHED)            |
|                                                |
| Snackbar: errors (Not your turn / Cell taken)  |
+------------------------------------------------+

GameScreen (Winner):
+------------------------------------------------+
| < Back         Code: 0274        You: X        |
|------------------------------------------------|
| Winner: X                                      |
|                                                |
|   +---+---+---+                                |
|   | X | O | X |                                |
|   +---+---+---+                                |
|   | O | X | O |                                |
|   +---+---+---+                                |
|   | X |   |   |                                |
|   +---+---+---+                                |
|                                                |
| [ Rematch ]                                    |
|                                                |
| Snackbar: "Rematch started"                    |
+------------------------------------------------+

GameScreen (Draw):
+------------------------------------------------+
| < Back         Code: 0274        You: O        |
|------------------------------------------------|
| Result: Draw                                   |
|                                                |
|   +---+---+---+                                |
|   | X | O | X |                                |
|   +---+---+---+                                |
|   | O | O | X |                                |
|   +---+---+---+                                |
|   | X | X | O |                                |
|   +---+---+---+                                |
|                                                |
| [ Rematch ]                                    |
|                                                |
| Snackbar (if any)                              |
+------------------------------------------------+

GameScreen (Offline Sprint 2):
+------------------------------------------------+
| < Back         Code: 0274        You: X        |
|------------------------------------------------|
| Turn: X (You)   Status: OFFLINE                |
|                                                |
|   +---+---+---+                                |
|   | X | O | X |                                |
|   +---+---+---+                                |
|   | O |   |   |                                |
|   +---+---+---+                                |
|   |   |   |   |                                |
|   +---+---+---+                                |
|                                                |
| (Overlay: "Offline – moves disabled")          |
|                                                |
| [ Rematch ] (if finished)                      |
|                                                |
| Snackbar: "Offline – moves disabled"           |
+------------------------------------------------+

Rematch Flow:
FINISHED --(tap Rematch)--> ACTIVE (movesString cleared, nextTurn=host)

Snackbars (Sample Messages):
"Room code invalid" | "Room is full" | "Not your turn" | "Cell taken" | "Offline – moves disabled" | "Rematch started"

Moves Encoding Example:
movesString: "H+0,J+4,H+1,J+8,H+2" -> Host wins (top row)

Board Derivation Example:
Index -> char: 0->X,4->O,1->X,8->O,2->X
Board:
0 1 2
3 4 5
6 7 8
Symbols:
[X][X][X]
[.][O][.]
[.][.][O]

Accessibility Notes:
- Cells: contentDescription "Cell 0 empty" / "Cell 0 X" / "Cell 0 O"
- Rematch button: "Start a new game with same players"
- Offline overlay: "Offline. Moves disabled."

End of Wireframes.

