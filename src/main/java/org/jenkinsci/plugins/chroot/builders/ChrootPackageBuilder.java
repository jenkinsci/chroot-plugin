/*
 *  Copyright 2013, Roman Mohr <roman@fenkhuber.at>
 *  Copyright 2016, Xamarin, Inc
 *
 *  This file is part of Chroot-plugin.
 *
 *  Chroot-plugin is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Chroot-plugin is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Chroot-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.builders;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.servlet.ServletException;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.chroot.tools.ChrootToolset;
import org.jenkinsci.plugins.chroot.util.ChrootUtil;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 *
 * @author roman
 */
public class ChrootPackageBuilder extends Builder implements Serializable, SimpleBuildStep {

    private final String chrootName;
    private String archAllLabel;
    private boolean ignoreExit;
    private boolean clear;
    private final String sourcePackage;
    private boolean noUpdate;
    private boolean forceInstall;

    @DataBoundSetter
    public void setForceInstall(boolean forceInstall) {
        this.forceInstall = forceInstall;
    }
    
    public boolean isForceInstall() {
        return forceInstall;
    }

    @DataBoundSetter
    public void setNoUpdate(boolean noUpdate) {
        this.noUpdate = noUpdate;
    }
    
    public boolean isNoUpdate() {
        return noUpdate;
    }
    
    @DataBoundConstructor
    public ChrootPackageBuilder(@CheckForNull String chrootName, @CheckForNull String sourcePackage) throws IOException {
        this.chrootName = Util.fixNull(chrootName);
        this.sourcePackage = Util.fixNull(sourcePackage);
    }

    public String getChrootName() {
        return chrootName;
    }
    
    @DataBoundSetter
    public void setArchAllLabel(@CheckForNull String archAllLabel) {
        this.archAllLabel = Util.fixNull(archAllLabel);
    }
    
    public String getArchAllLabel() {
        return archAllLabel;
    }

    public String getSourcePackage() {
        return sourcePackage;
    }

    @DataBoundSetter
    public void setIgnoreExit(boolean ignoreExit) {
        this.ignoreExit = ignoreExit;
    }
    
    public boolean isIgnoreExit() {
        return ignoreExit;
    }

    @DataBoundSetter
    public void setClear(boolean clear) {
        this.clear = clear;
    }
    
    public boolean isClear() {
        return clear;
    }

    private static final class LocalCopyTo implements FileCallable<Void> {

        private final String target;

        public LocalCopyTo(String target) {
            this.target = target;
        }

        @Override
        public Void invoke(File source, VirtualChannel channel) throws IOException, InterruptedException {
            FilePath _source = new FilePath(source);
            FilePath _target = new FilePath(new File(target));
            _source.copyTo(_target);
            return null;
        }

        @Override
        public void checkRoles(RoleChecker rc) throws SecurityException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        EnvVars env = build.getEnvironment(listener);
        ChrootToolset installation = ChrootToolset.getInstallationByName(env.expand(this.chrootName));
        installation = installation.forNode(workspace.toComputer().getNode(), listener);
        installation = installation.forEnvironment(env);
        if (installation.getHome() == null) {
            listener.fatalError("Installation of chroot environment failed");
            listener.fatalError("Please check if pbuilder is installed on the selected node and that"
                    + " the user, Jenkins uses, cann run pbuilder with sudo.");
            throw new IOException("Installation of chroot environment failed");
        }
        FilePath tarBall = new FilePath(workspace.toComputer().getNode().getChannel(), installation.getHome());

        FilePath workerTarBall = workspace.child(env.expand(this.chrootName)).child(tarBall.getName());
        workerTarBall.getParent().mkdirs();

        // force environment recreation when clear is selected
        if (isClear()) {
            boolean ret = installation.getChrootWorker().cleanUp(build, launcher, listener, workerTarBall);
            if (ret == false) {
                listener.fatalError("Chroot environment cleanup failed");
                if(ignoreExit)
                    return;
                else
                    throw new IOException("Chroot environment cleanup failed");
            }
        }

        if (!workerTarBall.exists() || !ChrootUtil.isFileIntact(workerTarBall) || tarBall.lastModified() > workerTarBall.lastModified()) {
            tarBall.act(new LocalCopyTo(workerTarBall.getRemote()));
            ChrootUtil.getDigestFile(tarBall).act(new LocalCopyTo(ChrootUtil.getDigestFile(workerTarBall).getRemote()));
        }

        if (!this.isNoUpdate()) {
            boolean ret = installation.getChrootWorker().updateRepositories(build, launcher, listener, workerTarBall);
            if (ret == false) {
                listener.fatalError("Updating repository indices in chroot environment failed.");
                if(ignoreExit)
                    return;
                else
                    throw new IOException("Updating repository indices in chroot environment failed.");
            }
        }
        ChrootUtil.saveDigest(workerTarBall);
        if (!installation.getChrootWorker().perform(build, workspace, launcher, listener, workerTarBall, this.archAllLabel, this.sourcePackage) && !ignoreExit)
            throw new IOException("Package build failed");
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public AutoCompletionCandidates doAutoCompleteChrootName(@QueryParameter String value) {
            AutoCompletionCandidates c = new AutoCompletionCandidates();
            for (ChrootToolset set : ChrootToolset.list()) {
                if(set.getName().startsWith(value))
                    c.add(set.getName());
            }
            return c;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Chroot Package Builder";
        }

        public FormValidation doCheckPackagesFile(@QueryParameter String value)
                throws IOException, ServletException, InterruptedException {
            List<String> validationList = new LinkedList<String>();
            Boolean warn = false;
            Boolean error = false;
            for (String file : ChrootUtil.splitFiles(value)) {
                FilePath x = new FilePath(new File(file));
                if (!x.exists()) {
                    warn = true;
                    validationList.add(String.format("File %s does not yet exist.", file));
                } else if (x.isDirectory()) {
                    error = true;
                    validationList.add(String.format("%s is a directory. Enter a file.", file));
                }
            }
            if (error == true) {
                return FormValidation.error(StringUtils.join(validationList.listIterator(), "\n"));
            } else if (warn == true) {
                return FormValidation.warning(StringUtils.join(validationList.listIterator(), "\n"));
            }
            return FormValidation.ok();
        }
    }
}
