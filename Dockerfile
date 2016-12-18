FROM jenkins:2.19.4-alpine

# environmet vars
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false" \
    JENKINS_CONFIG_HOME="/usr/share/jenkins"

# install docker
USER root
ADD pip-requirements.txt /root/

RUN apk add --no-cache docker py-pip &&\
    adduser jenkins users &&\
    adduser jenkins docker &&\
    pip install --no-cache-dir -r /root/pip-requirements.txt
# pip freeze --disable-pip-version-check > pip-requirements.txt

ADD *.jar /usr/lib/jvm/java-1.8-openjdk/jre/lib/
# ADD jenkins_jobs.ini /etc/jenkins_jobs/jenkins_jobs.ini
USER jenkins
WORKDIR /var/jenkins_home
ADD *.py /usr/local/bin/

# install jenkins plugins
RUN /usr/local/bin/install-plugins.sh \
    matrix-auth \
    job-dsl \
    git \
    cloudbees-folder \
    envinject \
    groovy \
    multiple-scms \
    workflow-aggregator \
    github-organization-folder \
    monitoring \
    simple-theme-plugin
    # permissive-script-security  -Dpermissive-script-security.enabled=true
    # sidebar-link

ADD home/init.groovy.d/*.groovy /var/jenkins_home/init.groovy.d/
ADD org.jenkinsci.main.modules.sshd.SSHD.xml /var/jenkins_home/
ADD *.css /var/jenkins_home/userContent/
