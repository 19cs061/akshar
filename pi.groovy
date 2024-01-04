pipeline {
    agent any

    environment {
        DOCKER_REGISTRY_USER = credentials("blkDockerRegistryUserName")
        DOCKER_REGISTRY_PASS = credentials("blkDockerRegistryPassword")
    }

    stages {
        stage('Checkout Code') {
            steps {
                script {
                    // Define your Git repository URL and credentials ID
                def gitRepoUrl = 'https://sathesh-subramani@bitbucket.org/blackstrawai/cicd.git'
                def gitCredentialsId = 'bitbucket'

                // Check out the code from Git
                checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: gitCredentialsId, url: gitRepoUrl]]])
                }
            }
        }

        stage('Build and Push Docker Image') {
            steps {
                script {
                    def dockerImage = 'nginx:latest'
                    def dockerRegistryUrl = 'rrdeacr.azurecr.io' 

                    // Authenticate with Docker registry
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials-id', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh "docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD $dockerRegistryUrl"
                    }

                    // Build and push the Docker image
                    sh "docker build -t $dockerImage ."
                    sh "docker tag $dockerImage $dockerRegistryUrl/$dockerImage"
                    sh "docker push $dockerRegistryUrl/$dockerImage"
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    def namespace = 'dev-akshar'
                    def deploymentName = 'nginx-deployment'

                    // Deploy the Docker image to Kubernetes
                    sh "kubectl --namespace=$namespace run $deploymentName --image=$dockerRegistryUrl/nginx:latest --port=80 --expose"
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline succeeded! Perform any additional post-build steps here.'
        }
        failure {
            echo 'Pipeline failed. Handle failure scenarios here.'
        }
    }
}
