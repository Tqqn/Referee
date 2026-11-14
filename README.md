# Referee

A small framework for one-match-per-server Paper minigames. You describe your match as a list of states; Referee runs the clock, routes joins and quits, splits teams, and handles the lobby countdown and end-of-match flow.

- **States** with an optional countdown. Listeners, commands and tasks registered in a state are cleaned up when it ends.
- **Lobby** (`WaitingState`): min-player countdown, cancels when players leave, force start, assigns teams.
- **Playing** (`PlayingState`): eliminations, quitting with an optional reconnect window, late joiners spectate.
- **End** (`EndState`): countdown, sends players to your lobby server via BungeeCord/Velocity, shuts down.
- **Solo or teams**: `SoloMatch` for free-for-all, `TeamMatch` for teams (one-player teams too), with swappable team assignment.
- **Arenas**: copy a prepared map or generate a fresh world every match, with named spawn points from `arena.yml`.
- **Hooks** for messages, team assignment and status publishing (lobby signs, server lists).

Requires Paper 26.3+, Java 25 and [ACF](https://github.com/aikar/commands) (shade and relocate it in your plugin).

## Install (JitPack)

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.Tqqn</groupId>
    <artifactId>Referee</artifactId>
    <version>0.2.0</version>
</dependency>

<!-- Referee uses ACF but doesn't bundle it -->
<dependency>
    <groupId>co.aikar</groupId>
    <artifactId>acf-paper</artifactId>
    <version>0.5.1-SNAPSHOT</version>
</dependency>
```

ACF lives in `https://repo.aikar.co/content/groups/aikar/`. Shade both into your plugin with the `maven-shade-plugin` and relocate
`dev.tqqn.referee`, `co.aikar.commands` and `co.aikar.locales` into your own package. Want the latest commit instead of a release?
Use version `master-SNAPSHOT`.

## Example

```java
public final class SpleefMatch extends SoloMatch<MatchPlayer> {

    public SpleefMatch(JavaPlugin plugin, PaperCommandManager commands) {
        super(plugin, commands, MatchSettings.fromEnv(new MatchSettings(2, 1, 8, "CLASSIC")));   // min 2, up to 8 players
    }

    @Override 
    protected List<MatchState> createStates() {
        return List.of(new WaitingState<>(this), new SpleefActiveState(this), new EndState<>(this, "lobby"));
    }

    @Override 
    protected MatchPlayer createPlayer(UUID uuid) {
        return new MatchPlayer(uuid);
    }

    @Override 
    public Logger getLogger() {
        return getPlugin().getLogger();
    }
}

public final class SpleefActiveState extends PlayingState<SpleefMatch> {

    public SpleefActiveState(SpleefMatch match) { super(match, "Active", 600); }

    @Override 
    protected void onEnter() {
        super.onEnter();                        // IN_GAME status
        register(new SpleefListener(match));   // unregistered automatically when the state ends
    }
}
```

Call `match.start()` once your arena is ready. Until then, players are refused.

## Settings from environment

Handy for container-per-match setups: `MatchSettings.fromEnv(defaults)` reads `MIN_GAME_PLAYERS_TO_START`,
`PLAYERS_PER_TEAM`, `TEAM_COUNT` and `GAME_TYPE`, falling back to your defaults. Max players is team size × team count.

## Solo or teams

Pick the base class that fits the game:

| Game | Extend | Players |
|---|---|---|
| Free-for-all (UHC, Survival Games, Spleef) | `SoloMatch<P extends MatchPlayer>` | no teams at all |
| Team game (4 teams of 4) | `TeamMatch<P extends TeamPlayer>`, teamSize 4 | every player has a team + colour |
| Solo with colours (Bingo) | `TeamMatch<P extends TeamPlayer>`, teamSize 1 | one-player teams: own colour and slot |

```java
public final class BingoMatch extends TeamMatch<BingoPlayer> {           // BingoPlayer extends TeamPlayer
    public BingoMatch(JavaPlugin plugin, PaperCommandManager commands, MatchSettings settings) {
        super(plugin, commands, settings, settings.teamSize());          // e.g. teams of 1, 4 teams
    }
}

// active state: spawn and end the match
arena.teleportTeamsToSpawns(match.getTeams().getTeams());   // TeamMatch
arena.teleportToSpawns(match.getAlive());                   // SoloMatch: one spawn each
if (match.isDecided()) match.getWinner().ifPresent(this::announce);
```

Teams are assigned when the lobby countdown ends. At most 8 teams (one per colour); above that, use `SoloMatch`.

## Eliminations and quitting

Extend `PlayingState` for the part where the game is played. Your game decides when someone is out; Referee does the rest:

```java
// in your listener: fell into the void, died, ...
match.eliminate(player.getUniqueId());
```

`eliminate` makes them a spectator, fires `PlayerEliminatedEvent`, and when one player or team is left calls `onDecided()`
(default: go to the next state, usually your `EndState`). Players who quit are eliminated too, or get time to reconnect:

```java
@Override 
protected int rejoinSeconds() { return 30; }
```

Players who log in during the game spectate. Turn that off with `allowSpectators()`.

## Arenas

Two built-in arenas, both throwaway: the world is deleted on `unload()` and before every load.

```java
// A prepared map, copied fresh each match (plugins/YourGame/maps/<map>/ with region/, level.dat, arena.yml)
Arena arena = TemplateArena.random(plugin, plugin.getDataPath().resolve("maps"));

// Or a brand-new generated world
Arena arena = new GeneratedArena(plugin, new WorldCreator("arena").environment(World.Environment.NETHER));

arena.load()
        .thenRun(match::start)          // completes on the main thread
        .exceptionally(e -> {
        plugin.getLogger().log(Level.SEVERE, "Arena failed to load", e);
            Bukkit.shutdown();
            return null;
                    });

                    arena.teleportToSpawns(match.getAlive());            // SoloMatch
        arena.teleportTeamsToSpawns(match.getTeams().getTeams()); // TeamMatch
        player.teleport(arena.point("spectator"));
```

`arena.yml` inside a map folder:

```yaml
name: Sky Islands
points:
  spectator: [0.5, 80, 0.5]          # x, y, z
  spawn:                             # team N uses entry N; solo: one per player
    - [20.5, 64, 0.5, 90, 0]         # x, y, z, yaw, pitch
    - [-20.5, 64, 0.5, -90, 0]
```

Need borders, game rules or a pasted spawn? Extend an arena and override `setUp(World)`; return a future if it's async, and call `setPoint(...)` for spawns you find at runtime.

## License

MIT, see [LICENSE](LICENSE).