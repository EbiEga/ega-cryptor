/*
 *
 * Copyright 2019 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.ebi.ega.egacryptor.runner;

import joptsimple.OptionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.ebi.ega.egacryptor.runner.CommandLineOptionParser.FILE_TO_ENCRYPT_PATH;
import static uk.ac.ebi.ega.egacryptor.runner.CommandLineOptionParser.OUTPUT_FOLDER_PATH;
import static uk.ac.ebi.ega.egacryptor.runner.CommandLineOptionParser.USER_THREADS;
import static uk.ac.ebi.ega.egacryptor.runner.CommandLineOptionParser.UTILIZE_FULL_RESOURCE;
import static uk.ac.ebi.ega.egacryptor.runner.CommandLineOptionParser.UTILIZE_HALF_RESOURCE;
import static uk.ac.ebi.ega.egacryptor.runner.CommandLineOptionParser.UTILIZE_OPTIMIZE_RESOURCE;

public class CommandLineOptionProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineOptionProcessor.class);

    private final List<Path> fileToEncryptPaths;
    private final Path outputFolderPath;
    private final int noOfThreads;

    private CommandLineOptionProcessor(final OptionSet optionSet, final Path defaultOutputFilePath) throws FileNotFoundException {
        final String userDefinedOutputFilePath = optionSet.valueOf(OUTPUT_FOLDER_PATH).toString();

        outputFolderPath = StringUtils.hasText(userDefinedOutputFilePath)
                ? Paths.get(userDefinedOutputFilePath).normalize().toAbsolutePath()
                : defaultOutputFilePath.normalize().toAbsolutePath();

        final File outputFolder;

        if (!(outputFolder = outputFolderPath.toFile()).exists() && !outputFolder.mkdirs()) {
            final String message = String.format("Output directory path \"%s\" does not exist " +
                            "and it was not possible to create it either.", outputFolderPath.toAbsolutePath());
            throw new FileNotFoundException(message);
        }

        fileToEncryptPaths = Arrays.asList(optionSet.valueOf(FILE_TO_ENCRYPT_PATH).toString().split(",")).
                parallelStream().map(filePath -> Paths.get(filePath.trim()).normalize().toAbsolutePath()).collect(Collectors.toList());
        noOfThreads = determineNoOfThreads(optionSet);
        LOGGER.info("Maximum {} threads will be created to process the file(s)", noOfThreads);
    }

    static CommandLineOptionProcessor processOptions(final OptionSet optionSet, final Path defaultOutputFilePath) throws IOException {
        return new CommandLineOptionProcessor(optionSet, defaultOutputFilePath);
    }

    private int determineNoOfThreads(final OptionSet optionSet) {
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        LOGGER.info("The application has detected {} cores/processors.", availableProcessors);

        if (availableProcessors == 1) {
            LOGGER.info("Single processor has been detected. File(s) will be processed sequentially. " +
                    "Any provided option will be ignored");
            return 1;
        } else if (optionSet.has(UTILIZE_HALF_RESOURCE)) {
            LOGGER.info("Half resource option has been selected");
            return availableProcessors / 2;
        } else if (optionSet.has(UTILIZE_OPTIMIZE_RESOURCE)) {
            LOGGER.info("Optimized resource option has been selected");
            return (int) (availableProcessors * (75.0f / 100.0f));
        } else if (optionSet.has(UTILIZE_FULL_RESOURCE)) {
            LOGGER.info("Full resource option has been selected");
            return availableProcessors - 1;
        } else if (optionSet.has(USER_THREADS)) {
            LOGGER.info("User defined resource option has been selected");
            final int userDefinedThreads = Integer.parseInt(optionSet.valueOf(USER_THREADS).toString());
            if (userDefinedThreads >= availableProcessors) {
                LOGGER.warn("The requested number of threads is greater than or equal " +
                        "to the number of available cores on this machine. " +
                        "The application will try to use the maximum number of available cores to process the file(s)");
                return availableProcessors - 1;
            } else if (userDefinedThreads <= 0) {
                LOGGER.warn("The requested number of threads is less than or equal to zero. " +
                        "The application will process the file(s) sequentially, using a single thread");
                return 1;
            }
            return userDefinedThreads;
        }
        LOGGER.warn("No option has been provided. Application will process the file(s) sequentially using single thread");
        return 1;
    }

    public List<Path> getFileToEncryptPaths() {
        return fileToEncryptPaths;
    }

    public Path getOutputFolderPath() {
        return outputFolderPath;
    }

    public int getNoOfThreads() {
        return noOfThreads;
    }
}
