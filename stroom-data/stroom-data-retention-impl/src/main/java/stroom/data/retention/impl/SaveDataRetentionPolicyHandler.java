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

package stroom.data.retention.impl;

import stroom.data.retention.shared.DataRetentionRules;
import stroom.data.retention.shared.SaveDataRetentionRulesAction;
import stroom.security.api.Security;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;


class SaveDataRetentionPolicyHandler extends AbstractTaskHandler<SaveDataRetentionRulesAction, DataRetentionRules> {
    private final DataRetentionRulesService dataRetentionRulesService;
    private final Security security;

    @Inject
    SaveDataRetentionPolicyHandler(final DataRetentionRulesService dataRetentionRulesService,
                                   final Security security) {
        this.dataRetentionRulesService = dataRetentionRulesService;
        this.security = security;
    }

    @Override
    public DataRetentionRules exec(final SaveDataRetentionRulesAction task) {
        return security.secureResult(() -> dataRetentionRulesService.writeDocument(task.getDataRetentionRules()));
    }
}
