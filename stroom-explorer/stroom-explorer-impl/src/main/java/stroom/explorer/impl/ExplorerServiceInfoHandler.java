package stroom.explorer.impl;

import stroom.explorer.api.ExplorerService;
import stroom.explorer.shared.ExplorerServiceInfoAction;
import stroom.explorer.shared.SharedDocRefInfo;
import stroom.docref.DocRefInfo;
import stroom.security.api.Security;
import stroom.task.api.AbstractTaskHandler;

import javax.inject.Inject;


class ExplorerServiceInfoHandler extends AbstractTaskHandler<ExplorerServiceInfoAction, SharedDocRefInfo> {

    private final ExplorerService explorerService;
    private final Security security;

    @Inject
    ExplorerServiceInfoHandler(final ExplorerService explorerService,
                               final Security security) {
        this.explorerService = explorerService;
        this.security = security;
    }

    @Override
    public SharedDocRefInfo exec(final ExplorerServiceInfoAction task) {
        return security.secureResult(() -> {
            final DocRefInfo docRefInfo = explorerService.info(task.getDocRef());

            return new SharedDocRefInfo.Builder()
                    .type(docRefInfo.getDocRef().getType())
                    .uuid(docRefInfo.getDocRef().getUuid())
                    .name(docRefInfo.getDocRef().getName())
                    .otherInfo(docRefInfo.getOtherInfo())
                    .createTime(docRefInfo.getCreateTime())
                    .createUser(docRefInfo.getCreateUser())
                    .updateTime(docRefInfo.getUpdateTime())
                    .updateUser(docRefInfo.getUpdateUser())
                    .build();
        });
    }
}
