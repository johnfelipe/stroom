/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.pipeline.task;


import org.junit.jupiter.api.Test;
import stroom.data.store.impl.mock.MockStore;
import stroom.core.dataprocess.PipelineDataProcessorTaskExecutor;
import stroom.meta.impl.mock.MockMetaService;
import stroom.meta.shared.Meta;
import stroom.processor.api.DataProcessorTaskExecutor;
import stroom.data.shared.StreamTypeNames;
import stroom.test.AbstractProcessIntegrationTest;
import stroom.test.CommonTranslationTestHelper;
import stroom.test.common.ComparisonHelper;
import stroom.test.common.StroomPipelineTestFileUtil;
import stroom.util.shared.Severity;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;

import static org.assertj.core.api.Assertions.assertThat;

class TestTranslationTask extends AbstractProcessIntegrationTest {
    private static final int N3 = 3;
    private static final int N4 = 4;

    private static final String DIR = "TestTranslationTask/";

    @Inject
    private MockMetaService metaService;
    @Inject
    private MockStore streamStore;
    @Inject
    private CommonTranslationTestHelper commonTranslationTestHelper;

    /**
     * Tests Task with a valid resource and feed.
     *
     * @throws IOException Could be thrown.
     */
    @Test
    void testBothValid() throws IOException {
        commonTranslationTestHelper.setup();

        final List<DataProcessorTaskExecutor> results = commonTranslationTestHelper.processAll();
        assertThat(results.size()).isEqualTo(N4);
        for (final DataProcessorTaskExecutor result : results) {
            final PipelineDataProcessorTaskExecutor processor = (PipelineDataProcessorTaskExecutor) result;
            assertThat(processor.getWritten() > 0).as(result.toString()).isTrue();
            assertThat(processor.getRead() <= processor.getWritten()).as(result.toString()).isTrue();
            assertThat(processor.getMarkerCount(Severity.SEVERITIES)).as(result.toString()).isEqualTo(0);
        }

        final Path inputDir = StroomPipelineTestFileUtil.getTestResourcesDir().resolve(DIR);
        final Path outputDir = StroomPipelineTestFileUtil.getTestOutputDir().resolve(DIR);

        for (final Entry<Long, Meta> entry : metaService.getMetaMap().entrySet()) {
            final long streamId = entry.getKey();
            final Meta meta = entry.getValue();
            if (StreamTypeNames.EVENTS.equals(meta.getTypeName())) {
                final byte[] data = streamStore.getFileData().get(streamId).get(meta.getTypeName());

                // Write the actual XML out.
                final OutputStream os = StroomPipelineTestFileUtil.getOutputStream(outputDir, "TestTranslationTask.out");
                os.write(data);
                os.flush();
                os.close();

                ComparisonHelper.compareFiles(inputDir.resolve("TestTranslationTask.out"),
                        outputDir.resolve("TestTranslationTask.out"));
            }
        }

        // Make sure 26 records were written.
        assertThat(((PipelineDataProcessorTaskExecutor) results.get(N3)).getWritten()).isEqualTo(26);
    }

    /**
     * Tests Task with an invalid resource and valid feed.
     *
     * @throws IOException Could be thrown.
     */
    @Test
    void testInvalidResource() {
        commonTranslationTestHelper.setup(CommonTranslationTestHelper.FEED_NAME, CommonTranslationTestHelper.INVALID_RESOURCE_NAME);

        final List<DataProcessorTaskExecutor> results = commonTranslationTestHelper.processAll();
        assertThat(results.size()).isEqualTo(N4);

        // Make sure no records were written.
        assertThat(((PipelineDataProcessorTaskExecutor) results.get(N3)).getWritten()).isEqualTo(0);
    }
}
