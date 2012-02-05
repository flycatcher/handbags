package org.nevralia;

import org.apache.commons.cli.*;

//import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_objdetect.CascadeClassifier;

public class Handbags {

    private static final String OPT_CASCADE_SHORT = "c";
    private static final String OPT_CASCADE_LONG = "cascade";

    private enum ExitCode {
        Normal(0), Error(1);

        private final int _index;

        ExitCode(int index) {
            _index = index;
        }

        public int index() {
            return _index;
        }
    }

    /**
     * @param args
     * @throws ParseException
     */
    public static void main(String[] args) throws ParseException {
        Options opts = initOptions();

        if (args.length == 0) {
            printUsage(opts);
            final int EXIT_NO_ARGS = 1;
            System.exit(EXIT_NO_ARGS);
        }

        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse(opts, args);

        String cascadeFilePath = "";

        if (cl.hasOption(OPT_CASCADE_SHORT))
            cascadeFilePath = cl.getOptionValue(OPT_CASCADE_SHORT);
        else if (cl.hasOption(OPT_CASCADE_LONG))
            cascadeFilePath = cl.getOptionValue(OPT_CASCADE_LONG);
        else {
            System.out.println("Missing argument for cascade file. Assume demo.");
            cascadeFilePath = "demo.xml";
        }

        CascadeClassifier classifier = new CascadeClassifier();

        if (classifier.load(cascadeFilePath) == false) {
            System.out.printf("Can't load cascade file %1$s", cascadeFilePath);
            System.exit(ExitCode.Error.index());
        }
    }

    private static void printUsage(Options opts) {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("Usage", opts);
    }

    private static Options initOptions() {
        Options results = new Options();
        results.addOption(OPT_CASCADE_SHORT, OPT_CASCADE_LONG, true, "Cascade file with classifiers");
        return results;
    }
}