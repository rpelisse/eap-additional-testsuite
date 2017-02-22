#!/bin/bash

# This scrips
usage() {
  local script_name=$(basename ${0})

  echo "${script_name} <local-maven-repo> <jboss-version-code>"
  echo
  echo "Ex: ${script_name} ./maven_local_repo wildfly"
}

readonly LOCAL_REPO_DIR=${1}
readonly JBOSS_VERSION_CODE=${2}

if [ -z "${LOCAL_REPO_DIR}" ]; then
  echo "Missing LOCAL_REPO_DIR - please provide value to the local maven repo to use"
  usage
  exit 1
fi

if [ -z "${JBOSS_VERSION_CODE}" ]; then
  echo "Missing JBOSS_VERSION_CODE (eap7, eap64,...)."
  usage
  exit 2
fi


#
# Setting up maven
#

mkdir -p "${LOCAL_REPO_DIR}"

readonly MAVEN_HOME=${MAVEN_HOME:-'/home/jboss/jenkins_workspace/tools/hudson.tasks.Maven_MavenInstallation/Maven_3.3.9'}
export MAVEN_HOME

export PATH="${MAVEN_HOME}/bin:${PATH}"

export MAVEN_OPTS="${MAVEN_OPTS} -Dmaven.wagon.http.pool=false"
export MAVEN_OPTS="${MAVEN_OPTS} -Dmaven.wagon.httpconnectionManager.maxPerRoute=3"

#
# Checking out Wildfly/EAP
#

readonly WILDFLY_GIT_REPO=${WILDFLY_GIT_REPO:-'git@github.com:wildfly/wildfly.git'}
readonly WILDFLY_BRANCH=${WILDFLY_BRANCH:-'master'}
readonly WILDFLY_CHECKOUT_FOLDER=${WILDFLY_CHECKOUT_FOLDER:-"${PWD}/${JBOSS_VERSION_CODE}"}

git clone "${WILDFLY_GIT_REPO}" --branch "${WILDFLY_BRANCH}" "${WILDFLY_CHECKOUT_FOLDER}"


#
# Building Wildfly/EAP
#

cd "${WILDFLY_CHECKOUT_FOLDER}"
export JBOSS_VERSION="$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version|grep -Ev '(^\[|Download\w+:)')"
mvn clean install -DskipTests -Dmaven.repo.local=../${LOCAL_REPO_DIR} -s ../settings.xml
cd ..

export JBOSS_FOLDER=${WILDFLY_CHECKOUT_FOLDER}/dist/target/wildfly-${JBOSS_VERSION}


#
# Run EAT
#

rm -r -f eap*  # remove previous build
export MAVEN_OPTS="-Xmx1024m -Xms512m -XX:MaxPermSize=256m"
mvn clean install -D${JBOSS_VERSION_CODE} -Dstandalone -Dmaven.repo.local=${LOCAL_REPO_DIR}
