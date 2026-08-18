# Darth Invaders

A Java Swing arcade game with theme customisation, bosses, power-ups, sound, and a local leaderboard.

## Requirements

- JDK 21 or later
- Visual Studio Code with the Extension Pack for Java, or a JDK command line

## Run in VS Code

1. Open the repository folder in VS Code.
2. Open `src/spaceinvaders/Main.java`.
3. Choose **Run Java** above `main`, or use the Run and Debug view.

The game assets live under `src/resources`. Keep that folder available on the runtime classpath when using a custom launch configuration.

## Graphical Display

Darth Invaders is a Swing desktop application and needs an active graphical display. It cannot render in a headless environment such as a default GitHub Codespace, CI runner, or remote SSH shell without X11 forwarding. Run the game locally, or configure X11 forwarding and set `DISPLAY` before launching it remotely.

## Compile from PowerShell

```powershell
$sources = Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName }
javac -d build/classes $sources
java -cp "build/classes;src" spaceinvaders.Main
```

## Tests

The project includes JDK-only test harnesses (no external framework) for score persistence/leaderboard rules and for scoring/collision logic. Run them from PowerShell with:

```powershell
$sources = Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName }
$tests = Get-ChildItem -Path test -Filter *.java -Recurse | ForEach-Object { $_.FullName }
javac -d build/test-classes $sources $tests
java -cp build/test-classes spaceinvaders.scores.ScoreFileHandlerTest
java -cp build/test-classes spaceinvaders.GameLogicTest
```

## Controls

| Key | Action |
| --- | --- |
| Enter or Space | Start the game from the title screen |
| D | Cycle the title-screen difficulty preset |
| Left / Right | Move the shooter |
| Space | Fire |
| P | Pause or resume |
| R | Save the completed game score and restart after game over |

## Leaderboard Data

Scores are stored outside the repository so they work regardless of how the game is launched:

- Windows: `%APPDATA%\Darth-Invader\scores.txt`
- Other platforms: `~/.darth-invader/scores.txt`

On first save, any legacy `scores.txt` found in the launch directory is imported into the new location. Delete the new score file while the game is closed to reset the leaderboard.

## Player Preferences

Cosmetic choices (shooter/invader/bullet skins, background, explosion effects) persist across sessions in the same data directory:

- Windows: `%APPDATA%\Darth-Invader\preferences.properties`
- Other platforms: `~/.darth-invader/preferences.properties`

Theme and music selections are not persisted, since themes progress and unlock within a playthrough rather than being a fixed preference.

## Assets and Themes

Images, audio, backgrounds, bullets, and theme definitions are kept in `src/resources`. Theme definitions are JSON files in `src/resources/Themes`. Add a theme by placing its referenced assets in the matching resource folders and keeping all resource paths absolute from `/resources`.

## Architecture

- `spaceinvaders.Main`: creates the game window and application menus.
- `spaceinvaders.UI.SpaceInvadersUI`: owns game state, rendering, and restart lifecycle.
- `spaceinvaders.GameCalculator`: updates game state on a dedicated worker thread.
- `spaceinvaders.ListenerActions`: maps keyboard input to game actions.
- `spaceinvaders.DataHandlers`: loads and applies menu selections, music, and themes.
- `spaceinvaders.scores`: maintains the active score and persists the leaderboard.
