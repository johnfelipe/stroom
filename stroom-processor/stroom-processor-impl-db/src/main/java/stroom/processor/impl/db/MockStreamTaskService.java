/*
 * Copyright 2017 Crown Copyright
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
 *
 */

package stroom.processor.impl.db;

import stroom.entity.MockEntityService;
import stroom.util.shared.BaseResultList;
import stroom.util.shared.SummaryDataRow;
import stroom.streamtask.shared.FindStreamTaskCriteria;
import stroom.processor.shared.ProcessorFilterTask;

import javax.inject.Singleton;

/**
 * Mock object.
 * <p>
 * In memory simple process manager that also uses the mock stream store.
 */
@Singleton
public class MockStreamTaskService extends MockEntityService<ProcessorFilterTask, FindStreamTaskCriteria>
        implements StreamTaskService {
    @Override
    public BaseResultList<SummaryDataRow> findSummary(final FindStreamTaskCriteria criteria) {
        return null;
    }

    @Override
    public Class<ProcessorFilterTask> getEntityClass() {
        return ProcessorFilterTask.class;
    }
}