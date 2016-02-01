import jenkins.model.*
import org.jvnet.hudson.plugins.SbtPluginBuilder

Thread.start {
  def instance = Jenkins.getInstance()

  def descriptor = instance.getDescriptor("org.jvnet.hudson.plugins.SbtPluginBuilder")
  def sbt_name = "sbt-default"
  def sbt_path = "/usr/share/sbt-launcher-packaging/bin/sbt-launch.jar"

  def sbt_installation = new SbtPluginBuilder.SbtInstallation(sbt_name, sbt_path, "", [])
  descriptor.setInstallations(sbt_installation)

  descriptor.save()
}
//  https://github.com/jenkinsci/sbt-plugin/blob/master/src/main/java/org/jvnet/hudson/plugins/SbtPluginBuilder.java
