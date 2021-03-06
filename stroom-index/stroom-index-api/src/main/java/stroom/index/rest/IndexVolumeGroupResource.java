package stroom.index.rest;

import io.swagger.annotations.Api;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(value = "stroom-index volumeGroup - /v1")
@Path("/stroom-index/volumeGroup/v1")
@Produces(MediaType.APPLICATION_JSON)
public interface IndexVolumeGroupResource {

    @GET
    @Path("/names")
    Response getNames();

    @GET
    Response getAll();

    @GET
    @Path("/{name}")
    Response get(@PathParam("name") String name);

    @POST
    @Path("/{name}")
    Response create(@PathParam("name") String name);

    @DELETE
    @Path("/{name}")
    Response delete(@PathParam("name") String name);
}
