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

package stroom.explorer.impl;

import stroom.explorer.api.ExplorerService;
import stroom.explorer.shared.ExplorerServiceRenameAction;
import stroom.explorer.shared.SharedDocRef;
import stroom.security.api.Security;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;


class ExplorerServiceRenameHandler
        extends AbstractTaskHandler<ExplorerServiceRenameAction, SharedDocRef> {
    private final ExplorerService explorerService;
    private final Security security;

    @Inject
    ExplorerServiceRenameHandler(final ExplorerService explorerService,
                                 final Security security) {
        this.explorerService = explorerService;
        this.security = security;
    }

    @Override
    public SharedDocRef exec(final ExplorerServiceRenameAction action) {
        return security.secureResult(() -> SharedDocRef.create(explorerService.rename(action.getDocRef(), action.getDocName())));
    }
}