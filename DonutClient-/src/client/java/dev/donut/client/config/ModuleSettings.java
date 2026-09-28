package dev.donut.client.config;

/** Shared on/off and favorite state for every toggleable module. */
public interface ModuleSettings {
    boolean isEnabled();

    void setEnabled(boolean value);

    boolean isFavorite();

    void setFavorite(boolean value);
}
