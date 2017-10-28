// https://github.com/DataDog/jenkins-datadog-plugin
import groovy.transform.Field
import jenkins.model.Jenkins
import org.datadog.jenkins.plugins.datadog.DatadogBuildListener

@Field
Logger logger = Logger.getLogger('setup-datadog.groovy')

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


if ( Jenkins.instance.pluginManager.activePlugins.find { it.shortName == "datadog" } != null ) {
  logger.info('--> Configuring datadog plugin')
  def j = Jenkins.getInstance()
  def d = j.getDescriptor("org.datadog.jenkins.plugins.datadog.DatadogBuildListener")
  def h = config.hostname ?: env['HOSTNAME'].toString()
  def url = String.format("%s://%s:%s", config.web_proto ?: 'http', HOSTNAME, config.web_port ?: '8080')
  d.setHostname(url)
  d.setTagNode(true)
  d.setApiKey(config.datadog?.apikey ?: env['DATADOG_API_KEY'].toString())
  d.setBlacklist(','.join(config.datadog?.black_list ?: ''))
  d.save()
  logger.info('--> Configuring datadog plugin... Done')
}
