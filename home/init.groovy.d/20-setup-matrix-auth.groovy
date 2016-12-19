@Grab(group='org.yaml', module='snakeyaml', version='1.17')

import org.yaml.snakeyaml.Yaml
import java.util.logging.Logger

import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.CredentialsStore
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*

import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import com.cloudbees.plugins.credentials.CredentialsScope
import com.cloudbees.jenkins.plugins.sshcredentials.impl.*

import jenkins.model.*
import jenkins.security.*
import hudson.model.*
import hudson.security.*
import hudson.plugins.sshslaves.*
import hudson.util.Secret

env = System.getenv()
JENKINS_SETUP_YAML = env['JENKINS_SETUP_YAML'] ?: "${env['JENKINS_CONFIG_HOME']}/setup.yml"
Logger logger = Logger.getLogger('setup-matrix-auth.groovy')
def config = new Yaml().load(new File(JENKINS_SETUP_YAML).text)
def JENKINS = Jenkins.getInstance()
def PLUGIN = 'com.cloudbees.plugins.credentials.SystemCredentialsProvider'

logger.info('Started setting up matrix auth')
// setup global credentials
    
    
    credentials_store = JENKINS.getExtensionList(PLUGIN)[0].getStore()

    if((env['JENKINS_GITHUB_USER'] != null) && (env['JENKINS_GITHUB_TOKEN'] != null)) {
        def master_creds = [:]
        master_creds['username'] = env['JENKINS_GITHUB_USER']
        master_creds['password'] = env['JENKINS_GITHUB_TOKEN']
        master_creds['id'] = 'master-creds'
        def master_token = [:]
        master_token['secret_text'] = env['JENKINS_GITHUB_TOKEN']
        master_token['id'] = 'master-token'
        config.credentials.global[0].add(master_creds)
        config.credentials.global[0].add(master_token)
    }

    config.credentials.each {
        it.global.each {
            if(it.username) {
                def credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.USER,
                    it.id, it.description, it.username, it.password)
                credentials_store.addCredentials(Domain.global(), credentials)
            }
            if(it.secret_text){
                def secret = new Secret(it.secret_text)
                def string_credentials = new StringCredentialsImpl(CredentialsScope.USER,
                    it.id, it.description, secret)
                credentials_store.addCredentials(Domain.global(), string_credentials)
            }
        }
    }
    JENKINS.save()
    logger.info('Configured Global Credentials')


// setup master ssh key
    logger.info('Started Master SSH Credentials thread')
    if (Jenkins.instance.pluginManager.activePlugins.find { it.shortName == 'ssh-credentials' } != null
        && new File('/var/jenkins_home/.ssh').exists()) {
        // adds SSHUserPrivateKey From the Jenkins master ${HOME}/.ssh
        credentials_store = Jenkins.instance.getExtensionList(PLUGIN)[0].getStore()

        // signature Scope, Id, Username, Keysource, Passphrase, Description
        // https://github.com/jenkinsci/ssh-credentials-plugin/blob/master/src/main/java/com/cloudbees/jenkins/plugins/sshcredentials/impl/BasicSSHUserPrivateKey.java
        credentials = new BasicSSHUserPrivateKey(
            CredentialsScope.GLOBAL,'ssh-key-sirjenkins','sirjenkins',
            new BasicSSHUserPrivateKey.UsersPrivateKeySource(),'','description')

        credentials_store.addCredentials(Domain.global(), credentials)
        logger.info('Configured Master SSH Credentials')
    }


// setup matrix-auth configuration
// Thread.start {
    logger.info('Started Matrix-Auth AuthorizationStrategy')
    if (JENKINS.pluginManager.activePlugins.find { it.shortName == 'matrix-auth' } != null) {
        def hudson_realm = new HudsonPrivateSecurityRealm(false)
        def admin_username = env['JENKINS_ADMIN_USERNAME'] ?: config.admin.username ?: 'admin'
        def admin_password = env['JENKINS_ADMIN_PASSWORD'] ?: config.admin.password ?: 'password'
        hudson_realm.createAccount(admin_username, admin_password)

        JENKINS.setSecurityRealm(hudson_realm)
        
        def strategy = new hudson.security.GlobalMatrixAuthorizationStrategy()
        //  Setting Anonymous Permissions
        strategy.add(hudson.model.Hudson.READ,'anonymous')
        strategy.add(hudson.model.Item.BUILD,'anonymous')
        strategy.add(hudson.model.Item.CANCEL,'anonymous')
        strategy.add(hudson.model.Item.DISCOVER,'anonymous')
        strategy.add(hudson.model.Item.READ,'anonymous')
        // Setting Admin Permissions
        strategy.add(Jenkins.ADMINISTER, 'admin')
        // Setting easy settings for local development
        if (env['BUILD_ENV'] == 'local') {
          //  Overall Permissions
          strategy.add(hudson.model.Hudson.ADMINISTER,'anonymous')
          strategy.add(hudson.PluginManager.CONFIGURE_UPDATECENTER,'anonymous')
          strategy.add(hudson.model.Hudson.READ,'anonymous')
          strategy.add(hudson.model.Hudson.RUN_SCRIPTS,'anonymous')
          strategy.add(hudson.PluginManager.UPLOAD_PLUGINS,'anonymous')
        }

        if (!hudson_realm.equals(Jenkins.instance.getSecurityRealm())) {
            // Jenkins.instance.setSecurityRealm(hudson_realm)
            // Jenkins.instance.save()
            JENKINS.setAuthorizationStrategy(strategy)
            JENKINS.save()
        }
        logger.info('Configured AuthorizationStrategy')
    }


logger.info('Finished setting up matrix auth')
