# Changelog

All notable changes to Referee. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow [Semantic Versioning](https://semver.org/), so while Referee is 0.x a minor release can break things
(marked **Breaking** below).

## [0.3.2] - 2026-11-15

### Added
- `SpectatorHandler#setTransparent(true)`: spectators see each other as see-through ghosts. Off by default.
- Spectators show as `✖ | name` in gray in tab and above their head; change it with
  `MatchMessages#spectatorNamePrefix()` and `spectatorNameColor()`.
- Spectator chat: spectators only chat with each other, with their team in front if they had one
  (`MatchMessages#spectatorChat(...)` and `spectatorChatTeam(...)`).
- Spectators are listed below the players in tab.

### Fixed
- Spectators could move items in their inventory.

## [0.3.1] - 2026-11-14

### Fixed
- Spectators could pick up arrows and tridents from the ground.
- Projectiles stopped at hidden spectators instead of flying through them.

## [0.3.0] - 2026-11-14

### Added
- Opt-in spectators: return `new SpectatorHandler<>(this)` from `Match#createSpectatorHandler()`. They fly in
  adventure mode, hidden from players, and can't touch the game.
- Spectator compass with a teleport menu of players still in the match (pages past 45 players). Unlike vanilla
  spectator mode, it can't teleport to eliminated players.
- Customisable teleport menu entries: extend `SpectatorHandler<P>` and override `headName`, `headLore` or
  `createHead`. By default an entry shows the player's team.
- Staff with `referee.spectate` can join a running match to spectate; override `PlayingState#canSpectate(player)`
  to change who may.
- `MatchMessages#teleportCompassName()`, `teleportMenuTitle()`, `teleportHeadTeam(...)` and `teleportHeadClick()`.

### Changed
- **Breaking:** `PlayingState#allowSpectators()` is replaced by `canSpectate(Player)`. Outsiders are refused on
  join (when permissions are known) instead of at login.
- Games that use spectators need the [Satchel](https://github.com/Tqqn/Satchel) plugin (`depend: [Satchel]`);
  `SpectatorHandler` refuses to start without it. Games without spectators don't need it.

## [0.2.0] - 2026-11-11

### Added
- `match.eliminate(player)` / `eliminate(uuid)`: makes the player a spectator and fires `PlayerEliminatedEvent`.
  When one player or team is left, `onDecided()` runs (default: the next state, usually `EndState`).
- `match.spectate(player)`: sets up a spectator (vanilla spectator mode); override for your own.
- `PlayingState`: the state where the game is played. Players who quit are eliminated, or get `rejoinSeconds()` to
  reconnect. Players who log in during the game spectate unless `allowSpectators()` is false. Sets the status to
  `IN_GAME`.
- `MatchMessages#playerDisconnected(...)`, `playerRejoined(...)` and `playerLeftMatch(...)`.

### Fixed
- README: the install snippet used `v0.1.0` (tags have no `v`) and `main-SNAPSHOT` (the branch is `master`).
- README: examples used the removed 3-argument `MatchSettings` and `envInt`, and missed `getLogger()`.

## [0.1.2] - 2026-11-06

### Added
- Logs when a state is entered and exited.

### Changed
- **Breaking:** `Match` has an abstract `getLogger()` every match must implement (usually `getPlugin().getLogger()`).

## [0.1.1] - 2026-11-05

### Added
- `AbstractState#getMatch()`.
- `WaitingState#getLeft()`: seconds left on the lobby countdown, -1 while waiting for players.

## [0.1.0] - 2026-11-05

First release.

### Added
- States with an optional countdown; listeners, commands and tasks registered in a state are cleaned up when it ends.
- `WaitingState`: lobby with a minimum-player countdown, cancel on leave, force start, team assignment.
- `EndState`: countdown, sends players to the lobby server, shuts the server down.
- `SoloMatch` and `TeamMatch` (one-player teams too), with swappable team assignment and eight team colours.
- Arenas: `TemplateArena` copies a prepared map, `GeneratedArena` creates a fresh world; named spawn points from
  `arena.yml`.
- `MatchSettings.fromEnv(...)` for container-per-match setups.
- Hooks for messages, team assignment and status publishing.

[0.3.0]: https://github.com/Tqqn/Referee/compare/0.2.0...0.3.0
[0.2.0]: https://github.com/Tqqn/Referee/compare/0.1.2...0.2.0
[0.1.2]: https://github.com/Tqqn/Referee/compare/0.1.1...0.1.2
[0.1.1]: https://github.com/Tqqn/Referee/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/Tqqn/Referee/releases/tag/0.1.0
