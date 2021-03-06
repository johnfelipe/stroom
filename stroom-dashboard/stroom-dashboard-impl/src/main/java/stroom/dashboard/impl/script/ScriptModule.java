/*
 * Copyright 2018 Crown Copyright
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

package stroom.dashboard.impl.script;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import stroom.docstore.api.DocumentActionHandlerBinder;
import stroom.explorer.api.ExplorerActionHandler;
import stroom.importexport.api.ImportExportActionHandler;
import stroom.script.shared.FetchScriptAction;
import stroom.script.shared.ScriptDoc;
import stroom.task.api.TaskHandlerBinder;
import stroom.util.guice.ResourcePaths;
import stroom.util.guice.ServletBinder;

public class ScriptModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ScriptStore.class).to(ScriptStoreImpl.class);

        ServletBinder.create(binder())
                .bind(ResourcePaths.ROOT_PATH + "/script", ScriptServlet.class);

        TaskHandlerBinder.create(binder())
                .bind(FetchScriptAction.class, FetchScriptHandler.class);

        final Multibinder<ExplorerActionHandler> explorerActionHandlerBinder = Multibinder.newSetBinder(binder(), ExplorerActionHandler.class);
        explorerActionHandlerBinder.addBinding().to(ScriptStoreImpl.class);

        final Multibinder<ImportExportActionHandler> importExportActionHandlerBinder = Multibinder.newSetBinder(binder(), ImportExportActionHandler.class);
        importExportActionHandlerBinder.addBinding().to(ScriptStoreImpl.class);

        DocumentActionHandlerBinder.create(binder())
                .bind(ScriptDoc.DOCUMENT_TYPE, ScriptStoreImpl.class);

//        final Multibinder<FindService> findServiceBinder = Multibinder.newSetBinder(binder(), FindService.class);
//        findServiceBinder.addBinding().to(stroom.script.ScriptStoreImpl.class);
    }
}