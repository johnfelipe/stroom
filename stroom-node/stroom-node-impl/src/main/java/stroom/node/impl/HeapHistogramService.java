package stroom.node.impl;

import com.google.common.base.Preconditions;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class for generating a java heap map histogram using the 'jmap' tool supplied with the JDK. Requires that
 * jmap is available on the filesystem and executable by this java process.
 */
@Singleton
@SuppressWarnings("unused")
class HeapHistogramService {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeapHistogramService.class);

    private static final int STRING_TRUNCATE_LIMIT = 200;
    private static String ID_REPLACEMENT = "--ID-REMOVED--";

    private final HeapHistogramConfig heapHistogramConfig;
    private final Pattern lineMatchPattern;

    @SuppressWarnings("unused")
    @Inject
    HeapHistogramService(final HeapHistogramConfig heapHistogramConfig) {
        this.heapHistogramConfig = heapHistogramConfig;
        this.lineMatchPattern = Pattern.compile("\\s*\\d+:\\s+(?<instances>\\d+)\\s+(?<bytes>\\d+)\\s+(?<class>.*)");
    }

    /**
     * Generates a jmap heap histogram by calling the 'jmap' binary on the filesystem.  Will
     * block until jmap completes/fails. Reads the content of stdout and parses it to return a
     * list of {@link HeapHistogramEntry}
     */
    List<HeapHistogramEntry> generateHeapHistogram() {
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

        String executable = getExecutable();
        CommandLine command = new CommandLine(executable);
        command.addArguments(new String[]{"-histo:live", pid}, false);

        DefaultExecutor executor = new DefaultExecutor();
        executor.setExitValue(0);
        ExecuteWatchdog watchdog = new ExecuteWatchdog(120000);
        executor.setWatchdog(watchdog);
        //ensure the process is killed if stroom is shutting down
        executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());

        ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
        ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(stdOut, stdErr);
        executor.setStreamHandler(pumpStreamHandler);

        final List<HeapHistogramEntry> heapHistogramEntries;
        try {
            LOGGER.info("Executing a heap histogram using command [{}]", command.toString());
            int exitCode = executor.execute(command);
            if (exitCode == 0) {
                heapHistogramEntries = processSuccess(stdOut, stdErr);
            } else {
                logError(stdOut, stdErr, watchdog);
                heapHistogramEntries = Collections.emptyList();
            }
        } catch (final IOException e) {
            logError(stdOut, stdErr, watchdog);
            throw new RuntimeException(String.format("Error executing command %s", command.toString()), e);
        }
        return heapHistogramEntries;
    }

    private String getExecutable() {
        String executable = heapHistogramConfig.getjMapExecutable();

        if (executable == null || executable.isEmpty()) {
            throw new RuntimeException("Property jMapExecutable has no value");
        }
        return executable;
    }

    private void logError(final ByteArrayOutputStream stdOut,
                          final ByteArrayOutputStream stdErr,
                          final ExecuteWatchdog watchdog) {
        if (watchdog != null && watchdog.killedProcess()) {
            LOGGER.error("The jmap call timed out");
        } else {
            String stdOutStr;
            String stdErrStr;
            try {
                stdOutStr = getTruncatedStr(getStringOutput(stdOut));
            } catch (final RuntimeException e) {
                stdOutStr = "Unable to get stdOut str due to error " + e.getMessage();
            }
            try {
                stdErrStr = getTruncatedStr(getStringOutput(stdErr));
            } catch (final RuntimeException e) {
                stdErrStr = "Unable to get stdErr str due to error " + e.getMessage();
            }
            LOGGER.error("The jmap call failed with stdout [%s] and stderr [%s]", stdOutStr, stdErrStr);
        }
    }

    private List<HeapHistogramEntry> processSuccess(final ByteArrayOutputStream stdOut,
                                                    final ByteArrayOutputStream stdErr) {
        String result;
        String error;
        List<HeapHistogramEntry> heapHistogramEntries = null;
        try {
            result = getStringOutput(stdOut);
            error = getStringOutput(stdErr);

            if (error != null && !error.isEmpty()) {
                throw new RuntimeException(String.format("jmap completed with exit code 0 but stderr is not empty [%s]",
                        getTruncatedStr(error)));
            } else if (result == null || result.isEmpty()) {
                throw new RuntimeException("jmap completed with exit code 0 but stdout is empty");
            }
            heapHistogramEntries = processStdOut(result);

        } catch (RuntimeException e) {
            LOGGER.error("Error handling result of jmap call", e);
        }
        return heapHistogramEntries;
    }

    private String getStringOutput(final ByteArrayOutputStream outputStream) {
        try {
            return outputStream.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error extracting stream output as UTF-8", e);
        }
    }

    private static String getTruncatedStr(final String str) {
        if (str != null && str.length() > STRING_TRUNCATE_LIMIT) {
            return str.substring(0, STRING_TRUNCATE_LIMIT) + "...TRUNCATED...";
        } else {
            return str;
        }
    }

    private Predicate<String> getClassNameMatchPredicate() {
        String classNameRegexStr = heapHistogramConfig.getClassNameMatchRegex();

        if (classNameRegexStr == null || classNameRegexStr.isEmpty()) {
            //no prop value so return an always true predicate
            return str -> true;
        } else {
            try {
                return Pattern.compile(classNameRegexStr).asPredicate();
            } catch (final RuntimeException e) {
                throw new RuntimeException(String.format("Error compiling regex string [%s]", classNameRegexStr), e);
            }
        }
    }

    private Function<String, String> getClassReplacementMapper() {
        final String anonymousIdRegex = heapHistogramConfig.getClassNameReplacementRegex();

        if (anonymousIdRegex == null || anonymousIdRegex.isEmpty()) {
            return Function.identity();
        } else {
            try {
                final Pattern pattern = Pattern.compile(anonymousIdRegex);
                return className -> pattern.matcher(className).replaceAll(ID_REPLACEMENT);
            } catch (final RuntimeException e) {
                LOGGER.error("Value [{}] for property [{}] is not valid regex",
                        anonymousIdRegex, "classNameReplacementRegex", e);
                return Function.identity();
            }
        }
    }

    private Function<String, Optional<HeapHistogramEntry>> buildLineToEntryMapper(final Function<String, String> classNameReplacer) {
        Preconditions.checkNotNull(classNameReplacer);
        return line -> {
            Matcher matcher = lineMatchPattern.matcher(line);
            if (matcher.matches()) {
                //if this is a data row then extract the values of interest
                final long instances = Long.parseLong(matcher.group("instances"));
                final long bytes = Long.parseLong(matcher.group("bytes"));
                final String className = matcher.group("class");
                final String newClassName = classNameReplacer.apply(className);
                LOGGER.trace("className [{}], newClassName [{}]", className, newClassName);

                return Optional.of(new HeapHistogramEntry(newClassName, instances, bytes));
            } else {
                LOGGER.trace("Ignoring jamp histogram line [{}]", line);
                return Optional.empty();
            }
        };
    }

    private List<HeapHistogramEntry> processStdOut(final String stdOut) {
        Preconditions.checkNotNull(stdOut);

        try {
            Predicate<String> classNamePredicate = getClassNameMatchPredicate();
            Function<String, String> classNameReplacer = getClassReplacementMapper();
            Function<String, Optional<HeapHistogramEntry>> lineToEntryMapper = buildLineToEntryMapper(classNameReplacer);

            String[] lines = stdOut.split("\\r?\\n");

            LOGGER.debug("processing {} lines of stdout", lines.length);

            final List<HeapHistogramService.HeapHistogramEntry> histogramEntries = Arrays.stream(lines)
                    .map(lineToEntryMapper)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(heapHistogramEntry -> classNamePredicate.test(heapHistogramEntry.getClassName()))
                    .collect(Collectors.toList());

            LOGGER.debug("histogramEntries size [%s]", histogramEntries.size());
            if (histogramEntries.size() == 0) {
                LOGGER.error("Something has gone wrong filtering the heap histogram, zero entries returned");
            }
            return histogramEntries;

        } catch (final RuntimeException e) {
            throw new RuntimeException(String.format("Error processing stdOut string [%s]",
                    getTruncatedStr(stdOut)), e);
        }
    }

    static class HeapHistogramEntry {
        private final String className;
        private final long instances;
        private final long bytes;

        HeapHistogramEntry(final String className, final long instances, final long bytes) {
            this.className = Preconditions.checkNotNull(className);
            this.instances = instances;
            this.bytes = bytes;
        }

        String getClassName() {
            return className;
        }

        long getInstances() {
            return instances;
        }

        long getBytes() {
            return bytes;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final HeapHistogramEntry that = (HeapHistogramEntry) o;

            if (instances != that.instances) return false;
            if (bytes != that.bytes) return false;
            return className.equals(that.className);
        }

        @Override
        public int hashCode() {
            int result = className.hashCode();
            result = 31 * result + (int) (instances ^ (instances >>> 32));
            result = 31 * result + (int) (bytes ^ (bytes >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "HeapHistogramEntry{" +
                    "className='" + className + '\'' +
                    ", instances=" + instances +
                    ", bytes=" + bytes +
                    '}';
        }
    }
}
