package dev.btstudio.herobrine.entity;

import java.util.Locale;

import dev.btstudio.herobrine.world.Feature;

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

    /**
     * The switch that turns this one sighting off. Every other part of the haunting keeps running;
     * only this way of showing himself goes quiet.
     */
    public Feature feature() {
        return switch (this) {
            case WATCH -> Feature.WATCH;
            case SCARE -> Feature.SCARE;
            case JUMPSCARE -> Feature.JUMPSCARE;
            case SPIKE -> Feature.SPIKE;
            case RUNNER -> Feature.RUNNER;
            case LURKING -> Feature.LURKING;
            case DWELLING -> Feature.DWELLING;
        };
    }

    /**
     * How often this sighting is rolled, out of 100, before stage 2 and from stage 2 onwards. The
     * early game is mostly him standing and watching; the longer a world has been running, the more
     * often he is right behind you or in your face.
     */
    public int weight(int stage) {
        boolean early = stage < 2;
        return switch (this) {
            case WATCH -> early ? 30 : 15;
            case RUNNER -> 15;
            case LURKING -> 20;
            case DWELLING -> early ? 15 : 20;
            case SCARE -> early ? 10 : 12;
            case JUMPSCARE -> early ? 7 : 10;
            case SPIKE -> early ? 3 : 8;
        };
    }
}
