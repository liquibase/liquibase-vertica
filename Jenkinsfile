/*
  These steps are provided by CICD team maintained global shared library.
  https://github.com/integralads/jenkins-pipeline-scripts/tree/master/vars
  See:
    - publishToNexusOnNewTag.groovy
    - createTagOnMergeTo.groovy
 */

//use tag release step provided by integralads/jenkins-pipeline-scripts repository
createTagOnMergeTo('master')

//use nexus publish step provided by integralads/jenkins-pipeline-scripts repository
publishToNexusOnNewTag()