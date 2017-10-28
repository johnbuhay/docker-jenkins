@Grab(group='org.yaml', module='snakeyaml', version='1.18')

import groovy.transform.Field
import hudson.plugins.git.BranchSpec
import hudson.plugins.git.GitSCM
import hudson.plugins.git.UserRemoteConfig
import java.lang.System
import java.util.Collections
import java.util.List
import java.util.logging.Logger
import jenkins.model.Jenkins
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.workflow.libs.SCMRetriever
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever
import org.yaml.snakeyaml.Yaml

@Field
Logger logger = Logger.getLogger('setup-global-pipeline-libs.groovy')

@Field
def env = System.getenv()

@Field
def JENKINS_PIPELINES_YAML = env['JENKINS_PIPELINES_YAML'] ?: "${env['JENKINS_CONFIG_HOME']}/setup/pipelines.yml"

@Field
def config = null


try {
  config = new Yaml().load(new File(JENKINS_PIPELINES_YAML).text)
} catch (Throwable e) {
  logger.warning("--> No configuration file found at ${JENKINS_SETUP_YAML}")
  return
}

if(config.pipeline_libs) {
  logger.info('--> Configuring global pipeline libraries')
  List<LibraryConfiguration> libs = new ArrayList<LibraryConfiguration>()

  config.pipeline_libs.each { library ->
      List<UserRemoteConfig> userRemotes = new ArrayList<UserRemoteConfig>()
      userRemotes.add(
        new UserRemoteConfig(
          library.scm_repo, '', '', library.scm_scan_creds
        )
      )
      List<BranchSpec> branches = Collections.singletonList(
        new BranchSpec(library.branch ?: 'master')
      )
      if(library.scm_type == 'github') {
          def id = library.id ?: ''
          def scm_api_uri = library.scm_api_uri ?: ''
          def scm_checkout_creds_id = library.scm_checkout_creds_id ?: 'SAME'
          def scm_scan_creds = library.scm_scan_creds
          def scm_owner = library.scm_owner
          def scm_repo = library.scm_repo
          GitHubSCMSource github_scm = new GitHubSCMSource(
            id, scm_api_uri, scm_checkout_creds_id, scm_scan_creds, scm_owner, scm_repo
          )
          globalConfig = new LibraryConfiguration(library.name, new SCMSourceRetriever(github_scm))
      } else {
          GitSCM git_scm = new GitSCM(userRemotes,
                                     branches,
                                     false,
                                     null, null, null, null)
          globalConfig = new LibraryConfiguration(library.name, new SCMRetriever(git_scm))

      }
      globalConfig.setDefaultVersion(library.default_version ?: 'master')
      globalConfig.setImplicit(library.implicitly ?: true)
      globalConfig.setAllowVersionOverride(library.allow_version_override ?: false)
      libs.add(globalConfig)
  }
  GlobalLibraries globalLibs = GlobalLibraries.get()
  globalLibs.setLibraries(libs)
  logger.info('--> Configuring global pipeline libraries... done')
}
