def call(){
    echo "--- Delete apps --->"
    def etAppNames = ["cucumber-et-1", "cucumber-et-2", "cucumber-et-3"]
    def mysqlAppNames = ["cucumber-et-1-mysql", "cucumber-et-2-mysql", "cucumber-et-3-mysql"]
    etAppNames.each { app ->
        openshift.selector("all", [ app : "$app" ]).delete()
    }
    mysqlAppNames.each { app ->
        openshift.selector("all", [ app : "$app" ]).delete()
    }
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
}
