import jenkins.model.*

def instance = Jenkins.getInstance()

def executors = 0
executors = System.getenv("EXECUTORS").toInteger()

instance.setNumExecutors(executors)

instance.save()