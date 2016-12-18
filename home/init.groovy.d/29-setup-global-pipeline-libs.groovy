@Grab(group='org.yaml', module='snakeyaml', version='1.17')

import org.yaml.snakeyaml.Yaml
import java.util.logging.Logger

import jenkins.model.*
import jenkins.security.*

env = System.getenv()
JENKINS_SETUP_YAML = env['JENKINS_SETUP_YAML'] ?: "${env['JENKINS_CONFIG_HOME']}/setup.yml"
Logger logger = Logger.getLogger('setup-github.groovy')
def config = new Yaml().load(new File(JENKINS_SETUP_YAML).text)

import java.lang.System
import hudson.model.*
import hudson.plugins.git.GitSCM
import hudson.plugins.git.*
import org.jenkinsci.plugins.workflow.libs.*
import java.util.Collections
import java.util.List
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource


GlobalLibraries global_libs = GlobalLibraries.get()
List<LibraryConfiguration> libs = new ArrayList<LibraryConfiguration>()

config.pipeline_libs.each() {
    List<UserRemoteConfig> user_remotes = new ArrayList<UserRemoteConfig>()
    user_remotes.add(new UserRemoteConfig(it.scm_repo, '', '', it.scm_scan_creds))
    List<BranchSpec> branches = Collections.singletonList(new BranchSpec(it.branch ?: 'master'))
    if(it.scm_type == 'github') {
        def id = it.id ?: ''
        def scm_api_uri = it.scm_api_uri ?: ''
        def scm_checkout_creds_id = it.scm_checkout_creds_id ?: 'SAME'
        def scm_scan_creds = it.scm_scan_creds
        def scm_owner = it.scm_owner
        def scm_repo = it.scm_repo
        GitHubSCMSource github_scm = new GitHubSCMSource(id, scm_api_uri, scm_checkout_creds_id, scm_scan_creds, scm_owner, scm_repo)
        global_config = new LibraryConfiguration(it.name, new SCMSourceRetriever(github_scm))
    } else {
        GitSCM git_scm = new GitSCM(user_remotes,
                                   branches,
                                   false,
                                   null, null, null, null)
        global_config = new LibraryConfiguration(it.name, new SCMRetriever(git_scm))
        
    }
    global_config.setDefaultVersion(it.default_version ?: 'master')
    global_config.setImplicit(it.implicitly ?: true)
    global_config.setAllowVersionOverride(it.allow_version_override ?: false)
    libs.add(global_config)

}

global_libs.setLibraries(libs)

logger.info('--> Configured global pipeline libraries')
