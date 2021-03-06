package stroom.index.rest;

import stroom.index.service.IndexVolumeGroupService;
import stroom.index.shared.IndexVolumeGroup;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.List;

public class IndexVolumeGroupResourceImpl implements IndexVolumeGroupResource {

    private final IndexVolumeGroupService indexVolumeGroupService;

    @Inject
    public IndexVolumeGroupResourceImpl(final IndexVolumeGroupService indexVolumeGroupService) {
        this.indexVolumeGroupService = indexVolumeGroupService;
    }

    @Override
    public Response getNames() {
        final List<String> names = indexVolumeGroupService.getNames();

        return Response.ok(names).build();
    }

    @Override
    public Response getAll() {
        final List<IndexVolumeGroup> groups = indexVolumeGroupService.getAll();

        return Response.ok(groups).build();
    }

    @Override
    public Response get(final String name) {
        final IndexVolumeGroup group = indexVolumeGroupService.get(name);

        if (null != group) {
            return Response.ok(group).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @Override
    public Response create(final String name) {
        final IndexVolumeGroup group = indexVolumeGroupService.get(name);
        return Response.ok(group).build();
    }

    @Override
    public Response delete(final String name) {
        indexVolumeGroupService.delete(name);
        return Response.noContent().build();
    }
}
