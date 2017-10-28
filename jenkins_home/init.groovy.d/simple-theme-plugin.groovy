import java.util.logging.Logger
import jenkins.model.Jenkins

Logger logger = Logger.getLogger('simple-theme-plugin.groovy')
def env = System.getenv()

if ( Jenkins.instance.pluginManager.activePlugins.find { it.shortName == "simple-theme-plugin" } != null ) {
  logger.info('--> setting theme for simple-theme-plugin')
  def theme = Jenkins.instance.getExtensionList("org.codefirst.SimpleThemeDecorator")[0]
  def cssPath = 'userContent/layout/theme.css'
  def jsPath = 'userContent/layout/theme.js'
  if (new File("${env.JENKINS_HOME}/${cssPath}").exists()) {
    theme.cssUrl = cssPath
  }
  if (new File("${env.JENKINS_HOME}/${jsPath}").exists()) {
    theme.jsUrl = jsPath
  }
  theme.save()
  logger.info('--> setting theme for simple-theme-plugin... done')
}
