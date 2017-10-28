@Grab(group='org.yaml', module='snakeyaml', version='1.18')

import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.plugins.credentials.CredentialsStore
import com.cloudbees.plugins.credentials.domains.Domain
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import groovy.transform.Field
import hudson.util.Secret
import java.util.logging.Logger
import jenkins.model.Jenkins
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import org.yaml.snakeyaml.Yaml


@Field
Logger logger = Logger.getLogger('setup-credentials.groovy')

@Field
def env = System.getenv()

@Field
def JENKINS_SETUP_YAML = env['JENKINS_SETUP_YAML'] ?: "${env['JENKINS_CONFIG_HOME']}/setup/main.yml"

@Field
def JENKINS_CREDENTIALS_YAML = env['JENKINS_CREDENTIALS_YAML'] ?: "${env['JENKINS_CONFIG_HOME']}/setup/credentials.yml"

@Field
def config = null

@Field
def PLUGIN = 'com.cloudbees.plugins.credentials.SystemCredentialsProvider'

@Field
def store = Jenkins.instance.getExtensionList(PLUGIN)[0].getStore()


def getSecret(secret) {
  switch (secret.type) {
    case "secretText":
      return new StringCredentialsImpl(
        CredentialsScope.USER,
        secret.id,
        secret.description,
        new Secret(secret.secret_text))
      break
    case "UsernamePassword":
      return new UsernamePasswordCredentialsImpl(
        CredentialsScope.USER,
        secret.id,
        secret.description,
        secret.username,
        secret.password
      )
      break
    default:
      logger.warning('Did not find a secret type for that.')
      logger.warning(secret.getClass().toString())
      logger.info(secret.toString())
      return null
    break
  }
}

def loadCredentials(credentials, domain) {
  credentials.each {
      def secret = getSecret(it)
      if (secret) {
        store.addCredentials(domain, secret)
      }
  }
  Jenkins.instance.save()
}


try {
  logger.info('--> Started setting up Credentials')
  config = new Yaml().load(new File(JENKINS_SETUP_YAML).text)
} catch (Throwable e) {
  logger.warning("--> No configuration file found at ${JENKINS_SETUP_YAML}")
  return
}

if (config.credentials?.global) {
  logger.info('--> Configuring Global Credentials')
  // logger.info(config.credentials.global.toString())
  config.credentials.global.each { domainCredentials ->
    loadCredentials(domainCredentials, Domain.global())//   credential.each {
  }
  logger.info('--> Configuring Global Credentials... done')
}

def getDomain(d) {
  // TODO
  // create domain if it does not exist
  // return domain object if it does exist
  return Domain.global()
}

if (new File(JENKINS_CREDENTIALS_YAML).exists()) {
  logger.info('--> Configuring Extra Credentials')
  def yaml = new Yaml().load(new File(JENKINS_CREDENTIALS_YAML).text)
  // logger.info(yaml.credentials.getClass().toString())
  yaml.credentials?.each {
    // logger.warning(it.toString())
    it.each { domainCredentials ->
      def domain = getDomain(domainCredentials.key)
      loadCredentials(domainCredentials.value, domain)
    }
    logger.info('--> Configuring Extra Credentials... done')
  }
}

// https://github.com/jenkinsci/ssh-credentials-plugin/blob/master/src/main/java/com/cloudbees/jenkins/plugins/sshcredentials/impl/BasicSSHUserPrivateKey.java
if (Jenkins.instance.pluginManager.activePlugins.find { it.shortName == 'ssh-credentials' } != null
  && new File('/var/jenkins_home/.ssh').exists()) {
  // adds SSHUserPrivateKey From the Jenkins master ${HOME}/.ssh
  logger.info('--> Configuring Master SSH Credentials')
  // signature Scope, Id, Username, Keysource, Passphrase, Description
  def sshCredentials = new BasicSSHUserPrivateKey(
      CredentialsScope.GLOBAL,
      'ssh-key-blueocean',
      'blueocean',
      new BasicSSHUserPrivateKey.UsersPrivateKeySource(),
      '',
      'description'
  )
  store.addCredentials(Domain.global(), sshCredentials)
  logger.info('--> Configuring Master SSH Credentials... done')
  Jenkins.instance.save()
}
