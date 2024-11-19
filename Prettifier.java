package itinerary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Prettifier {

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String ITALIC = "\u001B[3m";
    private static final String UNDERLINE = "\u001B[4m";

    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String PURPLE = "\u001B[35m";

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("-h")) {
            printUsage();
            return;
        }

        if (args.length != 3) {
            System.err.println(YELLOW + ITALIC + BOLD + "Invalid number of arguments" + RESET);
            printUsage();
            return;
        }

        String inputPath = args[0];
        String outputPath = args[1];
        String airportLookupPath = args[2];

        try (BufferedReader reader = new BufferedReader(new FileReader(inputPath))) {
            Map<String, String> airportLookup = loadAirportLookup(airportLookupPath);
            if (airportLookup == null) {
                System.err.println(UNDERLINE + BOLD + GREEN + "Airport lookup not found " + airportLookupPath + RESET);
                return;
            }
            String inputList = readInputFile(inputPath, airportLookup);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
                String[] lines = inputList.split("\n");
                List<String> nonEmptyLines = new ArrayList<>();
                int emptyLinesCount = 0;
                for (String line : lines) {
                    if (line.trim().isEmpty()) {
                        emptyLinesCount++;
                        if (emptyLinesCount <= 1) {
                            nonEmptyLines.add(line);
                        }
                    } else {
                        nonEmptyLines.add(line);
                        emptyLinesCount = 0;
                    }
                }
                writer.write(String.join("\n", nonEmptyLines));

            } catch (IOException e) {
                System.err.println("Error writing to output file");
            }
        } catch (FileNotFoundException e) {
            System.err.println(BOLD + RED + "Input not found: " + inputPath + RESET);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String readInputFile(String inputPath, Map<String, String> airportLookup) throws IOException {
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(inputPath))) {

            String line;
            while ((line = reader.readLine()) != null) {

                line = handleWhitespace(line);
                line = codeToNameOrMunicipality(airportLookup, line);
                line = formatDatesAndTimes(line);
                output.append(line).append("\n");
            }
        }

        return output.toString();
    }

    private static String handleWhitespace(String line) {
        line = line.replaceAll("[\r\\v\f]+", "\n");
        line = line.replaceAll("\\s+", " ").replaceAll("\\n{2,}", "\n\n");
        return line.trim();
    }

    private static String codeToNameOrMunicipality(Map<String, String> airportLookup, String line) {
        Pattern iata = Pattern.compile("(#[A-Z]{3}|\\*#[A-Z]{3})", Pattern.CASE_INSENSITIVE);
        Pattern icoa = Pattern.compile("(##[A-Z]{4}|\\*##[A-Z]{4})", Pattern.CASE_INSENSITIVE);
        Matcher iataMatcher = iata.matcher(line);
        Matcher icoaMatcher = icoa.matcher(line);
        while (iataMatcher.find()) {
            String iataCode = iataMatcher.group().replace("#", "").replace("*", "");
            boolean isCityLookup = iataMatcher.group().startsWith("*");

            if (airportLookup.containsKey(iataCode)) {
                String lookupValue = isCityLookup ? airportLookup.get("*" + iataCode)
                        : airportLookup.get(iataCode);
                line = line.replace(iataMatcher.group(), lookupValue);
            }
        }

        while (icoaMatcher.find()) {
            String icaoCode = icoaMatcher.group().replace("##", "").replace("*", "");
            boolean isCityLookup = icoaMatcher.group().startsWith("*");

            if (airportLookup.containsKey(icaoCode)) {
                String lookupValue = isCityLookup ? airportLookup.get("*" + icaoCode)
                        : airportLookup.get(icaoCode);
                line = line.replace(icoaMatcher.group(), lookupValue);
            }
        }
        return line;
    }

    private static void printUsage() {
        System.out.println(BOLD + PURPLE + UNDERLINE + "itinerary usage:\n" +
                "$ java Prettifier.java ./input.txt ./output.txt ./airport-lookup.csv" + RESET);
    }

    private static String formatDatesAndTimes(String line) {
        Pattern datePattern = Pattern.compile("(D|T12|T24)\\(([^)]+)\\)");
        Matcher matcher = datePattern.matcher(line);

        while (matcher.find()) {
            String dateTimeString = matcher.group(2);
            ZonedDateTime dateTime;

            if (!dateTimeString.matches(".*(Z|[+-]\\d{2}:\\d{2})?")) {
                System.out.println("Malformed datetime: " + dateTimeString);
                continue;
            }

            try {
                dateTime = ZonedDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME);

            } catch (DateTimeParseException e) {

                continue;
            }

            String formattedDateTime = "";
            switch (matcher.group(1)) {
                case "D":
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
                    formattedDateTime = dateTime.format(dateFormatter);
                    break;

                case "T12":
                    if (dateTimeString.contains("+") || dateTimeString.contains("-")) {
                        DateTimeFormatter time12Formatter = DateTimeFormatter.ofPattern("hh:mma (zzz)");
                        formattedDateTime = dateTime.format(time12Formatter);
                    } else {
                        DateTimeFormatter time12Formatter = DateTimeFormatter.ofPattern("hh:mma");
                        formattedDateTime = dateTime.format(time12Formatter);
                    }
                    break;

                case "T24":
                    if (dateTimeString.contains("+") || dateTimeString.contains("-")) {
                        DateTimeFormatter time24Formatter = DateTimeFormatter.ofPattern("HH:mm (zzz)");
                        formattedDateTime = dateTime.format(time24Formatter);
                    } else {
                        DateTimeFormatter time24Formatter = DateTimeFormatter.ofPattern("HH:mm");
                        formattedDateTime = dateTime.format(time24Formatter);
                    }
                    break;

                default:
                    continue;
            }
            line = line.replace(matcher.group(0), formattedDateTime);

        }
        return line.replace("Z", "+00:00");
    }

    private static Map<String, String> loadAirportLookup(String airportLookupPath) {
        Map<String, String> airportLookup = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(airportLookupPath))) {

            String header = reader.readLine();
            if (header == null) {
                System.err.println(BOLD + RED + "Airport lookup file is empty" + RESET);
                return null;
            }
            String[] headerColumns = header.split(",");
            if (headerColumns.length != 6) {
                System.err.println(BOLD + RED + "Airport lookup malformed" + RESET);
                System.exit(1);
                return null;
            }
            int nameIndex = Arrays.asList(headerColumns).indexOf("name");
            int municipalityIndex = Arrays.asList(headerColumns).indexOf("municipality");
            int icaoCodeIndex = Arrays.asList(headerColumns).indexOf("icao_code");
            int iataCodeIndex = Arrays.asList(headerColumns).indexOf("iata_code");
            int isoCountryIndex = Arrays.asList(headerColumns).indexOf("iso_country");
            int coordinatesIndex = Arrays.asList(headerColumns).indexOf("coordinates");

            if (nameIndex == -1) {
                System.err.println(BOLD + RED + "Airport lookup malformed: Missing 'name' column" + RESET);
            }
            if (municipalityIndex == -1) {
                System.err.println(BOLD + RED + "Airport lookup malformed: Missing 'municipality' column" + RESET);
            }
            if (icaoCodeIndex == -1) {
                System.err.println(BOLD + RED + "Airport lookup malformed: Missing 'icao_code' column" + RESET);
            }
            if (iataCodeIndex == -1) {
                System.err.println(BOLD + RED + "Airport lookup malformed: Missing 'iata_code' column" + RESET);
            }
            if (isoCountryIndex == -1) {
                System.err.println(BOLD + RED + "Airport lookup malformed: Missing 'iso_country' column" + RESET);
            }
            if (coordinatesIndex == -1) {
                System.err.println(BOLD + RED + "Airport lookup malformed: Missing 'coordinates' column" + RESET);
            }

            if (nameIndex == -1 || municipalityIndex == -1 || icaoCodeIndex == -1 || iataCodeIndex == -1
                    || isoCountryIndex == -1 || coordinatesIndex == -1) {
                System.exit(1);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (columns.length != 6) {
                    System.err.println(BOLD + RED + "Airport lookup malformed at line: " + line + RESET);
                    System.exit(1);
                    return null;
                }
                for (String column : columns) {
                    if (column.isEmpty()) {
                        System.err.println(BOLD + RED + "Airport lookup malformed at line: " + line + RESET);
                        System.exit(1);
                        return null;
                    }
                }

                String airportName = columns[nameIndex].trim();
                String municipality = columns[municipalityIndex].trim();
                String icaoCode = columns[icaoCodeIndex].trim();
                String iataCode = columns[iataCodeIndex].trim();
            

                airportLookup.put(iataCode, airportName);
                airportLookup.put(icaoCode, airportName);
                airportLookup.put("*" + iataCode, municipality);
                airportLookup.put("*" + icaoCode, municipality);
            }

        } catch (IOException e) {
            return null;
        }

        return airportLookup;
    }
}