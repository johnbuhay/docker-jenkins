@Grab(group='org.yaml', module='snakeyaml', version='1.17')

import org.yaml.snakeyaml.Yaml
import java.util.logging.Logger


import jenkins.model.*
import jenkins.security.*

import groovy.json.*
import org.jenkinsci.plugins.github.config.GitHubServerConfig
import org.jenkinsci.plugins.github.GitHubPlugin

def Boolean has_changed = false

env = System.getenv()
JENKINS_SETUP_YAML = env['JENKINS_SETUP_YAML'] ?: "${env['JENKINS_CONFIG_HOME']}/setup.yml"
Logger logger = Logger.getLogger('setup-github-org-plugin.groovy')
def config = new Yaml().load(new File(JENKINS_SETUP_YAML).text)


// setup github organization plugin
try {
    def JENKINS = Jenkins.getInstance()
    def PLUGIN = 'github-plugin-configuration'
    def descriptor = JENKINS.getDescriptor(PLUGIN)
    // def githubPluginConfig = JENKINS.getExtensionList('org.jenkinsci.plugins.github.config.GitHubPluginConfig')[0]
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
    // githubPluginConfig.setConfigs([githubServerConfig])
    has_changed = (configs.size() > 0) // TODO: make this an actual modified check

    // Save new configuration to disk
    descriptor.save()
    // githubPluginConfig.save()
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


// Adapted from https://github.com/infOpen/ansible-role-jenkins/blob/master/files/groovy_scripts/remove_jenkins_github_servers.groovy

// import jenkins.model.Jenkins
import jenkins.branch.OrganizationFolder
import org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProjectFactory
import com.cloudbees.hudson.plugins.folder.computed.PeriodicFolderTrigger
import com.cloudbees.hudson.plugins.folder.computed.DefaultOrphanedItemStrategy

config.github.folders.each {
    // Create organization folder
    def github_api_uri = ''
    def github_account = it.owner
    def github_creds = it.scan_creds
    def orgFolder = new OrganizationFolder(Jenkins.getInstance(), github_account)

    // Create Github navigator for organization folder
    def githubNavigator = new GitHubSCMNavigator(github_api_uri, github_account, github_creds, 'SAME')
    orgFolder.getNavigators().push(githubNavigator)

    // Set folder to look for Jenkinsfiles in repositories
    orgFolder.getProjectFactories().push(new WorkflowMultiBranchProjectFactory())

    // Set periodic trigger so the folder is at most 1 hour out of date
    def periodicFolderTrigger = new PeriodicFolderTrigger("1h")
    orgFolder.addTrigger(periodicFolderTrigger)

    // Set orphaned item strategy of 90 days/90 items maximums
    def orphanedItemStrategy = new DefaultOrphanedItemStrategy(true, "90", "90")
    orgFolder.setOrphanedItemStrategy(orphanedItemStrategy)

    // Add organization folder to Jenkins
    Jenkins.getInstance().putItem(orgFolder)

    // Trigger initial computation of organisation folder
    orgFolder.getComputation().run() //orgFolder.scheduleBuild()
}

//  https://github.com/williamroberts/jenkins/tree/master/groovy-scripts

