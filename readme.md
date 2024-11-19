
# Itinerary Prettifier

## Overview
The **Itinerary Prettifier** is a Java-based utility that processes travel itinerary text files by performing the following tasks:
1. Replaces IATA and ICAO airport codes with the corresponding airport name or municipality from a lookup file.
2. Formats date and time entries into more human-readable forms.
3. Cleans up whitespace and reduces multiple empty lines in the input file.

## Features
- Converts airport codes (IATA/ICAO) to airport names or city municipalities.
- Formats dates in `dd MMM yyyy` format.
- Supports both 12-hour and 24-hour time formats.
- Removes redundant whitespace and empty lines from the input text.

## Usage
### Command Line
```bash
$ java Prettifier.java <inputFile> <outputFile> <airportLookupFile>
```
### Example
```bash
$ java Prettifier.java ./input.txt ./output.txt ./airport-lookup.csv
```

### Arguments
1. `<inputFile>`: The path to the file containing the raw itinerary data.
2. `<outputFile>`: The path to the file where the processed output will be saved.
3. `<airportLookupFile>`: A CSV file containing airport lookup information for code-to-name translation.

### Help
To view the usage guide:
```bash
$ java Prettifier -h
```

## Input Format
- The input file should contain itineraries with airport codes in the format:
  - IATA: `#ABC`
  - ICAO: `##ABCD`
  - City Lookup (municipality): `*#ABC` or `*##ABCD`
- Dates should follow ISO date-time formats like `2022-09-14T12:00:00Z` for easy recognition.

## Output
- The program will produce a cleaner and more readable version of the itinerary with formatted dates, times, and replaced airport codes, ensuring no redundant spaces or extra newlines.

## Date and Time Formats
- Dates are formatted as `dd MMM yyyy` (e.g., `14 Sep 2022`).
- Time can be formatted in:
  - 12-hour format: `hh:mma`
  - 24-hour format: `HH:mm`
  - Time zones are also handled if included in the input.

## Example Lookup File Format (CSV)
The `airport-lookup.csv` file should have the following structure:

| name            | municipality | icao_code | iata_code | ... |
|-----------------|--------------|-----------|-----------|-----|
| Los Angeles Intl| Los Angeles  | KLAX      | LAX       | ... |
| London Heathrow | London       | EGLL      | LHR       | ... |

## Dependencies
- Java 8 or higher.

## Error Handling
- If an incorrect number of arguments are provided, or if files are not found, the program will print an error message and usage guide.
- Errors in parsing or malformed CSV rows will also trigger error messages, helping to debug input file issues.

