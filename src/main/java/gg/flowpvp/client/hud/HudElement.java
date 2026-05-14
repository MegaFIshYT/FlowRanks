package gg.flowpvp.client.hud;

public enum HudElement {
    SELECTED_GAMEMODE("Selected Gamemode", false),
    YOUR_ELO("Your ELO", true),
    OPPONENT_ELO("Opponent ELO", true);

    public final String label;
    /** If true, only shown when a match is detected. */
    public final boolean matchOnly;

    HudElement(String label, boolean matchOnly) {
        this.label = label;
        this.matchOnly = matchOnly;
    }
}
