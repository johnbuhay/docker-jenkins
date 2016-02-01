import jenkins.model.*

def instance = Jenkins.getInstance()

def descriptor = instance.getDescriptor("jenkins.model.JenkinsLocationConfiguration")

//def env = System.getenv()
//def HOSTNAME = env['HOSTNAME'].toString()
//def ADMIN_ADDRESS = env['ADMIN_ADDRESS'].toString()
def HOSTNAME = System.getenv("HOSTNAME").toString()
def ADMIN_ADDRESS = System.getenv("ADMIN_ADDRESS").toString()

descriptor.setUrl('http://' + HOSTNAME + ':8080')
descriptor.setAdminAddress(ADMIN_ADDRESS)

descriptor.save()
