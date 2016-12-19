@Grab(group='org.yaml', module='snakeyaml', version='1.17')

import org.yaml.snakeyaml.Yaml
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement
import java.util.logging.Logger

env = System.getenv()
JENKINS_SETUP_YAML = env['JENKINS_SETUP_YAML'] ?: "${env['JENKINS_CONFIG_HOME']}/setup.yml"
config = new Yaml().load(new File(JENKINS_SETUP_YAML).text)
Logger logger = Logger.getLogger('seed.groovy')


Thread.start {
    try {
        def seedJobDsl = config.seed_jobdsl
        WORKSPACE_BASE = "${env['JENKINS_HOME']}/workspace"
        def workspace = new File("${WORKSPACE_BASE}")
        // workspace.mkdirs()
        // def seedJobDsl = new File("${WORKSPACE_BASE}/seed.groovy")
        // logger.info(seedJobDsl)

        def jobManagement = new JenkinsJobManagement(System.out, [:], workspace)
        new DslScriptLoader(jobManagement).runScript(seedJobDsl)
        logger.info('Created first job from seed')
    } catch(e) {
        logger.info('Did not create seed job')
    }
}
