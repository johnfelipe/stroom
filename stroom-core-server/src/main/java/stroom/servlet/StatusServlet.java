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

package stroom.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.node.api.NodeInfo;
import stroom.node.shared.FindVolumeCriteria;
import stroom.node.shared.VolumeEntity;
import stroom.node.shared.VolumeEntity.VolumeType;
import stroom.node.shared.VolumeEntity.VolumeUseStatus;
import stroom.node.shared.VolumeState;
import stroom.security.Security;
import stroom.util.BuildInfoProvider;
import stroom.util.shared.BuildInfo;
import stroom.volume.VolumeService;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * <p>
 * SERVLET that reports status of Stroom for scripting purposes.
 * </p>
 */
public class StatusServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusServlet.class);

    private static final long serialVersionUID = 1L;
    private static final String INFO = "INFO";
    private static final String WARN = "WARN";
    private static final String ERROR = "ERROR";
    private static final String DELIMITER = ",";
    private static final String AREA_HTTP = "HTTP";
    private static final String AREA_BUILD = "BUILD";
    private static final String AREA_DB = "DB";
    private static final String AREA_VOLUME = "VOLUME";
    private static final String MSG_OK = "OK";

    private final BuildInfoProvider buildInfoProvider;
    private final NodeInfo nodeInfo;
    private final VolumeService volumeService;
    private final Security security;

    @Inject
    StatusServlet(final BuildInfoProvider buildInfoProvider,
                  final NodeInfo nodeInfo,
                  final VolumeService volumeService,
                  final Security security) {
        this.buildInfoProvider = buildInfoProvider;
        this.nodeInfo = nodeInfo;
        this.volumeService = volumeService;
        this.security = security;
    }

    @Override
    public void init() throws ServletException {
        LOGGER.info("Initialising Status Servlet");
        super.init();
        LOGGER.info("Initialised Status Servlet");
    }

    @Override
    public void destroy() {
        LOGGER.info("Destroying Status Servlet");
        super.destroy();
        LOGGER.info("Destroyed Status Servlet");
    }

    /**
     * Method interceptor needs to go on public API By-pass authentication / authorisation checks.
     * <p>
     * This servlet is NOT protected by default and should be filtered by Apache access controls, see documentation for
     * details.
     */
    @Override
    public void service(final ServletRequest arg0, final ServletResponse arg1) {
        security.insecure(() -> {
            try {
                super.service(arg0, arg1);
            } catch (ServletException e) {
                throw new RuntimeException(e.getMessage(), e);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {
        security.asProcessingUser(() -> {
            try {
                response.setContentType("text/plain");

                final PrintWriter pw = response.getWriter();

                reportHTTP(pw);
                reportNodeStatus(pw);
                reportVolumeStatus(pw);

                pw.close();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * Sub reporting method.
     */
    private void reportHTTP(final PrintWriter pw) {
        writeInfoLine(pw, AREA_HTTP, MSG_OK);
    }

    /**
     * Sub reporting method.
     */
    private void reportNodeStatus(final PrintWriter pw) {
        try {
            final BuildInfo buildInfo = buildInfoProvider.get();
            writeInfoLine(pw, AREA_BUILD, "Build version " + buildInfo.getBuildVersion());
            writeInfoLine(pw, AREA_BUILD, "Build date " + buildInfo.getBuildDate());
            writeInfoLine(pw, AREA_BUILD, "Up date " + buildInfo.getUpDate());
            writeInfoLine(pw, AREA_BUILD, "Node name " + nodeInfo.getThisNodeName());
            writeInfoLine(pw, AREA_DB, MSG_OK);
        } catch (final RuntimeException e) {
            writeErrorLine(pw, AREA_DB, e.getMessage());
        }
    }

    /**
     * Sub reporting method.
     */
    private void reportVolumeStatus(final PrintWriter pw) {
        try {
            final FindVolumeCriteria criteria = new FindVolumeCriteria();
            boolean oneOKVolume = false;
            final List<VolumeEntity> volumeList = volumeService.find(criteria);
            for (final VolumeEntity volume : volumeList) {
                final VolumeState state = volume.getVolumeState();
                if (state.getPercentUsed() == null) {
                    writeErrorLine(pw, AREA_VOLUME,
                            "Unknown Status for volume " + volume.getPath() + " on node " + volume.getNode().getName());
                } else {
                    if (volume.isFull()) {
                        writeWarnLine(pw, AREA_VOLUME,
                                "Volume " + volume.getPath() + " full on node " + volume.getNode().getName());
                    } else {
                        writeInfoLine(pw, AREA_VOLUME, "Volume " + volume.getPath() + " " + state.getPercentUsed()
                                + "% full on node " + volume.getNode().getName());
                        if (VolumeUseStatus.ACTIVE.equals(volume.getStreamStatus())
                                && VolumeType.PUBLIC.equals(volume.getVolumeType())) {
                            oneOKVolume = true;
                        }
                    }
                }
            }
            if (volumeList.size() == 0) {
                writeErrorLine(pw, AREA_VOLUME, "No volumes listed");
            }
            if (!oneOKVolume) {
                writeErrorLine(pw, AREA_VOLUME, "No OK public volumes listed");
            }

        } catch (final RuntimeException e) {
            writeErrorLine(pw, AREA_VOLUME, e.getMessage());
        }
    }

    /**
     * Write a info line.
     */
    private void writeInfoLine(final PrintWriter pw, final String area, final String msg) {
        pw.print(INFO);
        pw.print(DELIMITER);
        pw.print(area);
        pw.print(DELIMITER);
        pw.print(msg);
        pw.println();
    }

    /**
     * Write a info line.
     */
    private void writeWarnLine(final PrintWriter pw, final String area, final String msg) {
        pw.print(WARN);
        pw.print(DELIMITER);
        pw.print(area);
        pw.print(DELIMITER);
        pw.print(msg);
        pw.println();
    }

    /**
     * Write a error line.
     */
    private void writeErrorLine(final PrintWriter pw, final String area, final String msg) {
        pw.print(ERROR);
        pw.print(DELIMITER);
        pw.print(area);
        pw.print(DELIMITER);
        pw.print(msg);
        pw.println();
    }
}
