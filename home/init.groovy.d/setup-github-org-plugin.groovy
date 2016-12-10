@Grab(group='org.yaml', module='snakeyaml', version='1.17')

import org.yaml.snakeyaml.Yaml
import java.util.logging.Logger


import jenkins.model.*
import jenkins.security.*
// import hudson.model.*
// import hudson.security.*

import groovy.json.*
import org.jenkinsci.plugins.github.config.GitHubServerConfig
import org.jenkinsci.plugins.github.GitHubPlugin

def Boolean has_changed = false


env = System.getenv()
JENKINS_SETUP_YAML = env['JENKINS_SETUP_YAML'] ?: "${env['JENKINS_CONFIG_HOME']}/setup.yml"
Logger logger = Logger.getLogger('setup-github-org-plugin.groovy')
def config = new Yaml().load(new File(JENKINS_SETUP_YAML).text)


// setup github organization plugin
Thread.start {
    try {
        def JENKINS = Jenkins.getInstance()
        def PLUGIN = 'github-plugin-configuration'
        def descriptor = JENKINS.getDescriptor(PLUGIN)
        def github_servers = []

        // create objects from config
        config.github.servers.each {
          def ghs = new GitHubServerConfig(it.credentials)
          if(it.api_url){ghs.setApiUrl(it.api_url)}
          if(it.client_cache_size){ghs.setClientCacheSize(it.client_cache_size)}
          if(it.manage_hooks){ghs.allowedToManageHooks(it.manage_hooks)}
          github_servers.add(ghs)
        }

        // Manage new configuration
        def List<GitHubServerConfig> configs = descriptor.getConfigs()

        // Set github plugin configuration
        descriptor.setConfigs(github_servers)
        has_changed = (configs.size() > 0) // TODO: make this an actual modified check

        // Save new configuration to disk
        descriptor.save()
        JENKINS.save()
    }
    catch(Exception e) {
        throw new RuntimeException(e.getMessage())
    }

    // Build json result
    result = new JsonBuilder()
    result {
        changed has_changed.any()
        output {
            changed has_changed
        }
    }

    println result
}


// Adapted from https://github.com/infOpen/ansible-role-jenkins/blob/master/files/groovy_scripts/remove_jenkins_github_servers.groovy
