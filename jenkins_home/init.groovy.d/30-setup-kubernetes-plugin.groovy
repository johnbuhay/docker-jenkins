// Resource: https://plugins.jenkins.io/kubernetes
// Resource: https://github.com/jenkinsci/kubernetes-plugin
@Grab(group='org.yaml', module='snakeyaml', version='1.18')

import groovy.transform.Field
import java.util.logging.Logger
import jenkins.model.Jenkins
import org.csanchez.jenkins.plugins.kubernetes.ContainerEnvVar
import org.csanchez.jenkins.plugins.kubernetes.ContainerTemplate
import org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud
import org.csanchez.jenkins.plugins.kubernetes.PodEnvVar
import org.csanchez.jenkins.plugins.kubernetes.PodImagePullSecret
import org.csanchez.jenkins.plugins.kubernetes.PodTemplate
import org.csanchez.jenkins.plugins.kubernetes.PodVolumes
// import org.csanchez.jenkins.plugins.kubernetes.ServiceAccountCredential
// import org.csanchez.jenkins.plugins.kubernetes.ServiceAccountCredential.DescriptorImpl
import org.csanchez.jenkins.plugins.kubernetes.volumes.EmptyDirVolume
import org.csanchez.jenkins.plugins.kubernetes.volumes.SecretVolume
import org.yaml.snakeyaml.Yaml


@Field
Logger logger = Logger.getLogger('setup-kubernetes-plugin.groovy')

@Field
def env = System.getenv()

@Field
def JENKINS_KUBERNETES_SETUP_YAML = env['JENKINS_KUBERNETES_SETUP_YAML'] ?: "${env['JENKINS_CONFIG_HOME']}/setup/kubernetes.yml"

@Field
def config = null

try {
  logger.info('--> Configuring Kubernetes Cloud Plugin')
  config = new Yaml().load(new File(JENKINS_KUBERNETES_SETUP_YAML).text)
} catch (Throwable e) {
  logger.warning("--> No configuration file found at ${JENKINS_KUBERNETES_SETUP_YAML}")
  return
}

cloudList = []
if (config?.cloud?.kubernetes) {
    config.cloud.kubernetes.each { kubeClouds ->
      kubeClouds.each { kube ->
        kubernetes = new KubernetesCloud(kube.name)
        kubernetes.with {
          setServerUrl(kube.serverUrl)
          setNamespace(kube.namespace)
          setJenkinsUrl(kube.jenkinsUrl)
          setJenkinsTunnel(kube.jenkinsTunnel)
          setSkipTlsVerify(kube.skipTlsVerify)
          setCredentialsId(kube.credentialsId ?: "${kube.name}-${kube.namespace}-credentials")
          setServerCertificate(kube.credentials?.certificate_authority_data)
        }
        loadTemplates(kube.agentTemplates).each {
          kubernetes.addTemplate(it)
        }
        cloudList.add(kubernetes)
      }
    }
}

cloudList.each { Jenkins.instance.clouds.replace(it) }
Jenkins.instance.save()
logger.info('--> Configuring Kubernetes Cloud Plugin... done')


// Helper Methods
def getContainers(podContainers) {
  List<ContainerTemplate> containers = new ArrayList<ContainerTemplate>()
  podContainers.each { c ->
    container = new ContainerTemplate(c.name, c.image)
    container.with {
      setAlwaysPullImage(c.alwaysPullImage ?: true)
      setWorkingDir(c.workingDir ?: '')
      setCommand(c.command ?: '')
      setArgs(c.args ?: '')
      setPrivileged(c.privileged ?: false)
      setEnvVars(getContainerEnvVars(c.envVars))
    }
    containers.add(container)
  }
  return containers
}

def getContainerEnvVars(variables) {
    List<ContainerEnvVar> envVars = new ArrayList<>()
    variables.each { variable ->
      envVars.add(new ContainerEnvVar(variable.key, variable.value))
    }
    return envVars
}

def getPodEnvVars(variables) {
    List<PodEnvVar> envVars = new ArrayList<>()
    variables.each { variable ->
      envVars.add(new PodEnvVar(variable.key, variable.value))
    }
    return envVars
}

def getVolumes(podVolumes) {
  List<PodVolumes> volumes = new ArrayList<PodVolumes>()
  podVolumes.each { volume ->
    switch (volume.type) {
      case "EmptyDirVolume":
        volumes.add(new EmptyDirVolume(volume.mountPath, volume.inMemory ?: true))
        break;
      case "SecretVolume":
        volumes.add(new SecretVolume(volume.mountPath, volume.secretName))
        break;
    }
  }
  return volumes
}

def getPodImagePullSecrets(secrets) {
    List<PodImagePullSecret> imagePullSecrets = new ArrayList<PodImagePullSecret>()
    secrets.each { secret ->
      imagePullSecrets.add(new PodImagePullSecret(secret))
    }
    return imagePullSecrets
}

def loadTemplates(podTemplates) {
    List<PodTemplate> templates = new ArrayList<PodTemplate>()
    podTemplates.each { pod ->
      PodTemplate template = new PodTemplate()
      template.with {
        setName(pod.name)
        setLabel(pod.label)
        setIdleMinutes(pod.idleMinutes)
        setInstanceCap(pod.instanceCap ?: -1)
        setNamespace(pod.namespace)
        setNodeUsageMode(pod.nodeUsageMode)
        setNodeSelector(pod.nodeSelector)

        setContainers(getContainers(pod.containers))
        setEnvVars(getPodEnvVars(pod.envVars))
        setImagePullSecrets(getPodImagePullSecrets(pod.imagePullSecrets))
        setVolumes(getVolumes(pod.volumes))
      }
      templates.add(template)
    }
    return templates
}
