@Grab(group='org.yaml', module='snakeyaml', version='1.18')

import groovy.transform.Field
import hudson.tasks.Shell
import java.util.logging.Logger
import jenkins.model.Jenkins
import org.yaml.snakeyaml.Yaml

@Field
Logger logger = Logger.getLogger('setup-master-config.groovy')

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

// setup Time Zone
Thread.start {
  TZ = env['JENKINS_TZ'] ?: config.time_zone ?: 'America/New_York'
  System.setProperty('org.apache.commons.jelly.tags.fmt.timeZone', TZ)
}

// disable sending anonymous usage statistics
Thread.start {
  // adds '<noUsageStatistics>true</noUsageStatistics>' to $JENKINS_HOME/config.xml
  Jenkins.instance.setNoUsageStatistics(true)
  Jenkins.instance.save()
  logger.info("--> Sending anonymous usage statistics is set to " + Jenkins.getInstance().isUsageStatisticsCollected().toString())
}

// setup master executors
Thread.start {
  int executors = env['JENKINS_EXECUTORS'] ?: config.executors?.master.toInteger() ?: 2
  if (Jenkins.instance.getNumExecutors() != executors) {
    Jenkins.instance.setNumExecutors(executors)
    Jenkins.instance.save()
  }
}

Thread.start {
  // Set shell executable location
  def shell = Jenkins.instance.getDescriptorByType(Shell.DescriptorImpl)
  shell.setShell(config.shell_executable ?: '/bin/bash')
  shell.save()
  logger.info('-->  Configured Shell')
}

// setup global git config
Thread.start {
  if (Jenkins.instance.pluginManager.activePlugins.find { it.shortName == 'git' } != null) {
    def PLUGIN = 'hudson.plugins.git.GitSCM'
    def globalConfigName = config.git?.config?.name ?: ''
    def globalConfigEmail = config.git?.config?.email ?: ''

    def descriptor = Jenkins.instance.getDescriptor(PLUGIN)
    if (globalConfigName != descriptor.getGlobalConfigName()) {
        descriptor.setGlobalConfigName(globalConfigName)
    }
    if (globalConfigEmail != descriptor.getGlobalConfigEmail()) {
        descriptor.setGlobalConfigEmail(globalConfigEmail)
    }
    if (!descriptor.equals(Jenkins.instance.getDescriptor(PLUGIN))) {
      descriptor.save()
    }
    logger.info('--> Configured Git SCM')
  }
}

// setup Jenkins generics
Thread.start {
  logger.info('--> Configuring Location')
  def PLUGIN = 'jenkins.model.JenkinsLocationConfiguration'
  def descriptor = Jenkins.instance.getDescriptor(PLUGIN)
  def HOSTNAME = config.hostname ?: env['HOSTNAME'].toString()
  def JENKINS_LOC_URL = String.format("%s://%s:%s", config.web_proto ?: 'http', HOSTNAME, config.web_port ?: '8080')

  if (JENKINS_LOC_URL != descriptor.getUrl()) {
      descriptor.setUrl(JENKINS_LOC_URL)
  }
  if (config.admin?.email != descriptor.getAdminAddress()) {
      descriptor.setAdminAddress(config.admin?.email)
  }
  if (!descriptor.equals(Jenkins.instance.getDescriptor(PLUGIN))) {
      descriptor.save()
      logger.info('--> Configured Location')
  }
}

// setup Mailer configuration
Thread.start {
  def PLUGIN = 'hudson.tasks.Mailer'
  def descriptor = Jenkins.instance.getDescriptor(PLUGIN)
  def smtpEmail = env['SMTP_EMAIL'] ?: config.mailer?.smtp_email ?: ''
  def smtpHost = env['SMTP_HOST'] ?: config.mailer?.smtp_host ?: 'smtp.gmail.com'
  def smtpAuthPasswordSecret = env['SMTP_PASSWORD'] ?: config.mailer?.smtp_password ?: ''
  if(smtpEmail != ''){
    descriptor.setSmtpAuth(smtpEmail, "${smtpAuthPasswordSecret}")
    descriptor.setReplyToAddress(smtpEmail)
    descriptor.setSmtpHost(smtpHost)
    descriptor.setUseSsl(true)
    descriptor.setSmtpPort('465')
    descriptor.setCharset('UTF-8')
    descriptor.save()
    logger.info('--> Configured Mailer')
  }
}

// setup master-slave security
Thread.start {
  if (config.set_master_kill_switch != null) {
  // Configure global master-slave security
    def master_slave_security = { home=env['JENKINS_HOME'], disabled=config.set_master_kill_switch ->
      new File(home + 'secrets/filepath-filters.d').mkdirs()
      new File(home + 'secrets/filepath-filters.d/50-gui.conf').createNewFile()
      new File(home + 'secrets/whitelisted-callables.d').mkdirs()
      new File(home + 'secrets/whitelisted-callables.d/gui.conf').createNewFile()
      Jenkins.instance.getInjector().getInstance(jenkins.security.s2m.AdminWhitelistRule.class).setMasterKillSwitch(disabled)
    }
  logger.info('--> Enabled Master -> Slave Security')
  }
}

// configure sshd port
Thread.start {
  int new_ssh_port = config.sshd_port ?: -1
  def ssh_port = org.jenkinsci.main.modules.sshd.SSHD.get()
  if( (new_ssh_port != ssh_port.getPort()) && (new_ssh_port > 1024) ) {
    /*
     * ports under 1024 are reserved
     * you will get this error if you try:
     *
     * SEVERE: Failed to restart SSHD
     * java.net.SocketException: Permission denied
     *
    */
    ssh_port.setPort(new_ssh_port)
    logger.info('--> setting port for sshd... done')
  }
}
