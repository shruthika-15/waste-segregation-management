import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class WasteSegregation {

    // Tune these keyword sets to your locale
    private static final Set<String> WET_KEYWORDS = new HashSet<>(Arrays.asList(
        "food", "vegetable", "fruit", "peel", "peels", "leftover", "tea", "coffee",
        "egg", "eggshell", "egg shell", "kitchen", "garden", "grass", "leaves",
        "meat", "fish", "bones", "flower", "rice", "pulp", "compost", "food waste"
    ));

    private static final Set<String> DRY_KEYWORDS = new HashSet<>(Arrays.asList(
        "paper", "cardboard", "plastic", "glass", "metal", "tin", "can", "cloth",
        "fabric", "rubber", "styrofoam", "packaging", "battery", "e-waste", "electronics",
        "bottle", "wrapper", "newspaper", "magazine"
    ));

    public static void main(String[] args) {
        if (args.length == 0) {
            interactiveMode();
        } else {
            String cmd = args[0].toLowerCase(Locale.ROOT);
            switch (cmd) {
                case "bulk":
                    if (args.length < 2) {
                        System.out.println("Usage: java WasteSegregation bulk <input.txt> [output.csv]");
                        return;
                    }
                    String input = args[1];
                    String out = args.length >= 3 ? args[2] : null;
                    bulkMode(input, out);
                    break;
                case "sample":
                    sampleRun();
                    break;
                default:
                    System.out.println("Unknown command. Use no arguments for interactive, 'bulk' or 'sample'.");
            }
        }
    }

    private static void interactiveMode() {
        System.out.println("Waste Segregation — Wet vs Dry (type 'exit' to stop, 'save <filename>' to save)");
        Scanner sc = new Scanner(System.in);
        List<Record> records = new ArrayList<>();

        while (true) {
            System.out.print("Enter waste item: ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.equals("exit")) break;
            if (lower.startsWith("save ")) {
                String[] parts = line.split("\\s+", 2);
                String filename = parts.length > 1 ? parts[1] : "waste_report.csv";
                saveCsv(records, filename);
                System.out.println("Saved " + records.size() + " records to " + filename);
                continue;
            }

            String cat = classifyItem(line);
            records.add(new Record(line, cat));
            System.out.println("-> '" + line + "' classified as: " + cat.toUpperCase());
        }

        if (!records.isEmpty()) {
            System.out.print("Save report? (y/n) ");
            String ans = new Scanner(System.in).nextLine().trim().toLowerCase(Locale.ROOT);
            if ("y".equals(ans)) {
                saveCsv(records, "waste_report.csv");
                System.out.println("Saved to waste_report.csv");
            }
        }
        System.out.println("Exiting interactive mode.");
    }

    private static void bulkMode(String inputPath, String outputCsv) {
        Path p = Paths.get(inputPath);
        if (!Files.exists(p)) {
            System.out.println("Input file not found: " + inputPath);
            return;
        }
        try {
            List<String> lines = Files.readAllLines(p).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            List<Record> records = new ArrayList<>();
            for (String line : lines) {
                records.add(new Record(line, classifyItem(line)));
            }

            String out = (outputCsv != null) ? outputCsv : p.getFileName().toString().replaceFirst("(\\.[^.]+)?$", "") + "_classified.csv";
            saveCsv(records, out);
            System.out.println("Classified " + records.size() + " items. Output -> " + out);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private static void sampleRun() {
        List<String> sampleItems = Arrays.asList(
            "Apple peel", "Plastic bottle", "Used tea bag", "Newspaper", "Eggshell",
            "Glass jar", "Vegetable leftover", "Styrofoam cup", "Old battery", "Grass clippings"
        );
        List<Record> recs = sampleItems.stream()
                .map(s -> new Record(s, classifyItem(s)))
                .collect(Collectors.toList());

        recs.forEach(r -> System.out.printf(" - %-20s -> %s%n", r.item, r.category));
        saveCsv(recs, "sample_waste_report.csv");
        System.out.println("Saved sample_waste_report.csv");
    }

    private static String classifyItem(String item) {
        String s = item.toLowerCase(Locale.ROOT);

        // direct keyword containment
        for (String kw : WET_KEYWORDS) {
            if (s.contains(kw)) return "wet";
        }
        for (String kw : DRY_KEYWORDS) {
            if (s.contains(kw)) return "dry";
        }

        // fallback heuristics
        String[] wetHints = {"leaf", "peel", "juice", "meat", "food", "cake", "rice", "pulp", "compost", "skin"};
        String[] dryHints = {"paper", "card", "plastic", "glass", "can", "bottle", "wrapper", "box"};

        for (String h : wetHints) if (s.contains(h)) return "wet";
        for (String h : dryHints) if (s.contains(h)) return "dry";

        return "unknown";
    }

    private static void saveCsv(List<Record> records, String filename) {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(filename))) {
            bw.write("item,category");
            bw.newLine();
            for (Record r : records) {
                // simple CSV escaping for commas and quotes
                String escapedItem = r.item.replace("\"", "\"\"");
                if (escapedItem.contains(",") || escapedItem.contains("\"")) {
                    escapedItem = "\"" + escapedItem + "\"";
                }
                bw.write(escapedItem + "," + r.category);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving CSV: " + e.getMessage());
        }
    }

    // simple record holder
    private static class Record {
        final String item;
        final String category;
        Record(String i, String c) { item = i; category = c; }
    }
}
