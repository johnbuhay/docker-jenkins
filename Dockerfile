FROM jenkins:latest

# Install supporting software
USER root
RUN apt-get update -qq && apt-get install -qqy --no-install-recommends  \
    apt-transport-https  \
    ca-certificates  \
    curl  \
    lxc  \
    iptables  \
    && rm -rf /var/cache/apt/* /var/lib/apt/lists/*

# Install plugins and config 
USER jenkins
COPY plugins.txt /usr/share/jenkins/plugins.txt


RUN /usr/local/bin/plugins.sh /usr/share/jenkins/plugins.txt
