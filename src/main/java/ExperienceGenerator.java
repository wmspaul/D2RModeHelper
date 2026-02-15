import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ExperienceGenerator {

    // ==========================
    // SETTINGS
    // ==========================
    static final int MAX_LEVEL = 250;          // Desired max level
    static final double XP_GAIN_PERCENT = 10000; // 10000 = 10000% XP
    static final String OUTPUT_FILE = "experience.txt";

    static final long MAX_SAFE_XP = 2_000_000_000L; // Prevent overflow

    // Base XP curve constants
    static final double BASE_XP = 500.0;
    static final double GROWTH_RATE = 1.15;  // Controls XP curve steepness

    public static void main(String[] args) {

        double xpMultiplier = XP_GAIN_PERCENT / 100.0;
        double divisor = xpMultiplier;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE))) {

            // Write header
            writer.write("Level\tAmazon\tSorceress\tNecromancer\tPaladin\tBarbarian\tDruid\tAssassin\tWarlock\tExpRatio\n");

            // Write MaxLvl row
            writer.write("MaxLvl");
            for (int i = 0; i < 8; i++) {
                writer.write("\t" + MAX_LEVEL);
            }
            writer.write("\t10\n");

            // Generate XP table
            for (int level = 0; level <= MAX_LEVEL; level++) {

                writer.write(String.valueOf(level));

                long xpValue;

                if (level == 0) {
                    xpValue = 0;
                } else {
                    double rawXp = BASE_XP * Math.pow(GROWTH_RATE, level - 1);
                    rawXp = rawXp / divisor;

                    if (rawXp < 1)
                        rawXp = 1;

                    if (rawXp > MAX_SAFE_XP)
                        rawXp = MAX_SAFE_XP;

                    xpValue = (long) rawXp;
                }

                // Write XP for all classes
                for (int i = 0; i < 8; i++) {
                    writer.write("\t" + xpValue);
                }

                writer.write("\t1024\n"); // ExpRatio
            }

            System.out.println("experience.txt generated successfully!");
            System.out.println("Max Level: " + MAX_LEVEL);
            System.out.println("XP Gain: " + XP_GAIN_PERCENT + "%");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
