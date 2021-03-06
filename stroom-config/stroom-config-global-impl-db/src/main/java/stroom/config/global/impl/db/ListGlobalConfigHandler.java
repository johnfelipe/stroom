package stroom.config.global.impl.db;

import stroom.config.global.api.ConfigProperty;
import stroom.config.global.api.ListGlobalConfigAction;
import stroom.util.shared.BaseResultList;
import stroom.util.shared.ResultList;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;


class ListGlobalConfigHandler extends AbstractTaskHandler<ListGlobalConfigAction, ResultList<ConfigProperty>> {
    private final GlobalConfigService globalConfigService;

    @Inject
    ListGlobalConfigHandler(final GlobalConfigService globalConfigService) {
        this.globalConfigService = globalConfigService;
    }

    @Override
    public ResultList<ConfigProperty> exec(final ListGlobalConfigAction task) {
        List<ConfigProperty> list = globalConfigService.list();

        if (task.getCriteria().getName() != null) {
            list = list.stream()
                    .filter(v -> task.getCriteria().getName().isMatch(v.getName()))
                    .peek(v -> {
                        if (v.isPassword()) {
                            v.setValue("********************");
                        }
                    })
                    .collect(Collectors.toList());
        }

        return BaseResultList.createPageLimitedList(list, task.getCriteria().obtainPageRequest());
    }
}
