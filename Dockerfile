FROM jenkins:2.19.3-alpine

# environmet vars
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false" \
    JENKINS_CONFIG_HOME="/usr/share/jenkins"

# install docker
USER root
RUN apk add --no-cache docker py-pip &&\
    adduser jenkins users &&\
    adduser jenkins docker &&\
    pip install jenkins-job-builder &&\
    mkdir -p /etc/jenkins_jobs

ADD *.jar /usr/lib/jvm/java-1.8-openjdk/jre/lib/ 
# ADD jenkins_jobs.ini /etc/jenkins_jobs/jenkins_jobs.ini
USER jenkins

# install jenkins plugins
RUN /usr/local/bin/install-plugins.sh \
    matrix-auth \
    job-dsl \
    git \
    cloudbees-folder \
    envinject \
    groovy \
    multiple-scms \
    workflow-aggregator
