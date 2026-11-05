package dev.tqqn.referee.message;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
public interface MatchMessages {

    MatchMessages DEFAULT = new MatchMessages() {};

    default String countdownStarted(int seconds) {
        return "<green>Enough players! Starting in <red><bold>" + seconds + "s<reset><green>.";
    }

    default String countdownTick(int seconds) {
        return "<red>Match is starting in <white><bold>" + seconds + "<reset><red>s.";
    }

    default String countdownCancelled() {
        return "<red>Not enough players, countdown cancelled.";
    }

    default String playerJoined(String name, int count, int max) {
        return "<yellow>[<aqua>" + count + "<yellow>/<aqua>" + max + "<yellow>] <green>+ " + name;
    }

    default String notJoinable() {
        return "<red>The match is not ready to join yet. Please try again later.";
    }

    default String full() {
        return "<red>This match is full.";
    }

    default String ended() {
        return "<red>This match has ended.";
    }

    default String restarting(int seconds) {
        return "<red>Restarting in <white>" + seconds + " <red>seconds.";
    }

    default String sendingToLobby() {
        return "<red>Server restarting... Sending to lobby...";
    }
}
