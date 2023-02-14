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
package uk.ac.ebi.ega.egacryptor.cryptography.pgp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.ac.ebi.ega.egacryptor.BaseTest;
import uk.ac.ebi.ega.egacryptor.configuration.EgaCryptorConfiguration;
import uk.ac.ebi.ega.egacryptor.cryptography.Cryptography;
import uk.ac.ebi.ega.egacryptor.exception.CryptographyException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource("classpath:application-test.properties")
@ContextConfiguration(classes = EgaCryptorConfiguration.class)
@ExtendWith(SpringExtension.class)
class PGPCryptographyTest extends BaseTest {

    @TempDir
    private Path temporaryFolder;

    @Autowired
    private Cryptography cryptography;

    @Test
    void encrypt_WhenGivenOutputStream_ThenReturnsPGPOutputStream() throws IOException, CryptographyException {
        final Path subDirs = createSubDirs(temporaryFolder, "path", "to", "process", "files");
        final Path createdFile = createFile(subDirs, "fileToProcess.txt.gpg");

        try (final OutputStream outputStream = Files.newOutputStream(createdFile);
             final OutputStream pgpOutputStream = cryptography.encrypt(outputStream)) {
            assertThat(pgpOutputStream).isNotNull();
        }
    }
}
