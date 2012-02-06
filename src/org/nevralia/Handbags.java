package org.nevralia;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;

import com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_objdetect.CascadeClassifier;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_objdetect.*;

/**
 * 
 * @author Jun Mei
 *
 */
public class Handbags {

    private static final String OPT_CASCADE_SHORT = "c";
    private static final String OPT_CASCADE_LONG = "cascade";
    private static final String OPT_INFO_SHORT = "i";
    private static final String OPT_INFO_LONG = "info";
    private static final String OPT_WIDTH_SHORT = "w";
    private static final String OPT_WIDTH_LONG = "width";
    private static final String OPT_HEIGHT_SHORT = "h";
    private static final String OPT_HEIGHT_LONG = "height";

    /**
     * Enumerations for exit codes
     */
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
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws ParseException, FileNotFoundException {
        Options opts = initOptions();

        if (args.length == 0) {
            printUsage(opts);
            final int EXIT_NO_ARGS = 1;
            System.exit(EXIT_NO_ARGS);
        }

        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse(opts, args);
        CascadeClassifier classifier = initClassifier(cl);
        Collection<String> imagePaths = initImages(cl);
        int width = initWidth(cl);
        int height = initHeight(cl);

        test(classifier, imagePaths, width, height);
    }

    private static void test(CascadeClassifier classifier, Collection<String> imagePaths, int width, int height) {
        final int OUT_FACTOR = 8;
        final CvSize minSize = cvSize(width, height);
        final CvSize maxSize = cvSize(width * OUT_FACTOR, height * OUT_FACTOR);

        for (String imagePath : imagePaths) {
            IplImage image = cvLoadImage(imagePath, CV_LOAD_IMAGE_GRAYSCALE);
            CvRect matches = new CvRect(null);
            classifier.detectMultiScale(image, matches, 1.1, 2, 0 | CV_HAAR_SCALE_IMAGE, minSize, maxSize);
            printResults(imagePath, matches);
        }
    }

    private static void printResults(String imagePath, CvRect matches) {
        int numMatches = matches.capacity();
        System.out.printf("%1$s %2$d match(es)", imagePath, numMatches);
        for (int i = 0; i < numMatches; ++i) {
            CvRect match = matches.position(i);
            System.out.printf(" [%1$d %2$d %3$d %4$d]", match.x(), match.y(), match.width(), match.height());
        }
        System.out.println("");
    }

    private static int initHeight(final CommandLine cl) {
        int result = 20;

        if (cl.hasOption(OPT_HEIGHT_SHORT)) {
            String str = cl.getOptionValue(OPT_HEIGHT_SHORT);
            result = Integer.parseInt(str);
        } else if (cl.hasOption(OPT_HEIGHT_LONG)) {
            String str = cl.getOptionValue(OPT_HEIGHT_LONG);
            result = Integer.parseInt(str);
        }

        return result;
    }

    private static int initWidth(final CommandLine cl) {
        int result = 20;

        if (cl.hasOption(OPT_WIDTH_SHORT)) {
            String str = cl.getOptionValue(OPT_WIDTH_SHORT);
            result = Integer.parseInt(str);
        } else if (cl.hasOption(OPT_WIDTH_LONG)) {
            String str = cl.getOptionValue(OPT_WIDTH_LONG);
            result = Integer.parseInt(str);
        }

        return result;
    }

    private static Collection<String> initImages(final CommandLine cl) {
        String sourceFilePath = "";

        if (cl.hasOption(OPT_INFO_SHORT))
            sourceFilePath = cl.getOptionValue(OPT_INFO_SHORT);
        else if (cl.hasOption(OPT_INFO_LONG))
            sourceFilePath = cl.getOptionValue(OPT_INFO_LONG);
        else
            sourceFilePath = "default.dat";

        FileReader reader = null;

        try {
            reader = new FileReader(sourceFilePath);
        } catch (FileNotFoundException ex) {
            System.err.printf("Cannot find info file %1$s\n", sourceFilePath);
            System.exit(ExitCode.Error.index());
        }

        BufferedReader buffer = new BufferedReader(reader);
        List<String> result = new ArrayList<String>();

        try {
            String line = null;
            while ((line = buffer.readLine()) != null) {
                File file = new File(line);

                if (file.exists() && file.canRead()) {
                    result.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(ExitCode.Error.index());
        }

        return result;
    }

    private static CascadeClassifier initClassifier(final CommandLine cl) {
        String cascadeFilePath = "";

        if (cl.hasOption(OPT_CASCADE_SHORT))
            cascadeFilePath = cl.getOptionValue(OPT_CASCADE_SHORT);
        else if (cl.hasOption(OPT_CASCADE_LONG))
            cascadeFilePath = cl.getOptionValue(OPT_CASCADE_LONG);
        else {
            System.out.println("Missing argument for cascade file. Assume demo.");
            cascadeFilePath = "demo.xml";
        }

        CascadeClassifier result = new CascadeClassifier();

        if (result.load(cascadeFilePath) == false) {
            System.out.printf("Can't load cascade file %1$s", cascadeFilePath);
            System.exit(ExitCode.Error.index());
        }

        return result;
    }

    private static void printUsage(Options opts) {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("", opts);
    }

    private static Options initOptions() {
        Options results = new Options();
        results.addOption(OPT_CASCADE_SHORT, OPT_CASCADE_LONG, true, "Cascade file with classifiers");
        results.addOption(OPT_INFO_SHORT, OPT_INFO_LONG, true, "Collection of input files");
        results.addOption(OPT_WIDTH_SHORT, OPT_WIDTH_LONG, true, "Minimum object width");
        results.addOption(OPT_HEIGHT_SHORT, OPT_HEIGHT_LONG, true, "Minimum object height");
        return results;
    }
}