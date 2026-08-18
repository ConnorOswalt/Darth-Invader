package spaceinvaders;

import spaceinvaders.characters.Invader;

/**
 * Pure scoring calculations shared by GameCalculator and SpaceInvadersUI.
 * Kept free of Swing/thread dependencies so it can be unit tested directly.
 */
public final class ScoringRules {

    private ScoringRules() {
    }

    public static int comboMultiplier(int comboCount) {
        if (comboCount >= 5) return 3;
        if (comboCount >= 2) return 2;
        return 1;
    }

    public static int basePointsForInvaderKill(Invader.InvaderType type, boolean tinyPanicActive) {
        if (type == Invader.InvaderType.TANK) {
            return tinyPanicActive ? 50 : 25;
        }
        return tinyPanicActive ? 20 : 10;
    }

    public static int pointsForInvaderKill(Invader.InvaderType type, boolean tinyPanicActive, int comboCount) {
        return basePointsForInvaderKill(type, tinyPanicActive) * comboMultiplier(comboCount);
    }
}
