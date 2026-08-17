package spaceinvaders.characters;

public class Invader {
    public enum InvaderType { NORMAL, FAST, TANK }

    private int x, y, size;
    private int speedPixelsPerUpdate;
    private final InvaderType type;
    private int health;

    public Invader(int x, int y, int size) {
        this(x, y, size, 2, InvaderType.NORMAL);
    }

    public Invader(int x, int y, int size, int speed) {
        this(x, y, size, speed, InvaderType.NORMAL);
    }

    public Invader(int x, int y, int size, int speed, InvaderType type) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.speedPixelsPerUpdate = speed;
        this.type = type;
        this.health = type == InvaderType.TANK ? 2 : 1;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getSize() {
        return size;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getSpeed() {
        return speedPixelsPerUpdate;
    }

    public void setSpeed(int speed) {
        this.speedPixelsPerUpdate = speed;
    }

    public InvaderType getType() {
        return type;
    }

    public int getHealth() {
        return health;
    }

    public void damage() {
        health = Math.max(0, health - 1);
    }
}
