/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.chroot.steps;

import com.google.inject.Inject;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.chroot.builders.ChrootPackageBuilder;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

/**
 *
 * @author directhex
 */
public class ChrootPackageStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {
    
    @StepContextParameter
    private transient TaskListener listener;
    
    @StepContextParameter
    private transient FilePath ws;
    
    @StepContextParameter
    private transient Run<?,?> build;
    
    @StepContextParameter
    private transient Launcher launcher;
    
    @Inject
    private transient ChrootPackageStep step;
    
    @Override
    protected Void run() throws Exception {
        ChrootPackageBuilder builder = new ChrootPackageBuilder(step.getChrootName(), step.getSourcePackage());
        builder.setArchAllBehaviour(step.getArchAllBehaviour());
        builder.setIgnoreExit(step.isIgnoreExit());
        builder.setClear(step.isClear());
        builder.setNoUpdate(step.isNoUpdate());
        builder.setForceInstall(step.isForceInstall());
        builder.perform(build, ws, launcher, listener);
        return null;
    }
}
