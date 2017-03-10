package org.jenkinsci.plugins.chroot.steps;

import hudson.Extension;
import hudson.Util;
import hudson.util.ListBoxModel;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author Jo Shields
 */
public class ChrootPackageStep extends AbstractStepImpl {
    private final @CheckForNull String chrootName;
    private @CheckForNull String archAllLabel;
    private boolean ignoreExit;
    private boolean clear;
    private final @CheckForNull String sourcePackage;
    private boolean noUpdate;
    private boolean forceInstall;
    private String archAllBehaviour;
    
    @DataBoundConstructor
    public ChrootPackageStep(@CheckForNull String chrootName, @CheckForNull String sourcePackage) {
        this.chrootName = Util.fixNull(chrootName);
        this.sourcePackage = Util.fixNull(sourcePackage);
    }
    
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
    
    public @CheckForNull String getChrootName() {
        return chrootName;
    }
    
    @DataBoundSetter
    public void setArchAllBehaviour(@CheckForNull String archAllBehaviour) {
        this.archAllBehaviour = Util.fixNull(archAllBehaviour);
    }
    
    public String getArchAllBehaviour() {
        return Util.fixEmptyAndTrim(archAllBehaviour);
    }


    public @CheckForNull String getSourcePackage() {
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
    
    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() { super(ChrootPackageStepExecution.class); }

        @Override
        public String getFunctionName() {
            return "chrootpackage";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Build Debian/RPM source package in a chroot";
        }
        
        
        public ListBoxModel doFillArchAllBehaviourItems() {
            return new ListBoxModel(
                    new ListBoxModel.Option("Default", null),
                    new ListBoxModel.Option("All binaries","all_and_arch"),
                    new ListBoxModel.Option("Architecture-specific binaries","arch")
            );
        }
    }
}