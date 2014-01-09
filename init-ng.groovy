/*
 * An admittedly somewhat kinda sorta dirty script that assumes that you have added a git submodule
 * or copy of the ng-boilerplate project (https://github.com/ngbp/ng-boilerplate) to your 
 * Grails app at the base directory and initialized it. It will then create symbolic links 
 * where necessary for the js resources and index.html file. The application context and 
 * UrlMappings.groovy files will also be updated. After this script is run, you can start 
 * your grails app and see your SPA Grails app home page, which is now utilizing the angular app. 
 */
println "Initializing ng-boilerplate integration with Grails..."
File expectedDir = new File("Gruntfile.js")
if(!expectedDir.exists()){
	println "Error: Expected to see Gruntfile.js. Please ensure you are in the ng-boilerplate base directory."
	System.exit(0)
}

expectedDir = new File("build")
if(!expectedDir.exists()){
	println "Error: Please compile your angular project with grunt prior to running this script."
	System.exit(0)
}

expectedDir = new File("../grails-app")
if(!expectedDir.exists()){
	println "Error: Are you sure your ng-boilerplate resides at the base dir of your Grails project? Is this even a Grails project at all!?"
	System.exit(0)
}

println "creating assets symlink..."
'rm ../web-app/assets'.execute().waitForProcessOutput(System.out, System.err)
'ln -s ../ng-app/build/assets ../web-app/assets'.execute().waitForProcessOutput(System.out, System.err)
println "creating src symlink..."
'rm ../web-app/src'.execute().waitForProcessOutput(System.out, System.err)
'ln -s ../ng-app/build/src ../web-app/src'.execute().waitForProcessOutput(System.out, System.err)
println "creating vendor symlink..."
'rm ../web-app/vendor'.execute().waitForProcessOutput(System.out, System.err)
'ln -s ../ng-app/build/vendor ../web-app/vendor'.execute().waitForProcessOutput(System.out, System.err)
println "creating templates-* symlinks..."
'rm ../web-app/templates-app.js'.execute().waitForProcessOutput(System.out, System.err)
'ln -s ../ng-app/build/templates-app.js ../web-app/templates-app.js'.execute().waitForProcessOutput(System.out, System.err)
'rm ../web-app/templates-common.js'.execute().waitForProcessOutput(System.out, System.err)
'ln -s ../ng-app/build/templates-common.js ../web-app/templates-common.js'.execute().waitForProcessOutput(System.out, System.err)
println "replacing index.gsp with a symlink to ng's index.html..."
'rm ../grails-app/views/index.gsp'.execute().waitFor()
'ln -s ../../ng-app/build/index.html ../grails-app/views/index.gsp'.execute().waitForProcessOutput(System.out, System.err)
println "symbolic links created."

println "Setting the application context..."
HashMap applicationProperties = [:]
File file = new File("../application.properties")
file.eachLine {
     int equalsSign = it.indexOf("=")
     if(equalsSign > 0){
          String key = it.substring(0, equalsSign)
          String value = it.substring(equalsSign+1)
          applicationProperties.put(key, value)
     } else {
          //A comment line
          applicationProperties.put(it, "")
     }
}

if(applicationProperties.containsKey("app.context")){
     println "app.context is being overridden..."
     applicationProperties["app.context"] = "/"
} else {
     applicationProperties.put("app.context", "/")
}
println "writing application properties..."
file.write(applicationProperties.collect{key, value -> !value ? "$key" :"$key=$value"}.join("\n"))

println "altering url mappings..."
File urlMappings = new File("../grails-app/conf/UrlMappings.groovy")
List<String> lines = []
urlMappings.eachLine{lines << it}
boolean contAct = false
int indexOfConstraints = -1
List<String> newLines = []
lines.eachWithIndex{ String line, int index ->
     if(line.trim() == """"/\$controller/\$action?/\$id?(.\${format})?"{"""){
          println "rewriting controller/action url mapping..."
          newLines << """\t\t"/\$prefix/\$controller/\$action?/\$id?(.\${format})?"{"""
          contAct = true
     } else if(contAct){
          if(line.trim() == "constraints {"){
               indexOfConstraints = index
               newLines << line
          } else if (index -1 == indexOfConstraints){
               println "adding prefix restriction..."
               newLines << "\t\t\t\tprefix(matches: /rest/)" + "\n$line"
               contAct = false
          }
     } else {
          newLines << line
     }
}
urlMappings.write(newLines.join("\n"))
println "UrlMappings.groovy updated."
println "AngularJS boilerplate initialization complete!"


