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

package stroom.receive.rules.client.presenter;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;
import stroom.data.retention.shared.TimeUnit;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.ExpressionOperator.Op;
import stroom.receive.rules.client.presenter.DataRetentionRulePresenter.DataRetentionRuleView;
import stroom.data.retention.shared.DataRetentionRule;
import stroom.meta.shared.MetaFieldNames;
import stroom.datasource.shared.FetchFieldsAction;

public class DataRetentionRulePresenter extends MyPresenterWidget<DataRetentionRuleView> {
    private final EditExpressionPresenter editExpressionPresenter;
    private DataRetentionRule originalRule;

    @Inject
    public DataRetentionRulePresenter(final EventBus eventBus,
                                      final DataRetentionRuleView view,
                                      final EditExpressionPresenter editExpressionPresenter,
                                      final ClientDispatchAsync dispatcher) {
        super(eventBus, view);
        this.editExpressionPresenter = editExpressionPresenter;
        view.setExpressionView(editExpressionPresenter.getView());

        dispatcher.exec(new FetchFieldsAction()).onSuccess(result -> editExpressionPresenter.init(dispatcher, MetaFieldNames.STREAM_STORE_DOC_REF, result.getFields()));
    }

    void read(final DataRetentionRule rule) {
        this.originalRule = rule;
        getView().setName(rule.getName());
        if (rule.getExpression() == null) {
            editExpressionPresenter.read(new ExpressionOperator.Builder(Op.AND).build());
        } else {
            editExpressionPresenter.read(rule.getExpression());
        }
        getView().setForever(rule.isForever());
        getView().setAge(rule.getAge());
        getView().setTimeUnit(rule.getTimeUnit());
    }

    DataRetentionRule write() {
        final ExpressionOperator expression = editExpressionPresenter.write();
        return new DataRetentionRule(originalRule.getRuleNumber(), originalRule.getCreationTime(), getView().getName(), originalRule.isEnabled(), expression, getView().getAge(), getView().getTimeUnit(), getView().isForever());
    }

    public interface DataRetentionRuleView extends View {
        void setExpressionView(View view);

        String getName();

        void setName(String name);

        boolean isForever();

        void setForever(boolean forever);

        int getAge();

        void setAge(int age);

        TimeUnit getTimeUnit();

        void setTimeUnit(TimeUnit timeUnit);
    }
}
