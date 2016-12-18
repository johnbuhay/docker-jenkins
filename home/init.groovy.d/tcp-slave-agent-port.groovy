@Grab(group='org.yaml', module='snakeyaml', version='1.17')

import org.yaml.snakeyaml.Yaml
import java.util.logging.Logger

import hudson.model.*;
import jenkins.model.*;

def env = System.getenv()
JENKINS_SETUP_YAML = env['JENKINS_SETUP_YAML'] ?: "${env['JENKINS_CONFIG_HOME']}/setup.yml"
Logger logger = Logger.getLogger('tcp-slave-agent-port.groovy')
def config = new Yaml().load(new File(JENKINS_SETUP_YAML).text)

Thread.start {
    sleep 8000
    logger.info('--> setting agent port for jnlp')    
    int port = config.jnlp_port ?: env['JENKINS_SLAVE_AGENT_PORT'].toInteger() ?: 50000
    Jenkins.instance.setSlaveAgentPort(port)
    logger.info('--> setting agent port for jnlp... done')
}
