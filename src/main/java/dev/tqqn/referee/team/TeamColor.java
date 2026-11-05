package dev.tqqn.referee.team;

import lombok.Getter;
import org.bukkit.Color;

/**
 * @author Tqqn (tqqn.dev)
 * Created on 05/11/2026
 */
@Getter
public enum TeamColor {
    RED("Red", "<red>", Color.RED),
    BLUE("Blue", "<blue>", Color.BLUE),
    GREEN("Green", "<green>", Color.GREEN),
    YELLOW("Yellow", "<yellow>", Color.YELLOW),
    PURPLE("Purple", "<dark_purple>", Color.PURPLE),
    AQUA("Aqua", "<aqua>", Color.AQUA),
    ORANGE("Orange", "<gold>", Color.ORANGE),
    WHITE("White", "<white>", Color.WHITE);

    private final String displayName;
    private final String miniColor;
    private final Color color;

    TeamColor(String displayName, String miniColor, Color color) {
        this.displayName = displayName;
        this.miniColor = miniColor;
        this.color = color;
    }
}