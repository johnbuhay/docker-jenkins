@Grab(group='org.yaml', module='snakeyaml', version='1.18')

import groovy.transform.Field
import java.util.logging.Logger
import jenkins.model.Jenkins
import org.yaml.snakeyaml.Yaml

@Field
Logger logger = Logger.getLogger('tcp-slave-agent-port.groovy')

@Field
def env = System.getenv()

@Field
def JENKINS_SETUP_YAML = env['JENKINS_SETUP_YAML'] ?: "${env['JENKINS_CONFIG_HOME']}/setup/main.yml"

@Field
def config = null

try {
  config = new Yaml().load(new File(JENKINS_SETUP_YAML).text)
} catch (java.io.FileNotFoundException f) {
  logger.warning("--> No configuration file found at ${JENKINS_SETUP_YAML}")
  return
}


Thread.start {
    sleep(8000)
    logger.info('--> setting agent port for jnlp')
    int port = config.jnlp_port ?: env['JENKINS_SLAVE_AGENT_PORT'].toInteger() ?: 50000
    Jenkins.instance.setSlaveAgentPort(port)
    logger.info('--> setting agent port for jnlp... done')
}
