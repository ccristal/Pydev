/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations.app_engine.wizards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.plugin.PyStructureConfigHelpers;
import org.python.pydev.ui.wizards.project.IWizardNewProjectNameAndLocationPage;
import org.python.pydev.ui.wizards.project.NewProjectNameAndLocationWizardPage;
import org.python.pydev.ui.wizards.project.PythonProjectWizard;

/**
 * Wizard that helps in the creation of a Pydev project configured for Google App Engine. 
 * 
 * @author Fabio
 */
public class AppEngineWizard extends PythonProjectWizard{

    private AppEngineConfigWizardPage appEngineConfigWizardPage;
    private AppEngineTemplatePage appEngineTemplatePage;

    /**
     * Add wizard pages to the instance
     * 
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    public void addPages(){
        addPage(projectPage);

        appEngineConfigWizardPage = new AppEngineConfigWizardPage("Goole App Engine Page");
        appEngineConfigWizardPage.setTitle("Google App Engine");
        appEngineConfigWizardPage.setDescription("Set Google App Engine Configuration");
        addPage(appEngineConfigWizardPage);
        
        appEngineTemplatePage = new AppEngineTemplatePage("Initial Structure");
        addPage(appEngineTemplatePage);
    }
    
    
    /**
     * Creates the project page.
     */
    protected IWizardNewProjectNameAndLocationPage createProjectPage(){
        return new NewProjectNameAndLocationWizardPage("Setting project properties"){
            
            @Override
            public void createControl(Composite parent){
                super.createControl(parent);
                checkSrcFolder.setVisible(false);
            }
            
            @Override
            public boolean shouldCreatSourceFolder(){
                return true; //Always start a google app engine with a source folder (we need it for the templates)
            }
        };
    }

    /**
     * Overridden to add the external source folders from google app engine. 
     */
    @Override
    protected void createAndConfigProject(final IProject newProjectHandle, final IProjectDescription description,
            final String projectType, final String projectInterpreter, IProgressMonitor monitor, Object ... additionalArgsToConfigProject) throws CoreException{
        ICallback<List<IContainer>, IProject> getSourceFolderHandlesCallback = new ICallback<List<IContainer>, IProject>(){

            public List<IContainer> call(IProject projectHandle){
                if(projectPage.shouldCreatSourceFolder()){
                    IContainer folder = projectHandle.getFolder("src");
                    List<IContainer> ret = new ArrayList<IContainer>();
                    ret.add(folder);
                    return ret;
                }else{
                    //if the user hasn't selected to create a source folder, use the project itself for that.
                    List<IContainer> ret = new ArrayList<IContainer>();
                    ret.add(projectHandle);
                    return ret;
                }
            }
        };

        ICallback<List<String>, IProject> getExternalSourceFolderHandlesCallback = new ICallback<List<String>, IProject>(){
        
            public List<String> call(IProject projectHandle){
                return appEngineConfigWizardPage.getExternalSourceFolders();
            }
        };
        
        ICallback<Map<String, String>, IProject> getVariableSubstitutionCallback = new ICallback<Map<String, String>, IProject>(){
            
            public Map<String, String> call(IProject projectHandle){
                return appEngineConfigWizardPage.getVariableSubstitution(); 
            }
        };
        
        PyStructureConfigHelpers.createPydevProject(description, newProjectHandle, monitor, projectType,
                projectInterpreter, getSourceFolderHandlesCallback, getExternalSourceFolderHandlesCallback,
                getVariableSubstitutionCallback);
        
        //Ok, after the default is created, let's see if we have a template...
        IFolder sourceFolder = newProjectHandle.getFolder("src");
        appEngineTemplatePage.fillSourceFolder(sourceFolder);
    }

}
