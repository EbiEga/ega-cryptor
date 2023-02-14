package uk.ac.ebi.ega.egacryptor.service;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.ega.egacryptor.model.FileToProcess;
import uk.ac.ebi.ega.egacryptor.pipeline.CryptographyPipeline;

import java.nio.file.Paths;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TaskExecutorServiceTest {

    private static final int NUMBER_OF_THREADS = 13;

    private static final FileToProcess FILE_TO_PROCESS = new FileToProcess(Paths.get("asd"));

    private CryptographyPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = mock(CryptographyPipeline.class);
    }


    @Test
    void taskExecutorServiceCorrectlyHandlesEmptyList() {
        final ITaskExecutorService service = new TaskExecutorService(pipeline);

        service.execute(Lists.emptyList());

        verify(pipeline, never()).process(any());
    }

    // EE-2663
    @Test
    void taskExecutorServiceCorrectlyHandlesEmptyListAndNumberOfThreads() {
        final ITaskExecutorService service = new TaskExecutorService(pipeline);

        service.execute(Lists.emptyList(), NUMBER_OF_THREADS);

        verify(pipeline, never()).process(any());
    }

    @Test
    void taskExecutorServiceCallsPipelineWithFileToProcess() {
        final ITaskExecutorService service = new TaskExecutorService(pipeline);

        service.execute(Collections.singletonList(FILE_TO_PROCESS));

        verify(pipeline).process(FILE_TO_PROCESS);
    }

    @Test
    void taskExecutorServiceCallsPipelineWithFileToProcessAndNumberOfThreads() {
        final ITaskExecutorService service = new TaskExecutorService(pipeline);

        service.execute(Collections.singletonList(FILE_TO_PROCESS), NUMBER_OF_THREADS);

        verify(pipeline).process(FILE_TO_PROCESS);
    }

    @Test
    void taskExecutorServiceCallsPipelineWithFilesToProcess() {
        final ITaskExecutorService service = new TaskExecutorService(pipeline);

        service.execute(Lists.list(FILE_TO_PROCESS, FILE_TO_PROCESS));

        verify(pipeline, times(2)).process(FILE_TO_PROCESS);
    }

    @Test
    void taskExecutorServiceCallsPipelineWithFilesToProcessAndNumberOfThreads() {
        final ITaskExecutorService service = new TaskExecutorService(pipeline);

        service.execute(Lists.list(FILE_TO_PROCESS, FILE_TO_PROCESS), NUMBER_OF_THREADS);

        verify(pipeline, times(2)).process(FILE_TO_PROCESS);
    }
}
