
import jenkins.model.*
//import javaposse.jobdsl.plugin
import com.cloudbees.hudson.plugins.folder.Folder

def JENKINS_HOME = '/var/jenkins_home/'
def JOBS_DIR = '/var/jenkins_home/jobs/'
String buildXML = ""

def local = System.getenv("BUILD").toString()
if(local == "local") {
  def localXML = """\
    <builders>
      <hudson.tasks.Shell>
        <command>test -L ci || ln -s /ci ci </command>
      </hudson.tasks.Shell>
      <javaposse.jobdsl.plugin.ExecuteDslScripts plugin="job-dsl@1.4">
        <targets>ci/master.groovy</targets>
        <usingScriptText>false</usingScriptText>
        <ignoreExisting>false</ignoreExisting>
        <removedJobAction>DELETE</removedJobAction>
        <lookupStrategy>JENKINS_ROOT</lookupStrategy>
      </javaposse.jobdsl.plugin.ExecuteDslScripts>
    </builders>
    <publishers>
      <hudson.tasks.BuildTrigger>
        <childProjects>ci/master-downstream</childProjects>
        <threshold>
          <name>SUCCESS</name>
          <ordinal>0</ordinal>
          <color>BLUE</color>
          <completeBuild>true</completeBuild>
        </threshold>
      </hudson.tasks.BuildTrigger>
    </publishers>
    """
    buildXML = localXML
}
else {
  println "Not local, so checkout a repo"
}

def sparkXml = """\
  <?xml version='1.0' encoding='UTF-8'?>
  <project>
    <actions/>
    <description>This job is a template file that will run a job dsl.</description>
    <keepDependencies>false</keepDependencies>
    <properties>
      <hudson.plugins.throttleconcurrents.ThrottleJobProperty plugin="throttle-concurrents@1.8.4">
        <maxConcurrentPerNode>0</maxConcurrentPerNode>
        <maxConcurrentTotal>0</maxConcurrentTotal>
        <throttleEnabled>false</throttleEnabled>
        <throttleOption>project</throttleOption>
      </hudson.plugins.throttleconcurrents.ThrottleJobProperty>
    </properties>
    <scm class="hudson.scm.NullSCM"/>
    <canRoam>true</canRoam>
    <disabled>false</disabled>
    <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
    <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
    <triggers/>
    <concurrentBuild>false</concurrentBuild>
    ${buildXML}
    <buildWrappers/>
  </project>
  """.stripIndent()

//folder = Folder('smart')  folderName + "/" + 
def folderName = 'ci'
def jobName = 'spark'
if (!Jenkins.instance.getItem(jobName)) {
  def xmlStream = new ByteArrayInputStream( sparkXml.getBytes() )
  try {
    def seedJob = Jenkins.instance.createProjectFromXML(jobName, xmlStream)
    seedJob.scheduleBuild(0, null)
  } catch (ex) {
    println "ERROR: ${ex}"
    println sparkXml.stripIndent()
  }
}
