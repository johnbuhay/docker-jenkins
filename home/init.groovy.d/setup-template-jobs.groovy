import jenkins.model.*


String server_id = System.getenv('ARTIFACTORY_HOSTNAME') ?: 'localhost'
String artifactory_url = "http://${server_id}:8081/artifactory"

def artifactory_repositories = [
  //"libs-java-release",
  //"libs-java-snapshot"
  "libs-release-local",
  "libs-snapshot-local"
]

artifactory_repositories.each {
  def job_name = "template_freestyle_" + it.toString()
  if (!Jenkins.instance.getItem(job_name)) {
    def tfs_job_xml = """\
      <?xml version='1.0' encoding='UTF-8'?>
      <project>
        <actions/>
        <description></description>
        <keepDependencies>false</keepDependencies>
        <properties>
          <hudson.plugins.throttleconcurrents.ThrottleJobProperty plugin="throttle-concurrents@1.8.4">
            <maxConcurrentPerNode>0</maxConcurrentPerNode>
            <maxConcurrentTotal>0</maxConcurrentTotal>
            <throttleEnabled>true</throttleEnabled>
            <throttleOption>project</throttleOption>
          </hudson.plugins.throttleconcurrents.ThrottleJobProperty>
        </properties>
        <scm class="hudson.scm.NullSCM"/>
        <canRoam>true</canRoam>
        <disabled>true</disabled>
        <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
        <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
        <triggers/>
        <concurrentBuild>false</concurrentBuild>
        <builders/>
        <publishers/>
        <buildWrappers>
          <hudson.plugins.ansicolor.AnsiColorBuildWrapper plugin="ansicolor@0.4.2">
            <colorMapName>xterm</colorMapName>
          </hudson.plugins.ansicolor.AnsiColorBuildWrapper>
          <org.jfrog.hudson.generic.ArtifactoryGenericConfigurator plugin="artifactory@2.4.7">
            <details>
              <artifactoryName>${server_id}</artifactoryName>
              <artifactoryUrl>${artifactory_url}</artifactoryUrl>
              <deployReleaseRepository>
                <keyFromText>${it}</keyFromText>
                <keyFromSelect></keyFromSelect>
                <dynamicMode>true</dynamicMode>
              </deployReleaseRepository>
              <stagingPlugin/>
            </details>
            <resolverDetails>
              <artifactoryName>${server_id}</artifactoryName>
              <artifactoryUrl>${artifactory_url}</artifactoryUrl>
              <stagingPlugin/>
            </resolverDetails>
            <deployerCredentialsConfig>
              <credentials>
                <username></username>
                <password></password>
              </credentials>
              <credentialsId></credentialsId>
              <overridingCredentials>false</overridingCredentials>
            </deployerCredentialsConfig>
            <resolverCredentialsConfig>
              <credentials>
                <username></username>
                <password></password>
              </credentials>
              <credentialsId></credentialsId>
              <overridingCredentials>false</overridingCredentials>
            </resolverCredentialsConfig>
            <deployPattern>artifacts/**/*.jar
      artifacts/**/*.pom
      target/**/*.jar</deployPattern>
            <resolvePattern></resolvePattern>
            <matrixParams></matrixParams>
            <deployBuildInfo>true</deployBuildInfo>
            <includeEnvVars>true</includeEnvVars>
            <envVarsPatterns>
              <includePatterns></includePatterns>
              <excludePatterns>*password*,*secret*</excludePatterns>
            </envVarsPatterns>
            <discardOldBuilds>false</discardOldBuilds>
            <discardBuildArtifacts>true</discardBuildArtifacts>
            <multiConfProject>false</multiConfProject>
          </org.jfrog.hudson.generic.ArtifactoryGenericConfigurator>
        </buildWrappers>
      </project>
    """.stripIndent()

    def xmlStream = new ByteArrayInputStream( tfs_job_xml.getBytes() )
    try {
      def this_job = Jenkins.instance.createProjectFromXML(job_name, xmlStream)
    } catch (ex) {
      println "ERROR: ${ex}"
      println tfs_job_xml.stripIndent()
    }
  }

}
