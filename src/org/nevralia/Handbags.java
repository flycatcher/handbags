/**
 * Copyright (c) 2012 Jun Mei
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.nevralia;

import static com.googlecode.javacv.cpp.opencv_core.cvSize;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_objdetect.CascadeClassifier;

public class Handbags {

    private static final String OPT_CASCADE_SHORT = "c";
    private static final String OPT_CASCADE_LONG = "cascade";
    private static final String OPT_INFO_SHORT = "i";
    private static final String OPT_INFO_LONG = "info";
    private static final String OPT_WIDTH_SHORT = "w";
    private static final String OPT_WIDTH_LONG = "width";
    private static final String OPT_HEIGHT_SHORT = "h";
    private static final String OPT_HEIGHT_LONG = "height";
    private static final String OPT_SCALE_SHORT = "s";
    private static final String OPT_SCALE_LONG = "scale";

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
    public static void main(String[] args) throws ParseException,
        FileNotFoundException {
        Options opts = initOptions();

        if (args.length == 0) {
            printUsage(opts);
            final int EXIT_NO_ARGS = 1;
            System.exit(EXIT_NO_ARGS);
        }

        CommandLineParser parser = new GnuParser();
        CommandLine cl = parser.parse(opts, args);
        CascadeClassifier classifier = initClassifier(cl);
        List<String> imagePaths = initImages(cl);
        Collections.shuffle(imagePaths);
        int width = initWidth(cl);
        int height = initHeight(cl);
        float scale = initScale(cl);

        run(classifier, imagePaths, width, height, scale);
    }

    /**
     * Uses the given classifier to identify relevant objects for each image in
     * the given list
     * 
     * @param classifier
     *            The cascade classifier
     * @param images
     *            The list of images to be analyzed
     * @param w
     *            Minimum possible object width (number of pixels); objects
     *            smaller than this will be ignored
     * @param h
     *            Minimum possible object height (number of pixels); objects
     *            smaller than this will be ignored
     * @param scale
     *            A multiplier used to determine the maximum possible object
     *            size; objects larger than that will be ignored
     */
    private static void run(final CascadeClassifier classifier,
        final List<String> images, final int w, final int h, float scale) {
        final CvSize min = cvSize(w, h);
        final CvSize max = cvSize((int) (w * scale), (int) (h * scale));

        ForkJoinPool pool = new ForkJoinPool();
        ImageProcessor action = new ImageProcessor(classifier, images, 0,
            images.size(), min, max);

        pool.invoke(action);
    }

    /**
     * Parses and returns the maximum scaling factor from the command line
     * 
     * @param cl
     * @return
     */
    private static float initScale(final CommandLine cl) {
        final float DEFAULT_SCALE_FACTOR = 10f;
        float result = DEFAULT_SCALE_FACTOR;

        if (cl.hasOption(OPT_SCALE_SHORT)) {
            String str = cl.getOptionValue(OPT_SCALE_SHORT);
            result = Float.parseFloat(str);
        } else if (cl.hasOption(OPT_SCALE_LONG)) {
            String str = cl.getOptionValue(OPT_SCALE_LONG);
            result = Float.parseFloat(str);
        }

        return (result > 1f) ? result : DEFAULT_SCALE_FACTOR;
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

    private static List<String> initImages(final CommandLine cl) {
        String sourceFilePath = "";

        if (cl.hasOption(OPT_INFO_SHORT))
            sourceFilePath = cl.getOptionValue(OPT_INFO_SHORT);
        else if (cl.hasOption(OPT_INFO_LONG))
            sourceFilePath = cl.getOptionValue(OPT_INFO_LONG);
        else
            sourceFilePath = "default.dat";

        List<String> result = new ArrayList<>();

        try (BufferedReader buffer = new BufferedReader(new FileReader(
            sourceFilePath))) {
            String line = null;
            while ((line = buffer.readLine()) != null) {
                File file = new File(line);

                if (file.exists() && file.canRead()) {
                    result.add(line);
                }
            }
        } catch (FileNotFoundException ex) {
            System.err.printf("Cannot find info file %1$s\n", sourceFilePath);
            System.exit(ExitCode.Error.index());
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
            System.out
                .println("Missing argument for cascade file. Assume demo.");
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
        help.printHelp(
            "handbag -i [filename] -c [filename] -w [integer] -h [integer] -s [positive number, default 10]",
            opts);
    }

    private static Options initOptions() {
        Options results = new Options();
        results.addOption(OPT_CASCADE_SHORT, OPT_CASCADE_LONG, true,
            "Cascade file with classifiers");
        results.addOption(OPT_INFO_SHORT, OPT_INFO_LONG, true,
            "Collection of input files");
        results.addOption(OPT_WIDTH_SHORT, OPT_WIDTH_LONG, true,
            "Minimum object width");
        results.addOption(OPT_HEIGHT_SHORT, OPT_HEIGHT_LONG, true,
            "Minimum object height");
        results.addOption(OPT_SCALE_SHORT, OPT_SCALE_LONG, true,
            "Scale factor for the maximum object size (default 10)");
        return results;
    }
}