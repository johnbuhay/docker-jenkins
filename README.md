# docker-sirjenkins
Yaml interfaces for Jenkins

The goal of this branch is to provide a Yaml interface for configuring Jenkins in two ways:
  1 - Jenkins master system configuration
  2 - Job definitions enabled by Jenkinsfile


## Demo
  Replace `{{username}}` with your Github username.
  Create a token to use the api for your user which has read permissions. Replace `{{token}}` with this value.

  ```
    docker run -d -p 8080:8080 -e JENKINS_GITHUB_USER={{user}} -e JENKINS_GITHUB_TOKEN={{token}} --name sirjenkins jnbnyc/sirjenkins
  ```

## Dependencies
  Docker