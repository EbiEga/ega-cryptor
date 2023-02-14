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
package uk.ac.ebi.ega.egacryptor.pipeline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.ac.ebi.ega.egacryptor.BaseTest;
import uk.ac.ebi.ega.egacryptor.configuration.EgaCryptorConfiguration;
import uk.ac.ebi.ega.egacryptor.model.FileToProcess;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.ac.ebi.ega.egacryptor.cryptography.util.FileUtils.newEmptyPath;

@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(classes = EgaCryptorConfiguration.class)
@ExtendWith(SpringExtension.class)
class DefaultCryptographyPipelineTest extends BaseTest {

    @TempDir
    private Path temporaryFolder;

    @Autowired
    private CryptographyPipeline cryptographyPipeline;

    @Test
    void process_WhenGivenValidFilePathToProcess_ThenEncryptFiles() throws IOException {
        final Path outputFolder = createSubDirs(temporaryFolder, "path", "to", "process", "files");
        final Path createdFile = createFile(outputFolder, "fileToProcess.txt");

        try (final FileOutputStream fileOutputStream = new FileOutputStream(createdFile.toFile())) {
            fileOutputStream.write("File to encrypt".getBytes());
            fileOutputStream.flush();
        }

        final Path outputFolderPath = outputFolder.toAbsolutePath();
        final FileToProcess fileToProcess = new FileToProcess(createdFile.toAbsolutePath(), outputFolderPath);

        cryptographyPipeline.process(fileToProcess);

        assertThat(outputFolderPath.resolve(createdFile.getFileName().toString().concat(".md5"))).exists();
        assertThat(outputFolderPath.resolve(createdFile.getFileName().toString().concat(".gpg"))).exists();
        assertThat(outputFolderPath.resolve(createdFile.getFileName().toString().concat(".gpg.md5"))).exists();
    }

    @Test
    void process_WhenGivenInvalidInputPath_ThenThrowsException() throws IOException {
        final Path outputFolder = createSubDirs(temporaryFolder, "path", "to", "process", "files");

        final FileToProcess fileToProcess = new FileToProcess(newEmptyPath(), outputFolder.toAbsolutePath());

        assertThatThrownBy(
                () -> cryptographyPipeline.process(fileToProcess))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error while processing request")
                .hasRootCauseInstanceOf(IOException.class)
                .hasRootCauseMessage("Is a directory");
    }

    @Test
    void process_WhenGivenInvalidOutputPath_ThenThrowsException() {
        // This emptyPath will be an empty string ("").
        final Path emptyPath = newEmptyPath();

        // The emptyPath is an empty string (""), so the outputFilePath is also an empty string.
        // Since it is not possible to create this outputFilePath, therefore the whole process will fail.
        final FileToProcess fileToProcess = new FileToProcess(emptyPath.toAbsolutePath(), emptyPath);

        assertThatThrownBy(
                () -> cryptographyPipeline.process(fileToProcess))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error while processing request")
                .hasRootCauseMessage("The \"\" directory does not exist and it was not possible to create it, " +
                        "or one of its parent directories.");
    }

    @Test
    void processStopsIfOutputMd5Exists() throws IOException {
        final Path outputFolder = createSubDirs(temporaryFolder, "path", "to", "process", "files");
        final Path outputFolderPath = outputFolder.toAbsolutePath();

        final Path inputFile = createFile(outputFolder, "fileToProcess.txt");
        assertThat(inputFile).exists();

        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".md5"))).doesNotExist();
        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".gpg"))).doesNotExist();
        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".gpg.md5"))).doesNotExist();

        final Path alreadyExistingOutputFile = createFile(outputFolder, "fileToProcess.txt.md5");
        assertThat(alreadyExistingOutputFile).exists();

        final FileToProcess fileToProcess = new FileToProcess(inputFile.toAbsolutePath(), outputFolderPath);

        cryptographyPipeline.process(fileToProcess);

        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".md5"))).exists();
        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".gpg"))).doesNotExist();
        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".gpg.md5"))).doesNotExist();
    }

    @Test
    void processStopsIfOutputGpgExists() throws IOException {
        final Path outputFolder = createSubDirs(temporaryFolder, "path", "to", "process", "files");
        final Path outputFolderPath = outputFolder.toAbsolutePath();

        final Path inputFile = createFile(outputFolder, "fileToProcess.txt");
        assertThat(inputFile).exists();

        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".md5"))).doesNotExist();
        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".gpg"))).doesNotExist();
        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".gpg.md5"))).doesNotExist();

        final Path alreadyExistingOutputFile = createFile(outputFolder, "fileToProcess.txt.gpg");
        assertThat(alreadyExistingOutputFile).exists();

        final FileToProcess fileToProcess = new FileToProcess(inputFile.toAbsolutePath(), outputFolderPath);

        cryptographyPipeline.process(fileToProcess);

        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".md5"))).doesNotExist();
        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".gpg"))).exists();
        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".gpg.md5"))).doesNotExist();
    }

    @Test
    void processStopsIfOutputGpgMd5Exists() throws IOException {
        final Path outputFolder = createSubDirs(temporaryFolder, "path", "to", "process", "files");
        final Path outputFolderPath = outputFolder.toAbsolutePath();

        final Path inputFile = createFile(outputFolder, "fileToProcess.txt");
        assertThat(inputFile).exists();

        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".md5"))).doesNotExist();
        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".gpg"))).doesNotExist();
        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".gpg.md5"))).doesNotExist();

        final Path alreadyExistingOutputFile = createFile(outputFolder, "fileToProcess.txt.gpg.md5");
        assertThat(alreadyExistingOutputFile).exists();

        final FileToProcess fileToProcess = new FileToProcess(inputFile.toAbsolutePath(), outputFolderPath);

        cryptographyPipeline.process(fileToProcess);

        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".md5"))).doesNotExist();
        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".gpg"))).doesNotExist();
        assertThat(outputFolderPath.resolve(inputFile.getFileName().toString().concat(".gpg.md5"))).exists();
    }
}
