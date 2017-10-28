@Grab(group='org.yaml', module='snakeyaml', version='1.18')

import com.cloudbees.plugins.credentials.CredentialsProvider
import groovy.transform.Field
import hudson.model.Computer
import hudson.model.Hudson
import hudson.model.Item
import hudson.security.HudsonPrivateSecurityRealm
import hudson.security.Permission
import hudson.security.ProjectMatrixAuthorizationStrategy
import java.util.logging.Logger
import jenkins.model.Jenkins
import org.yaml.snakeyaml.Yaml

@Field
def config = null

@Field
def env = System.getenv()

@Field
def JENKINS_SETUP_YAML = env['JENKINS_SETUP_YAML'] ?: "${env['JENKINS_CONFIG_HOME']}/setup/main.yml"

@Field
Logger logger = Logger.getLogger('setup-matrix-auth.groovy')

@Field
def PLUGIN = 'com.cloudbees.plugins.credentials.SystemCredentialsProvider'

try {
  config = new Yaml().load(new File(JENKINS_SETUP_YAML).text)
} catch (Throwable e) {
  logger.warning("--> No configuration file found at ${JENKINS_SETUP_YAML}")
  return
}

class BuildPermission {
  static buildNewAccessList(userOrGroup, permissions) {
    def newPermissionsMap = [:]
    permissions.each {
      newPermissionsMap.put(Permission.fromId(it), userOrGroup)
    }
    return newPermissionsMap
  }
}

if (config.github) {
  logger.warning("--> Skipping Matrix Authorization configuration... done")
  return
}

logger.info('--> Started matrix authorization configuration')
if (Jenkins.instance.pluginManager.activePlugins.find { it.shortName == 'matrix-auth' } != null) {
  if (config.admin?.username && config.admin?.password) {
    def hudsonRealm = new HudsonPrivateSecurityRealm(false)
    def adminUsername = config.admin.username
    def adminPassword = config.admin.password
    hudsonRealm.createAccount(adminUsername, adminPassword)
    logger.info("--> Created Admin account")
    Jenkins.instance.setSecurityRealm(hudsonRealm)
    logger.info("--> Setting Project Matrix authorization strategy")
  }
}

strategy = new ProjectMatrixAuthorizationStrategy()

authenticatedPermissions = [
    "hudson.model.Hudson.Read",
    "hudson.model.Item.ExtendedRead",
    "hudson.model.Item.Build",
    "hudson.model.Item.Cancel",
]
credentialsPermissions = [
    "com.cloudbees.plugins.credentials.CredentialsProvider.Create",
    "com.cloudbees.plugins.credentials.CredentialsProvider.Delete",
    "com.cloudbees.plugins.credentials.CredentialsProvider.ManageDomains",
    "com.cloudbees.plugins.credentials.CredentialsProvider.Update",
    "com.cloudbees.plugins.credentials.CredentialsProvider.View",
]
swarmPermissions = [
    "hudson.model.Computer.Build",
    "hudson.model.Computer.Configure",
    "hudson.model.Computer.Create",
    "hudson.model.Computer.Delete",
    "hudson.model.Computer.Connect",
    "hudson.model.Computer.Disconnect",
]

anonymousPermissions = []
anonymous = BuildPermission.buildNewAccessList("anonymous", anonymousPermissions)
anonymous.each { p, u -> strategy.add(p, u) }

authenticated = BuildPermission.buildNewAccessList("authenticated", authenticatedPermissions)
authenticated.each { p, u -> strategy.add(p, u) }

strategy.add(Jenkins.ADMINISTER, config.admin?.username )

// jenkinsAdmin = BuildPermission.buildNewAccessList(team, credentialsPermissions)
// jenkinsAdmin.each { p, u -> strategy.add(p, u) }

// def hudsonRealm = new HudsonPrivateSecurityRealm(false)
// hudsonRealm.createAccount(config.swarm.username ?: 'swarmAgent', config.swarm.password ?: 'swarmPassword')
// Jenkins.instance.setSecurityRealm(hudsonRealm)
//
// jenkinsSwarm = BuildPermission.buildNewAccessList(config.github.teams.admin, swarmPermissions) //config.swarm.username
// jenkinsSwarm.each { p, u -> strategy.add(p, u) }

Jenkins.instance.setAuthorizationStrategy(strategy)
Jenkins.instance.save()
logger.info('--> Finished setting up matrix authorization')
