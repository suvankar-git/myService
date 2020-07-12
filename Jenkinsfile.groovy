#!/usr/bin/env groovy

import java.util.Date
import groovy.json.JsonOutput
import groovy.json.JsonSlurper


def displayName = env.JOB_NAME
def bucketName = env.JOB_NAME
def isMaster = env.BRANCH_NAME == "master"
def isStaging = env.BRANCH_NAME == "staging"
def start = new Date()
def err = null

String jobInfoShort = "${env.JOB_NAME} ${env.BUILD_DISPLAY_NAME}"
String jobInfo = "${env.JOB_NAME} ${env.BUILD_DISPLAY_NAME} \n${env.BUILD_URL}"
String buildStatus

currentBuild.result = "SUCCESS"

try {
    node {
                
        deleteDir()
        stage ('Checkout') {
            checkout scm
        }
        // In the interim, run builds only on staging and master
        if (isStaging || isMaster) {
            stage('Prepare Environment') {
                sh "pip3 install --user -r requirements.txt"
            }
            stage ("Deploy with Chalice"){
                if (isStaging) {
                    sh "export AWS_DEFAULT_REGION=ap-south-1 && serverless deploy --stage staging"
                } else {
                    sh "export AWS_DEFAULT_REGION=ap-south-1 && serverless deploy --stage production"
                }
                slackSend (color: 'good', message: "${response}") // send to slack
            }
        }
        
    }
} catch (caughtError) {
    err = caughtError
    currentBuild.result = "FAILURE"
    slackSend (color: 'danger', message: "_Build failed_: ${jobInfo}")
        throw err
}