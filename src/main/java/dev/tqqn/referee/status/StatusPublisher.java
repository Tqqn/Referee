package dev.tqqn.referee.status;

import dev.tqqn.referee.match.Match;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
@FunctionalInterface
public interface StatusPublisher {

    StatusPublisher NONE = (match, status) -> {};

    void publish(Match<?> match, MatchStatus status);
}
