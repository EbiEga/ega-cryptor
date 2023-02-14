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
package uk.ac.ebi.ega.egacryptor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.egacryptor.constant.FileExtensionType;
import uk.ac.ebi.ega.egacryptor.model.FileToProcess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FileDiscoveryService implements IFileDiscoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDiscoveryService.class);

    @Override
    public List<FileToProcess> discoverFilesRecursively(final List<Path> rootFilePaths, final Path outputFilePath) {
        LOGGER.trace("Executing file discovery service. rootFilePaths: \"{}\" , outputFilePath: \"{}\" ...",
                rootFilePaths, outputFilePath);

        return rootFilePaths.stream()
                .map(rootFilePath -> discoverFiles(rootFilePath, outputFilePath))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<FileToProcess> discoverFiles(final Path rootFilePath, final Path outputFilePath) {
        try (final Stream<Path> paths = Files.walk(rootFilePath)) {
            return paths
                    .filter(this::isValidFilePath)
                    .map(validFilePath -> calculateOutputPath(rootFilePath, validFilePath, outputFilePath))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Error in file discovery - {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean isValidFilePath(final Path path) {
        LOGGER.debug("Checking filePath whether it is valid: {} ...", path);

        if (!Files.isRegularFile(path)) {
            return false;
        }

        try {
            if (Files.isHidden(path)) {
                LOGGER.warn("The {} file is skipped because it is hidden.", path);
                return false;
            }
        } catch (IOException exception) {
            LOGGER.error("Exception while checking the {} path: ", exception.getMessage());
            return false;
        }

        if (FileExtensionType.containsFileExtension(path.toString())) {
            LOGGER.warn("The {} file is skipped because its extension " +
                    "is in the list of not allowed extensions: {}", path, FileExtensionType.values());
            return false;
        }

        if (Files.isDirectory(path)) {
            LOGGER.warn("The {} file is skipped because it is a directory.", path);
            return false;
        }

        return true;
    }

    private FileToProcess calculateOutputPath(final Path rootFilePath, final Path subFilePath, final Path outputFilePath) {
        if (outputFilePath.toString().isEmpty()) {
            return new FileToProcess(subFilePath);
        }

        final Path subPathMinusRootPath;
        if (!subFilePath.equals(rootFilePath) &&
                (subPathMinusRootPath = subFilePath.subpath(rootFilePath.getNameCount(), subFilePath.getNameCount())).getParent() != null) {
            final Path newOutputFilePath = outputFilePath.resolve(subPathMinusRootPath.getParent());
            return new FileToProcess(subFilePath, newOutputFilePath);
        }
        return new FileToProcess(subFilePath, outputFilePath);
    }
}
