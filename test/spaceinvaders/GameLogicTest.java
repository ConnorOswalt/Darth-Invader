package spaceinvaders;

import spaceinvaders.characters.Boss;
import spaceinvaders.characters.Invader;

/**
 * JDK-only test harness (no external test framework) covering pure scoring
 * and collision-adjacent logic that does not require a graphical display.
 */
public final class GameLogicTest {
    private GameLogicTest() {
    }

    public static void main(String[] args) {
        testComboMultiplierThresholds();
        testBasePointsForInvaderKill();
        testPointsForInvaderKillCombinesMultiplier();
        testTankInvaderTakesTwoHitsToDie();
        testNormalInvaderDiesInOneHit();
        testBossTakesConfiguredHitsToDie();
        testFinalBossHasMoreHealthThanRegularBoss();
        System.out.println("GameLogicTest passed");
    }

    private static void testComboMultiplierThresholds() {
        require(ScoringRules.comboMultiplier(0) == 1, "No combo should have a 1x multiplier");
        require(ScoringRules.comboMultiplier(1) == 1, "A single kill should not yet grant a combo bonus");
        require(ScoringRules.comboMultiplier(2) == 2, "Two kills in the combo window should grant a 2x multiplier");
        require(ScoringRules.comboMultiplier(4) == 2, "Four kills should still be within the 2x tier");
        require(ScoringRules.comboMultiplier(5) == 3, "Five kills should reach the top 3x tier");
        require(ScoringRules.comboMultiplier(20) == 3, "The multiplier should cap at 3x");
    }

    private static void testBasePointsForInvaderKill() {
        require(ScoringRules.basePointsForInvaderKill(Invader.InvaderType.NORMAL, false) == 10,
                "Normal invaders should award 10 base points");
        require(ScoringRules.basePointsForInvaderKill(Invader.InvaderType.NORMAL, true) == 20,
                "Tiny Panic should double normal invader points");
        require(ScoringRules.basePointsForInvaderKill(Invader.InvaderType.TANK, false) == 25,
                "Tank invaders should award 25 base points");
        require(ScoringRules.basePointsForInvaderKill(Invader.InvaderType.TANK, true) == 50,
                "Tiny Panic should double tank invader points");
    }

    private static void testPointsForInvaderKillCombinesMultiplier() {
        int points = ScoringRules.pointsForInvaderKill(Invader.InvaderType.NORMAL, false, 5);
        require(points == 30, "10 base points at a 3x combo multiplier should award 30 points, was " + points);
    }

    private static void testTankInvaderTakesTwoHitsToDie() {
        Invader tank = new Invader(0, 0, 40, 2, Invader.InvaderType.TANK);
        require(tank.getHealth() == 2, "Tank invaders should start with 2 health");
        tank.damage();
        require(tank.getHealth() == 1, "Tank invaders should survive the first hit");
        tank.damage();
        require(tank.getHealth() == 0, "Tank invaders should die after the second hit");
    }

    private static void testNormalInvaderDiesInOneHit() {
        Invader normal = new Invader(0, 0, 40, 2, Invader.InvaderType.NORMAL);
        require(normal.getHealth() == 1, "Normal invaders should start with 1 health");
        normal.damage();
        require(normal.getHealth() == 0, "Normal invaders should die after a single hit");
    }

    private static void testBossTakesConfiguredHitsToDie() {
        Boss boss = new Boss(0, 0);
        require(boss.getMaxHealth() == 5, "Regular bosses should have 5 max health");
        for (int i = 0; i < 4; i++) {
            boss.takeDamage(1);
        }
        require(!boss.isDead(), "Boss should survive 4 hits out of 5 health");
        boss.takeDamage(1);
        require(boss.isDead(), "Boss should die after taking damage equal to its max health");
    }

    private static void testFinalBossHasMoreHealthThanRegularBoss() {
        Boss finalBoss = new Boss(0, 0, null, null, null, true);
        Boss regularBoss = new Boss(0, 0);
        require(finalBoss.getMaxHealth() > regularBoss.getMaxHealth(),
                "The final boss should have more health than a regular boss");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
