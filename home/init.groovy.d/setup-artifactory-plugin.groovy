import jenkins.model.*

import org.jfrog.*
import org.jfrog.hudson.*
//import org.jfrog.hudson.util.Credentials;

Thread.start {
  def instance = Jenkins.getInstance()
  def descriptor = instance.getDescriptor("org.jfrog.hudson.ArtifactoryBuilder")

  String server_id = System.getenv('ARTIFACTORY_HOSTNAME') ?: 'localhost'
  String artifactory_url = "http://${server_id}:8081/artifactory"

  def deployer_username = System.getenv('ARTIFACTORY_DEPLOYER_USERNAME') ?: 'admin'
  def deployer_password = System.getenv('ARTIFACTORY_DEPLOYER_PASSWORD') ?: 'password'
  def deployerCredentialsConfig = new CredentialsConfig(deployer_username, deployer_password, "deployer")

  def resolver_username = System.getenv('ARTIFACTORY_RESOLVER_USERNAME') ?: ''
  def resolver_password = System.getenv('ARTIFACTORY_RESOLVER_PASSWORD') ?: ''
  def resolverCredentialsConfig = new CredentialsConfig(resolver_username, resolver_password, "resolver")  // for Authenticated downloads

  def server = [
    new ArtifactoryServer(
      server_id,
      artifactory_url,
      deployerCredentialsConfig,
      resolverCredentialsConfig,
      300,  // Connection Timeout
      false  // Bypass HTTP Proxy
    )
  ]

  descriptor.setArtifactoryServers(server)
  descriptor.save()

}
