import jenkins.model.*

def instance = Jenkins.getInstance()

def descriptor = instance.getDescriptor("hudson.plugins.git.GitSCM")
def ADMIN_ADDRESS = System.getenv("ADMIN_ADDRESS").toString()
descriptor.setGlobalConfigName("Sir Jenkins")
descriptor.setGlobalConfigEmail(ADMIN_ADDRESS)

descriptor.save()
