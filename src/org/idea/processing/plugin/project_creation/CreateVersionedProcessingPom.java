/*
 * Copyright (c) 2017  mistodev
 *
 * This file is part of "Processing IDEA plugin" and is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.idea.processing.plugin.project_creation;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.net.HttpConfigurable;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.idea.processing.plugin.project_creation.dependency.Version;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

class CreateVersionedProcessingPom implements Callable<File> {

    private final Logger logger = com.intellij.openapi.diagnostic.Logger.getInstance(CreateVersionedProcessingPom.class);

    private final String PROCESSING_CORE_RELATIVE_LOCATION = "/org/processing/core";

    private final Collection<URL> centralRepos;
    private final File downloadDestination;
    private final Version version;

    CreateVersionedProcessingPom(@NotNull Collection<URL> centralRepos,
                                        @NotNull Version version,
                                        @NotNull File downloadDestination) {
        this.version = version;
        this.centralRepos = centralRepos;
        this.downloadDestination = downloadDestination;
    }

    @Override
    @NotNull
    public File call() throws IOException {
        String processingCorePomName = "core-" + version.toString() + ".pom";

        Model pomModel = null;

        if (centralRepos.isEmpty()) {
            logger.warn("No central repositories were specified. Creation of custom dependency POM is likely to fail.");
        }

        for (URL repoUrl : centralRepos) {
            try {
                URL artifactPomUrl = new URL(repoUrl.toExternalForm() + PROCESSING_CORE_RELATIVE_LOCATION + "/" + version + "/" + processingCorePomName);

                logger.info("Attempting to download artifact at URL: " + artifactPomUrl.toString());

                HttpURLConnection pomConn = HttpConfigurable.getInstance().openHttpConnection(artifactPomUrl.toExternalForm());

                try {
                    pomModel = createModelFromRemotePom(pomConn);
                } catch (XmlPullParserException xml) {
                    logger.error(xml);
                    throw new IllegalStateException("Reading POM resource successful, but construction of in-memory dependency model from it failed.", xml);
                }

                if (pomModel != null) {
                    break;
                }
            } catch (MalformedURLException mue) {
                logger.error(mue);
                throw new IllegalStateException("Failed to create a valid URL to the remote Maven repository.", mue);
            }
        }

        Model projectDependenciesPomModel = createProjectDependencyPomModel(pomModel);

        if (! downloadDestination.exists()) {
            if (! downloadDestination.mkdirs()) {
                throw new IOException("Unable to create dependency directory at '" + downloadDestination + "'.");
            }
        }

        File projectTemplatePom = new File(downloadDestination, "pom.xml");

        MavenXpp3Writer mavenPomWriter = new MavenXpp3Writer();
        mavenPomWriter.write(new FileWriter(projectTemplatePom), projectDependenciesPomModel);

        return projectTemplatePom;
    }

    private Model createModelFromRemotePom(@NotNull HttpURLConnection pomResourceConn) throws IOException, XmlPullParserException {
        Reader bufferedPomReader = new BufferedReader(new InputStreamReader(pomResourceConn.getInputStream()));

        MavenXpp3Reader pomUnmarshaller = new MavenXpp3Reader();

        return pomUnmarshaller.read(bufferedPomReader);
    }

    private Model createProjectDependencyPomModel(Model processingPomModel) {
        Model templateModel = new Model();
        templateModel.setModelVersion(processingPomModel.getModelVersion());
        templateModel.setGroupId("org.processing.idea");
        templateModel.setArtifactId("core");
        templateModel.setVersion("1.0.0");

        List<Dependency> artifactDependencies = new ArrayList<>(6);

        String joglVersion = null;
        for (Dependency dependency : processingPomModel.getDependencies()) {
            if (dependency.getGroupId().equals("org.jogamp.jogl") && dependency.getArtifactId().equals("jogl")) {
                joglVersion = dependency.getVersion();
            }
        }

        if (joglVersion == null) {
            throw new IllegalStateException("Unable to determine JOGL version used for this build of Processing.");
        }

        Dependency core = new Dependency();
        core.setGroupId("org.processing");
        core.setArtifactId("core");
        core.setVersion(version.toString());
        artifactDependencies.add(core);

        Dependency pdfExport = new Dependency();
        pdfExport.setGroupId(VanillaLibrary.PDF_EXPORT.getGroupId());
        pdfExport.setArtifactId(VanillaLibrary.PDF_EXPORT.getArtifactId());
        pdfExport.setVersion(version.toString());
        artifactDependencies.add(pdfExport);

        Dependency serial = new Dependency();
        serial.setGroupId(VanillaLibrary.SERIAL.getGroupId());
        serial.setArtifactId(VanillaLibrary.SERIAL.getArtifactId());
        serial.setVersion(version.toString());
        artifactDependencies.add(serial);

        Dependency network = new Dependency();
        network.setGroupId(VanillaLibrary.NETWORK.getGroupId());
        network.setArtifactId(VanillaLibrary.NETWORK.getArtifactId());
        network.setVersion(version.toString());
        artifactDependencies.add(network);

        Dependency gluegenRt = new Dependency();
        gluegenRt.setGroupId("org.jogamp.gluegen");
        gluegenRt.setArtifactId("gluegen-rt-main");
        gluegenRt.setVersion(joglVersion);
        artifactDependencies.add(gluegenRt);

        Dependency joglAllMain = new Dependency();
        joglAllMain.setGroupId("org.jogamp.jogl");
        joglAllMain.setArtifactId("jogl-all-main");
        joglAllMain.setVersion(joglVersion);
        artifactDependencies.add(joglAllMain);

        if (SystemInfo.isMac) {
            Dependency appleExtensions = new Dependency();
            appleExtensions.setGroupId("com.apple");
            appleExtensions.setArtifactId("AppleJavaExtensions");
            appleExtensions.setVersion("LATEST");
            artifactDependencies.add(appleExtensions);
        }

        templateModel.setDependencies(artifactDependencies);

        return templateModel;
    }
}
