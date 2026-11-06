package dev.btstudio.herobrine.entity;

import java.util.Locale;

/**
 * The four ways he shows himself, straight out of <i>Him - Herobrine</i>.
 *
 * <ul>
 *   <li>{@link #WATCH} - the classic sighting: he stands far away, stares, and vanishes.</li>
 *   <li>{@link #SCARE} - he is already standing right behind you when you turn around. Hit him
 *       and he stops pretending: he turns hostile and comes for you.</li>
 *   <li>{@link #JUMPSCARE} - he materialises in front of your face with a scream, and the darkness
 *       closes in around the edges of your screen.</li>
 *   <li>{@link #SPIKE} - a silent watcher a few blocks away that lunges into your face and leaves
 *       you with nausea.</li>
 *   <li>{@link #RUNNER} - he lets you see him, and the moment you look or move towards him he
 *       bolts, jumps away through the trees and is gone.</li>
 *   <li>{@link #LURKING} - <i>From The Fog</i>'s "lurking": two to four times as far away as a
 *       normal sighting (up to 128 blocks), and he only reacts when you get close.</li>
 *   <li>{@link #DWELLING} - "dwelling": he stands behind you and simply refuses to go while it is
 *       dark. Light the place up to make him leave.</li>
 * </ul>
 */
public enum Apparition {
    WATCH,
    SCARE,
    JUMPSCARE,
    SPIKE,
    RUNNER,
    LURKING,
    DWELLING;

    /** Case-insensitive lookup for commands and saved data; null when the name is unknown. */
    public static Apparition byName(String name) {
        if (name == null) {
            return null;
        }

        String lower = name.toLowerCase(Locale.ROOT);

        for (Apparition apparition : values()) {
            if (apparition.name().toLowerCase(Locale.ROOT).equals(lower)) {
                return apparition;
            }
        }

        return null;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
