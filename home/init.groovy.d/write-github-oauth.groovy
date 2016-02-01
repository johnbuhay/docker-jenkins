import hudson.model.*;
import jenkins.model.*;


Thread.start {
  oauth = System.getenv("GITHUB_USEROAUTH") ?: ''

  write_file(".github", "oauth=" + oauth)
}

def write_file(name, contents) {
  def JENKINS_HOME = System.getenv('JENKINS_HOME') ?: '/var/jenkins_home'
  //Define the name of the file.
  def fileName = name
  // Defining a file handler/pointer to handle the file.
  def inputFile = new File("${JENKINS_HOME}/"+fileName)
  // Check if a file with same name exisits in the folder.
  if(inputFile.exists())
  {
    // if a file exisits then it will print the message to the log.
    println "A file named " + fileName + " already exisits in the same folder"
    inputFile.write(contents)
  }
  else {
    //else it will create a file and write the contents
    inputFile.write(contents)
  }
}