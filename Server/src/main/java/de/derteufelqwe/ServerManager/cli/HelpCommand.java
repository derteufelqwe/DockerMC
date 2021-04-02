package de.derteufelqwe.ServerManager.cli;

import picocli.CommandLine;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A custom version of {@link picocli.CommandLine.HelpCommand}, which adds support for automatic terminal width detection.
 * This class is mostly copied and modifications are marked with "// DMC: "
 */
@CommandLine.Command(name = "help", header = "Displays help information about the specified command",
        synopsisHeading = "%nUsage: ", helpCommand = true,
        description = {"%nWhen no COMMAND is given, the usage help for the main command is displayed.",
                "If a COMMAND is specified, the help for that command is shown.%n"})
public final class HelpCommand implements CommandLine.IHelpCommandInitializable2, Runnable {

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, descriptionKey = "helpCommand.help",
            description = "Show usage help for the help command and exit.")
    private boolean helpRequested;

    @CommandLine.Parameters(paramLabel = "COMMAND", descriptionKey = "helpCommand.command",
            description = "The COMMAND to display the usage help message for.")
    private String[] commands = new String[0];

    private CommandLine self;
    private PrintStream out;
    private PrintStream err;
    private PrintWriter outWriter;
    private PrintWriter errWriter;
    private CommandLine.Help.Ansi ansi; // for backwards compatibility with pre-4.0
    private CommandLine.Help.ColorScheme colorScheme;


    public void run() {
        CommandLine parent = self == null ? null : self.getParent();
        if (parent == null) { return; }
        parent.setUsageHelpAutoWidth(true);     // DMC: Added
        CommandLine.Help.ColorScheme colors = colorScheme != null ? colorScheme : CommandLine.Help.defaultColorScheme(ansi);

        if (commands.length > 0) {
            Map<String, CommandLine> parentSubcommands = parent.getCommandSpec().subcommands();
            String fullName = commands[0];

            if (parent.isAbbreviatedSubcommandsAllowed()) {
                fullName = AbbreviationMatcher.match(parentSubcommands.keySet(), fullName,
                        parent.isSubcommandsCaseInsensitive(), self);
            }

            CommandLine subcommand = parentSubcommands.get(fullName);

            if (subcommand != null) {
                subcommand.setUsageHelpAutoWidth(true); // DMC: Added
                if (outWriter != null) {
                    subcommand.usage(outWriter, colors);
                } else {
                    subcommand.usage(out, colors); // for compatibility with pre-4.0 clients
                }

            } else {
                throw new CommandLine.ParameterException(parent, "Unknown subcommand '" + commands[0] + "'.", null, commands[0]);
            }

        } else {
            if (outWriter != null) {
                parent.usage(outWriter, colors);

            } else {
                parent.usage(out, colors); // for compatibility with pre-4.0 clients
            }
        }
    }

    /** {@inheritDoc} */
    public void init(CommandLine helpCommandLine, CommandLine.Help.ColorScheme colorScheme, PrintWriter out, PrintWriter err) {
        this.self        = Assert.notNull(helpCommandLine, "helpCommandLine");
        this.colorScheme = Assert.notNull(colorScheme, "colorScheme");
        this.outWriter   = Assert.notNull(out, "outWriter");
        this.errWriter   = Assert.notNull(err, "errWriter");
    }

    private static class AbbreviationMatcher {
        public static List<String> splitIntoChunks(String command, boolean caseInsensitive) {
            List<String> result = new ArrayList<String>();
            int start = 0, codepoint;
            StringBuilder nonAlphabeticPrefix = new StringBuilder();
            while (start < command.length()) {
                codepoint = command.codePointAt(start);
                if (Character.isLetterOrDigit(codepoint)) {
                    break;
                }
                nonAlphabeticPrefix.appendCodePoint(codepoint);
                start += Character.charCount(codepoint);
            }
            if (nonAlphabeticPrefix.length() > 0) {
                result.add(nonAlphabeticPrefix.toString());
//                if (command.codePointBefore(start) == '-') {
//                    start--; // hint makeCanonical() to canonicalize the first chunk
//                }
            }
            for (int i = start; i < command.length(); i += Character.charCount(codepoint)) {
                codepoint = command.codePointAt(i);
                if ((!caseInsensitive && Character.isUpperCase(codepoint)) || '-' == codepoint) {
                    String chunk = makeCanonical(command.substring(start, i));
                    if (chunk.length() > 0) {
                        result.add(chunk);
                    }
                    start = i;
                }
            }
            if (start < command.length()) {
                String chunk = makeCanonical(command.substring(start));
                if (chunk.length() > 0) {
                    result.add(chunk);
                }
            }
            return result;
        }

        private static String makeCanonical(String str) {
            if ("-".equals(str)) {
                return "";
            }
            if (str.startsWith("-") && str.length() > 1) {
                String uppercase = String.valueOf(Character.toChars(Character.toUpperCase(str.codePointAt(1))));
                return uppercase + str.substring(1 + uppercase.length());
            }
            return str;
        }

        /** Returns the non-abbreviated name if found, otherwise returns the specified original abbreviation value. */
        public static String match(Set<String> set, String abbreviation, boolean caseInsensitive, CommandLine source) {
            if (set.contains(abbreviation) || set.isEmpty()) { // return exact match
                return abbreviation;
            }
            List<String> abbreviatedKeyChunks = splitIntoChunks(abbreviation, caseInsensitive);
            List<String> candidates = new ArrayList<String>();
            for (String key : set) {
                List<String> keyChunks = splitIntoChunks(key, caseInsensitive);
                if (matchKeyChunks(abbreviatedKeyChunks, keyChunks, caseInsensitive)) {
                    candidates.add(key);
                }
            }
            if (candidates.size() > 1) {
                String str = candidates.toString();
                throw new CommandLine.ParameterException(source, "Error: '" + abbreviation + "' is not unique: it matches '" +
                        str.substring(1, str.length() - 1).replace(", ", "', '") + "'");
            }
            return candidates.isEmpty() ? abbreviation : candidates.get(0); // return the original if no match found
        }

        private static boolean matchKeyChunks(List<String> abbreviatedKeyChunks, List<String> keyChunks, boolean caseInsensitive) {
            if (abbreviatedKeyChunks.size() > keyChunks.size()) {
                return false;
            }
            int matchCount = 0;
            if (isNonAlphabetic(keyChunks.get(0))) { // non-alphabetic prefix must be exactly the same
                if (!keyChunks.get(0).equals(abbreviatedKeyChunks.get(0))) {
                    return false;
                }
                matchCount++;
            }
            if (!startsWith(keyChunks.get(matchCount), abbreviatedKeyChunks.get(matchCount), caseInsensitive)) { // first alphabetic chunk must match
                return false;
            }
            matchCount++;
            for (int i = matchCount, lastMatchChunk = matchCount; i < abbreviatedKeyChunks.size(); i++, matchCount++) {
                boolean found = false;
                for (int j = lastMatchChunk; j < keyChunks.size(); j++) {
                    if ((found = startsWith(keyChunks.get(j), abbreviatedKeyChunks.get(i), caseInsensitive))) {
                        lastMatchChunk = j + 1;
                        break;
                    }
                }
                if (!found) { // not a candidate
                    break;
                }
            }
            return matchCount == abbreviatedKeyChunks.size();
        }

        private static boolean startsWith(String str, String prefix, boolean caseInsensitive) {
            if (prefix.length() > str.length()) {
                return false;
            }
            String strPrefix = str.substring(0, prefix.length());
            return caseInsensitive ? strPrefix.equalsIgnoreCase(prefix) : strPrefix.equals(prefix);
        }

        private static boolean isNonAlphabetic(String str) {
            for (int i = 0, codepoint; i < str.length(); i += Character.charCount(codepoint)) {
                codepoint = str.codePointAt(i);
                if (Character.isLetterOrDigit(codepoint)) { return false; }
            }
            return true;
        }
    }

    private static class Assert {
        /**
         * Throws a NullPointerException if the specified object is null.
         * @param object the object to verify
         * @param description error message
         * @param <T> type of the object to check
         * @return the verified object
         */
        static <T> T notNull(T object, String description) {
            if (object == null) {
                throw new NullPointerException(description);
            }
            return object;
        }
        static boolean equals(Object obj1, Object obj2) { return obj1 == null ? obj2 == null : obj1.equals(obj2); }
        static int hashCode(Object obj) {return obj == null ? 0 : obj.hashCode(); }
        static int hashCode(boolean bool) {return bool ? 1 : 0; }
        static void assertTrue(boolean condition, String message) {
            if (!condition) throw new IllegalStateException(message);
        }
        static void assertTrue(boolean condition, CommandLine.IHelpSectionRenderer producer) {
            if (!condition) throw new IllegalStateException(producer.render(null));
        }
        private Assert() {} // private constructor: never instantiate
    }

}