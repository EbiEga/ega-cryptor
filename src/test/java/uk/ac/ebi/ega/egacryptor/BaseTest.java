package uk.ac.ebi.ega.egacryptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseTest {

    protected static Path createSubDirs(final Path rootDir,
                                        final String... subDirs)
            throws IOException {

        final Path path = Paths.get(
                rootDir.toAbsolutePath().toString(),
                subDirs);

        final Path dir = Files.createDirectories(path).toAbsolutePath();

        assertThat(dir).exists();

        return dir;
    }

    protected static Path createFile(final Path rootDir,
                                     final String fileName)
            throws IOException {

        final Path fileInRootDir = Paths.get(
                rootDir.toAbsolutePath().toString(),
                fileName);

        final Path file = Files.createFile(fileInRootDir);

        assertThat(file).exists();

        return file;
    }

}
