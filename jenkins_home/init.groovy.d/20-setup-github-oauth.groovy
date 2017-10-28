// Resource: https://plugins.jenkins.io/github-oauth
@Grab(group='org.yaml', module='snakeyaml', version='1.18')

import groovy.transform.Field
import hudson.security.ProjectMatrixAuthorizationStrategy
import java.util.logging.Logger
import jenkins.model.Jenkins
import org.yaml.snakeyaml.Yaml

@Field
Logger logger = Logger.getLogger('setup-github-oauth.groovy')

@Field
def env = System.getenv()

@Field
def JENKINS_SETUP_YAML = env['JENKINS_SETUP_YAML'] ?: "${env['JENKINS_CONFIG_HOME']}/setup/main.yml"

@Field
def config = null

try {
  config = new Yaml().load(new File(JENKINS_SETUP_YAML).text)
} catch (Throwable e) {
  logger.warning("--> No configuration file found at ${JENKINS_SETUP_YAML}")
  return
}

// logger.warning(config.github.getClass().toString()) // LinkedHashMap
if (!config.github) {
  logger.warning("--> Skipping Github Security Realm configuration... done")
  return
}

// Automatically configure security realm via script console
import hudson.security.Permission
import hudson.security.SecurityRealm
import org.jenkinsci.plugins.GithubSecurityRealm

String githubWebUri = config.github?.oauth?.githubWebUri ?: 'https://github.com'
String githubApiUri = config.github?.oauth?.githubApiUri ?: 'https://api.github.com'
String clientID = config.github?.oauth?.clientID ?: 'someid'
String clientSecret = config.github?.oauth?.clientSecret ?: 'somesecret'
String oauthScopes = config.github?.oauth?.oauthScopes ?: 'read:org'
SecurityRealm githubRealm = new GithubSecurityRealm(githubWebUri, githubApiUri, clientID, clientSecret, oauthScopes)
// check for equality, no need to modify the runtime if no settings changed
if(!githubRealm.equals(Jenkins.instance.getSecurityRealm()) && !clientID.equals('someid')) {
    Jenkins.instance.setSecurityRealm(githubRealm)
    Jenkins.instance.save()
    logger.info('--> Configuring Github Security Realm... done')
}

@Field
def strategy = new ProjectMatrixAuthorizationStrategy()

if (config.github?.teams?.admin) {
  config.github?.teams?.admin.each { admin ->
    strategy.add(Jenkins.ADMINISTER, admin)
    logger.info("--> Setting ${admin} as Administrators... done")
  }
}

if (config.github?.teams?.agents) {
  config.github?.teams?.agents.each { agent ->
    // strategy.add(agentPermissions, agent)
    def agents = BuildPermission.buildNewAccessList(agentsPermissions, agent)
    agents.each { p, u -> strategy.add(p, u) }
    logger.info("--> Setting ${agent} as Agents... done")
  }
}

def authenticated = BuildPermission.buildNewAccessList(authenticatedPermissions, "authenticated")
authenticated.each { p, u -> strategy.add(p, u) }

def anonymous = BuildPermission.buildNewAccessList(anonymousPermissions, "anonymous")
anonymous.each { p, u -> strategy.add(p, u) }

Jenkins.instance.setAuthorizationStrategy(strategy)
Jenkins.instance.save()


class BuildPermission {
  static buildNewAccessList(permissions, userOrGroup) {
    def newPermissionsMap = [:]
    permissions.each {
      newPermissionsMap.put(Permission.fromId(it), userOrGroup)
    }
    return newPermissionsMap
  }
}

@Field
agentsPermissions = [
  "hudson.model.Computer.Build",
  "hudson.model.Computer.Configure",
  "hudson.model.Computer.Create",
  "hudson.model.Computer.Delete",
  "hudson.model.Computer.Connect",
  "hudson.model.Computer.Disconnect",
]

@Field
authenticatedPermissions = [
  "hudson.model.Hudson.Read",
  "hudson.model.Item.ExtendedRead",
  "hudson.model.Item.Build",
  "hudson.model.Item.Cancel",
  "com.cloudbees.plugins.credentials.CredentialsProvider.View",
  "hudson.model.View.Configure",
]

@Field
anonymousPermissions = []
