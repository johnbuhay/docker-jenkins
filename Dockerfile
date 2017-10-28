# https://hub.docker.com/r/jenkinsci/jenkins/tags/
# https://hub.docker.com/r/jenkins/jenkins/tags/
FROM jenkins/jenkins:2.86-alpine

# environmet vars
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false -Djava.awt.headless=true" \
    JENKINS_CONFIG_HOME="/usr/share/jenkins"

# Add java utils
ADD https://repo1.maven.org/maven2/org/yaml/snakeyaml/1.18/snakeyaml-1.18.jar /usr/lib/jvm/java-1.8-openjdk/jre/lib/

# install docker
USER root
RUN apk add --no-cache --virtual docker py-pip \
    && adduser jenkins users \
    && adduser jenkins docker

USER jenkins
WORKDIR /var/jenkins_home
RUN mkdir -pv /var/jenkins_home/setup /var/jenkins_home/secrets \
  && echo 'true' > /var/jenkins_home/secrets/slave-to-master-security-kill-switch
COPY xml/*.xml /var/jenkins_home/

# install jenkins plugins
RUN /usr/local/bin/install-plugins.sh \
    ansicolor \
    blueocean \
    cloudbees-folder \
    credentials-binding \
    datadog \
    envinject \
    git \
    github-oauth \
    github-branch-source \
    groovy \
    jobConfigHistory \
    job-dsl \
    kubernetes \
    kubernetes-pipeline-aggregator \
    kubernetes-pipeline-arquillian-steps \
    kubernetes-pipeline-devops-steps \
    kubernetes-pipeline-steps \
    matrix-auth \
    monitoring \
    multiple-scms \
    pegdown-formatter \
    pipeline-github-lib \
    pipeline-utility-steps \
    prometheus \
    simple-theme-plugin \
    swarm \
    workflow-aggregator \
    workflow-cps

# COPY assets/css/theme.css /var/jenkins_home/userContent/layout/theme.css
# COPY assets/js/theme.js /var/jenkins_home/userContent/layout/theme.js
COPY jenkins_home/init.groovy.d/*.groovy /usr/share/jenkins/ref/init.groovy.d/
