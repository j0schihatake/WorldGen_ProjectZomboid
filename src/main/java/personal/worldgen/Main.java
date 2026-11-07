package personal.worldgen;

public final class Main {
    public static void main(String[] args) {
        PersonalWorldGenAPI api = PersonalWorldGenAPI.getInstance();
        System.out.println("[PersonalWorldGen] Java core " + PersonalWorldGenAPI.VERSION
                + " loaded; " + api.status());
        System.out.println("[PersonalWorldGen] demo " + api.planDemo(42L));
    }
}
