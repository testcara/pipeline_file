def templateNameofET = 'errata-rails-aio-template'
def templateNameofMysql = 'errata-mysql-template'
//def templatePathofET = 'https://gitlab.cee.redhat.com/wlin/errata-upshift/raw/master/upshift/template/errata_services_template/errata-all-services-in-one-pod.yaml'
// def templatePathofMysql = 'https://gitlab.cee.redhat.com/wlin/errata-upshift/raw/master/upshift/template/mysql/mysql_template.yaml'
def templatePathofET = 'https://github.com/testcara/pipeline_file/raw/master/app_template.json'
def templatePathofMysql = 'https://github.com/testcara/pipeline_file/raw/master/mysql_template.json'
def ET_APP_NAME = 'et-qe-testing'
def MYSQL_APP_NAME = "$ET_APP_NAME-mysql"
def RUN_USER = '1058980001'
def etTemplateParameters = "-p=APP_NAME=$ET_APP_NAME -p=CPU_LIMITS=1 -p=ET_CONFIGMAP=et-qe-testing-settings -p=ET_SECRET=et-qe-testing-settings -p=ET_MYSQL_SECRET=et-qe-testing-mysql -p=RUN_USER=$RUN_USER"
def mysqlTemplateParameters = "-p=APP_NAME=$MYSQL_APP_NAME"
def etObjects = (String[]) ["is/$ET_APP_NAME-s2i", "is/$ET_APP_NAME-basic", "bc/$ET_APP_NAME-bc", "dc/$ET_APP_NAME-rails", "route/$ET_APP_NAME-route", "svc/$ET_APP_NAME-svc"]
def mysqlObjects = (String[]) ["is/$MYSQL_APP_NAME", "bc/$MYSQL_APP_NAME", "dc/$MYSQL_APP_NAME", "svc/$MYSQL_APP_NAME"]

def process_template_and_create_objects(String templateName,  String templateParameters, String[] objectsName) {
    def templateGeneratedSelector = openshift.selector(objectsName)
    def objectModels = openshift.process(templateName, templateParameters)
    def objects
    def verb
    def objectsGeneratedFromTemplate = templateGeneratedSelector.exists()
    if (!objectsGeneratedFromTemplate) {
        verb = "Created"
        objects = openshift.create(objectModels)
    } else {
        verb = "Found"
        objects = templateGeneratedSelector
    }
    objects.withEach {
        echo "${verb} ${it.name()} from template with labels ${it.object().metadata.labels}"
    }
    return objects
}

def check_builds(Integer time, String bcName) {
    def goodBuildStatus = (String[]) ['Running', 'Pending', 'Complete']
    def bcSelector = openshift.selector("bc", bcName)
    bcSelector.startBuild()
    def builds= bcSelector.related('builds')
    timeout(time) { 
        // Checking watch output and running watch closure again in 250ms
        builds.untilEach(1) {
            def status = it.object().status.phase
            if ( goodBuildStatus.contains(status) == false ) {
                throw new Exception("Build failed")
            } //if
            if ( status == 'Complete' )
            {
                echo "---> Build Complete ..."
                return true
            } //if
        } //each
    } //timeout
}

def check_deployments(Integer time, String dcName) {
    def goodBuildStatus = (String[]) ['Running', 'Pending', 'Complete']
    def dcSelector = openshift.selector("dc", dcName)
    dcSelector.rollout().latest()
    timeout(time) { 
        openshift.selector("dc", dcName).related('pods').untilEach(1) {
            if (it.object().status.phase == "Running" )
            {
                echo "---> Deploy Complete ..."
                return true
            }
        } //each
    } // timout
}

pipeline {
    agent any
    stages {
        stage('preamble') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject(){
                            echo "Using project: ${openshift.project()}"
                            sh '''
                            oc version
                            '''
                        } //project
                      } //cluster
                   } //script
            } //steps
        } //stage
        stage('cleanup') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject('errata-qe-test'){
                            echo "--- Delete apps --->"
                            openshift.selector("all", [ app : "$ET_APP_NAME" ]).delete()
                            openshift.selector("all", [ app : "$MYSQL_APP_NAME" ]).delete()
                            def exist1 = openshift.selector("template", "$templateNameofET").exists()
                            if (exist1) {
                                echo "--- Delete ET template --->"
                                openshift.selector("template", "$templateNameofET").delete()
                            } //if
                            
                            def exist2 = openshift.selector("template", "$templateNameofMysql").exists()
                            if (exist2) {
                                echo "--- Delete Mysql template --->"
                                openshift.selector("template", "$templateNameofMysql").delete()
                            }

                        } //project
                    } //cluster
                } //script
            } //steps
        } //stage
        stage('create templates') {
            steps {
              
                script {
                    openshift.withCluster() {
                        openshift.withProject('errata-qe-test'){
                            echo '--- Upload ET template --->'
                            openshift.create(templatePathofET)
                            echo '--- Upload Mysql template --->'
                            openshift.create(templatePathofMysql)
                        } //project
                    } //cluster
                } //script

            } //steps
        } // stage
        stage('create Mysql app') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject('errata-qe-test'){
                            echo '--- Create Mysql app from the ET Mysql template --->'
                            process_template_and_create_objects(templateNameofMysql, mysqlTemplateParameters, mysqlObjects)
                        } //project
                    } //cluster
                } //script
            } //steps
        } // stage
        stage('Build Mysql app') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject('errata-qe-test'){
                            echo '--- Build Mysql app  --->'
                            check_builds(20, "$MYSQL_APP_NAME")
                        } //project
                    } //cluster
                } //script
            } //steps
        } // stage
        stage('Deploy Mysql app') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject('errata-qe-test'){
                            echo '--- Deploy Mysql app --->'
                            check_deployments(2, "$MYSQL_APP_NAME")
                        } //project
                    } //cluster
                } //script
            } //steps
        } // stage
        stage('create ET app') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject('errata-qe-test'){
                            echo '--- Create ET apps from the ET template --->'
                            process_template_and_create_objects(templateNameofET, etTemplateParameters, etObjects)
                        } //project
                    } //cluster
                } //script
            } //steps
        } // stage
        stage('Build ET app') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject('errata-qe-test'){
                            echo '--- Build ET app  --->'
                            check_builds(20, "$ET_APP_NAME-bc")
                        } //project
                    } //cluster
                } //script
            } //steps
        } // stage
        stage('Deploy ET app') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject('errata-qe-test'){
                            echo '--- Deploy ET app --->'
                            check_deployments(2, "$ET_APP_NAME-rails")
                        } //project
                    } //cluster
                } //script
            } //steps
        } // stage
    } //stages
}
