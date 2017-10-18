package grails.plugin.cookiesession.mock

import grails.core.ArtefactHandler
import grails.core.ArtefactInfo
import grails.core.GrailsClass
import org.grails.config.PropertySourcesConfig
import org.grails.core.AbstractGrailsApplication
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.core.io.Resource

class MockGrailsApplication extends AbstractGrailsApplication {
    MockGrailsApplication() {
        super()
        setConfig(new PropertySourcesConfig())
    }

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext)
        super.setMainContext(applicationContext)
    }

    @SuppressWarnings("rawtypes")
    @Override
    Class[] getAllClasses() {
        return new Class[0]
    }

    @SuppressWarnings("rawtypes")
    @Override
    Class[] getAllArtefacts() {
        return new Class[0]
    }

    // removed in 3.3 @Override
    void refreshConstraints() {

    }

    @Override
    void refresh() {

    }

    @Override
    void rebuild() {

    }

    @SuppressWarnings("rawtypes")
    @Override
    Resource getResourceForClass(Class theClazz) {
        return null
    }

    @SuppressWarnings("rawtypes")
    @Override
    boolean isArtefact(Class theClazz) {
        return false
    }

    @SuppressWarnings("rawtypes")
    @Override
    boolean isArtefactOfType(String artefactType, Class theClazz) {
        return false
    }

    @Override
    boolean isArtefactOfType(String artefactType, String className) {
        return false
    }

    @Override
    GrailsClass getArtefact(String artefactType, String name) {
        return null
    }

    @SuppressWarnings("rawtypes")
    @Override
    ArtefactHandler getArtefactType(Class theClass) {
        return null
    }

    @Override
    ArtefactInfo getArtefactInfo(String artefactType) {
        return null
    }

    @Override
    GrailsClass[] getArtefacts(String artefactType) {
        return new GrailsClass[0]
    }

    @Override
    GrailsClass getArtefactForFeature(String artefactType, Object featureID) {
        return null
    }

    @SuppressWarnings("rawtypes")
    @Override
    GrailsClass addArtefact(String artefactType, Class artefactClass) {
        return null
    }

    @Override
    GrailsClass addArtefact(String artefactType, GrailsClass artefactGrailsClass) {
        return null
    }

    @Override
    void registerArtefactHandler(ArtefactHandler handler) {

    }

    @Override
    boolean hasArtefactHandler(String type) {
        return false
    }

    @Override
    ArtefactHandler[] getArtefactHandlers() {
        return new ArtefactHandler[0]
    }

    @Override
    void initialise() {

    }

    @Override
    boolean isInitialised() {
        return true
    }

    @Override
    GrailsClass getArtefactByLogicalPropertyName(String type, String logicalName) {
        return null
    }

    @SuppressWarnings("rawtypes")
    @Override
    void addArtefact(Class artefact) {

    }

    @SuppressWarnings("rawtypes")
    @Override
    void addOverridableArtefact(Class artefact) {

    }

    @Override
    ArtefactHandler getArtefactHandler(String type) {
        return null
    }

    // new in 3.3. @Override
    MappingContext getMappingContext() {
        return null
    }

    // new in 3.3. @Override
    void setMappingContext(MappingContext mappingContext) {

    }
}
