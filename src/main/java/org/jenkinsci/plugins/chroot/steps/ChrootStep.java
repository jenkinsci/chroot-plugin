package org.jenkinsci.plugins.chroot.steps;

import hudson.Extension;
import hudson.Util;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.chroot.util.ChrootUtil;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author Jo Shields
 */
public class ChrootStep extends AbstractStepImpl {
    private final @CheckForNull String chrootName;
    private List<String> additionalPackages;
    private @CheckForNull String packagesFile;
    private boolean ignoreExit;
    private boolean clear;
    private final @CheckForNull String command;
    private boolean loginAsRoot;
    private boolean noUpdate;
    private boolean forceInstall;
    
    @DataBoundConstructor
    public ChrootStep(@CheckForNull String chrootName, @CheckForNull String command) {
        this.chrootName = Util.fixNull(chrootName);
        this.command = Util.fixNull(command);
    }
    
    @DataBoundSetter
    public void setForceInstall(boolean forceInstall) {
        this.forceInstall = forceInstall;
    }
    
    public boolean isForceInstall() {
        return forceInstall;
    }
    
    @DataBoundSetter
    public void setPackagesFile(@CheckForNull String packagesFile) {
        this.packagesFile = Util.fixNull(packagesFile);
    }
    
    public @CheckForNull String getPackagesFile() {
        return Util.fixEmptyAndTrim(packagesFile);
    }
    
    @DataBoundSetter
    public void setLoginAsRoot(boolean loginAsRoot) {
        this.loginAsRoot = loginAsRoot;
    }
    
    public boolean isLoginAsRoot() {
        return loginAsRoot;
    }

    @DataBoundSetter
    public void setNoUpdate(boolean noUpdate) {
        this.noUpdate = noUpdate;
    }
    
    public boolean isNoUpdate() {
        return noUpdate;
    }
    
    public @CheckForNull String getChrootName() {
        return chrootName;
    }
    
    @DataBoundSetter
    public void setAdditionalPackages(@CheckForNull String additionalPackages) {
        this.additionalPackages = ChrootUtil.splitPackages(Util.fixNull(additionalPackages));
    }
    
    public @CheckForNull String getAdditionalPackages() {
        return Util.fixEmptyAndTrim(StringUtils.join(additionalPackages, " "));
    }

    public @CheckForNull String getCommand() {
        return command;
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
    
    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() { super(ChrootStepExecution.class); }

        @Override
        public String getFunctionName() {
            return "chroot";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Run script in a chroot";
        }
    }
}
