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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.ac.ebi.ega.egacryptor.BaseTest;
import uk.ac.ebi.ega.egacryptor.model.FileToProcess;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.ac.ebi.ega.egacryptor.cryptography.util.FileUtils.newEmptyPath;

class FileDiscoveryServiceTest extends BaseTest {

    @TempDir
    private Path temporaryFolder;

    @Test
    void discoverFilesRecursively_WhenCallWithoutOutputDirectory_ReturnsListOfFilesToBeProcessed() throws IOException {
        createSubDirs(temporaryFolder, "path", "to", "process", "files");

        final Path firstCreatedFilePath = createFile(temporaryFolder, "fileToProcessFirst.txt");
        final Path secondCreatedFilePath = createFile(temporaryFolder, "fileToProcessSecond.txt");
        final List<Path> filesToProcess = Arrays.asList(firstCreatedFilePath, secondCreatedFilePath);
        final FileDiscoveryService fileDiscoveryService = new FileDiscoveryService();
        final List<FileToProcess> fileToProcessList = fileDiscoveryService.discoverFilesRecursively(filesToProcess, newEmptyPath());

        assertThat(fileToProcessList.size()).isEqualTo(2);
    }

    @Test
    void discoverFilesRecursively_WhenCallWithOutputDirectory_ReturnsListOfFilesToBeProcessed() throws IOException {
        final Path inputFolderPath = createSubDirs(temporaryFolder, "path", "to", "process", "files");
        final Path outputFolderPath = createSubDirs(temporaryFolder, "output", "folder", "path");

        final Path firstFileToProcessPath = createFile(inputFolderPath, "firstFileToProcess.txt");
        final Path secondFileToProcessPath = createFile(inputFolderPath, "secondFileToProcess.txt");
        final List<Path> fileToProcessPathList = Arrays.asList(firstFileToProcessPath, secondFileToProcessPath);
        final FileDiscoveryService fileDiscoveryService = new FileDiscoveryService();
        final List<FileToProcess> fileToProcessList = fileDiscoveryService.discoverFilesRecursively(fileToProcessPathList, outputFolderPath);

        assertThat(fileToProcessList.size()).isEqualTo(2);

        final FileToProcess firstFileToProcess = fileToProcessList.get(0);

        assertThat(firstFileToProcess.getFileToEncryptPath().toString()).endsWith("path/to/process/files/firstFileToProcess.txt");
        assertThat(firstFileToProcess.getOutputFilePath().toString()).endsWith("output/folder/path");

        final FileToProcess secondFileToProcess = fileToProcessList.get(1);

        assertThat(secondFileToProcess.getFileToEncryptPath().toString()).endsWith("path/to/process/files/secondFileToProcess.txt");
        assertThat(secondFileToProcess.getOutputFilePath().toString()).endsWith("output/folder/path");
    }

    @Test
    void discoverFilesRecursively_WhenCallWithEmptyFilePath_ReturnsListFilesInBaseDirectory() {
        final FileDiscoveryService fileDiscoveryService = new FileDiscoveryService();

        final List<FileToProcess> filesToProcess = fileDiscoveryService.discoverFilesRecursively(
                Collections.singletonList(newEmptyPath()), newEmptyPath());

        assertThat(filesToProcess)
                .isNotEmpty()
                .hasSizeGreaterThan(100)
                .contains(
                        new FileToProcess(Paths.get("src/main/resources/application.properties")),
                        new FileToProcess(Paths.get("target/classes/uk/ac/ebi/ega/egacryptor/model/FileToProcess.class"))
                );
    }

    @Test
    void discoverFilesRecursively_WhenCallDotAsFilePath_ReturnsListFilesInBaseDirectory() {
        final FileDiscoveryService fileDiscoveryService = new FileDiscoveryService();

        final List<FileToProcess> filesToProcess = fileDiscoveryService.discoverFilesRecursively(
                Collections.singletonList(Paths.get(".")), newEmptyPath());

        assertThat(filesToProcess)
                .isNotEmpty()
                .hasSizeGreaterThan(100)
                .contains(
                        new FileToProcess(
                                Paths.get("./src/main/resources/application.properties"),
                                Paths.get("./src/main/resources")),
                        new FileToProcess(
                                Paths.get("./target/classes/uk/ac/ebi/ega/egacryptor/model/FileToProcess.class"),
                                Paths.get("./target/classes/uk/ac/ebi/ega/egacryptor/model"))
                );
    }

    // EE-2667: it can happen that the source directory contains not only the source file (e.g. sourceFile.bam),
    // but an accompanying MD5 file too (i.e. sourceFile.bam.md5). Previously, if this happened,
    // then the sourceFile.bam was skipped, which caused a lot of headaches (at least) to one user.
    // The current implementation allows accompanying files (i.e. sourceFile.bam.md5, sourceFile.bam.gpg,
    // sourceFile.bam.gpg.md5) to be present next to the source file.
    // This test tests the current implementation.
    @ParameterizedTest
    @ValueSource(strings = {"md5", "gpg", "gpg.md5"})
    void fileIsDiscoveredEvenIfSourceMD5Exists(final String accompanyingFileExtension) throws IOException {
        createSubDirs(temporaryFolder, "path", "to", "process", "files");

        final Path mainFile = createFile(temporaryFolder, "fileToProcessFirst.txt");
        final Path accompanyingFile = createFile(temporaryFolder,
                "fileToProcessFirst.txt." + accompanyingFileExtension);

        final List<Path> filesToProcess = Arrays.asList(mainFile, accompanyingFile);
        final FileDiscoveryService fileDiscoveryService = new FileDiscoveryService();

        final List<FileToProcess> fileToProcessList = fileDiscoveryService.discoverFilesRecursively(
                filesToProcess, newEmptyPath());

        assertThat(fileToProcessList)
                .containsExactly(new FileToProcess(mainFile));
    }

    @ParameterizedTest
    @ValueSource(strings = {"md5", "jar", "gpg"})
    void notPermittedFilesAreSkipped(final String notPermittedFileExtension) throws IOException {
        createSubDirs(temporaryFolder, "path", "to", "process", "files");

        final Path notPermittedFile = createFile(temporaryFolder,
                "fileToProcessFirst.txt." + notPermittedFileExtension);
        final List<Path> filesToProcess = Collections.singletonList(notPermittedFile);

        final FileDiscoveryService fileDiscoveryService = new FileDiscoveryService();

        final List<FileToProcess> fileToProcessList = fileDiscoveryService.discoverFilesRecursively(
                filesToProcess, newEmptyPath());

        assertThat(fileToProcessList).isEmpty();
    }

    @Test
    void directoriesAreSkipped_ButFilesInSubDirsAreNotSkipped() throws IOException {
        final Path dir3 = createSubDirs(temporaryFolder, "1", "2", "3");
        final Path md5File = createFile(dir3, "fileToProcessFirst.txt");

        final Path dir2 = dir3.getParent();
        final List<Path> dirToProcess = Collections.singletonList(dir2);

        final FileDiscoveryService fileDiscoveryService = new FileDiscoveryService();

        final List<FileToProcess> discoveredFiles = fileDiscoveryService.discoverFilesRecursively(dirToProcess, newEmptyPath());

        assertThat(discoveredFiles)
                .containsExactly(new FileToProcess(md5File.toAbsolutePath()));
    }

    @Test
    void hiddenFilesAreSkipped() throws IOException {
        createSubDirs(temporaryFolder, "path", "to", "process", "files");

        final Path hiddenFile = createFile(temporaryFolder, ".hiddenFile.txt");
        final List<Path> filesToProcess = Collections.singletonList(hiddenFile);
        final FileDiscoveryService fileDiscoveryService = new FileDiscoveryService();

        final List<FileToProcess> fileToProcessList = fileDiscoveryService.discoverFilesRecursively(filesToProcess, newEmptyPath());

        assertThat(fileToProcessList).isEmpty();
    }

    @Test
    void filesInHiddenDirsAreNotSkipped() throws IOException {
        createSubDirs(temporaryFolder, "path", "to", ".hiddenDir", "files");

        final Path normalFile = createFile(temporaryFolder, "file.txt");
        final List<Path> filesToProcess = Collections.singletonList(normalFile);
        final FileDiscoveryService fileDiscoveryService = new FileDiscoveryService();

        final List<FileToProcess> discoveredFiles = fileDiscoveryService.discoverFilesRecursively(filesToProcess, newEmptyPath());

        assertThat(discoveredFiles)
                .containsExactly(new FileToProcess(normalFile.toAbsolutePath()));
    }
}
