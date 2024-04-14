pipeline {
    // This pipeline can run on any available agent (Jenkins node).
    agent any

   // This block defines environment variables that will be used throughout the pipeline.
   // Credentials are retrieved using Jenkins credentials binding.
    environment {
        // AWS access key ID and secret access key are retrieved from Jenkins credentials using the credential IDs
        AWS_ACCESS_KEY_ID = credentials('aws-access-key-id')
        AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
        AWS_DEFAULT_REGION = 'your-aws-region'  // Specifies the AWS region where the deployment will occur.
        DOCKER_REGISTRY = 'your-docker-registry'  // Specifies the Docker registry where the Docker image will be pushed.
        DOCKER_IMAGE = 'your-docker-image'  // Specifies the name of the Docker image.
        EC2_INSTANCE = 'your-ec2-instance-id'  // Specifies the ID of the EC2 instance where the Docker container will be deployed.
        SSH_KEY = credentials('ssh-private-key')  // SSH private key used for SSH authentication to the EC2 instance, retrieved from Jenkins credentials.
    }

    // If the build gets stuck it will finish immediately (one hour)
    // It is set globally for all the stages
    options {
        timeout(time:  1, unit: 'Hours')
    }

    stages {
        // This stage builds the Docker image using the specified Dockerfile.
        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${DOCKER_REGISTRY}/${DOCKER_IMAGE}")
                }
            }
        }

        // This stage pushes the built Docker image to the specified Docker registry.
        // Docker credentials are provided using the credential ID docker-registry-credentials.
        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry("${DOCKER_REGISTRY}", 'docker-registry-credentials') {
                        docker.image("${DOCKER_REGISTRY}/${DOCKER_IMAGE}").push()
                    }
                }
            }
        }

        // This stage deploys the Docker image to the specified EC2 instance.
        stage('Deploy to EC2') {
            steps {
                script {
                    // It starts by using SSH agent credentials to authenticate SSH connections to the EC2 instance.
                    sshagent(credentials: ['${SSH_KEY}']) {
                        // It pulls the Docker image from the Docker registry.
                        sh "ssh -o StrictHostKeyChecking=no ec2-user@${EC2_INSTANCE} 'docker pull ${DOCKER_REGISTRY}/${DOCKER_IMAGE}'"
                        // It stops and removes any existing Docker container with the same name to ensure a clean deployment.
                        sh "ssh -o StrictHostKeyChecking=no ec2-user@${EC2_INSTANCE} 'docker stop ${DOCKER_IMAGE} || true'"
                        sh "ssh -o StrictHostKeyChecking=no ec2-user@${EC2_INSTANCE} 'docker rm ${DOCKER_IMAGE} || true'"
                        // Finally, it runs a new Docker container on the EC2 instance, mapping port 80 of the container to port 80 of the EC2 instance.
                        sh "ssh -o StrictHostKeyChecking=no ec2-user@${EC2_INSTANCE} 'docker run -d --name ${DOCKER_IMAGE} -p 80:80 ${DOCKER_REGISTRY}/${DOCKER_IMAGE}'"
                    }
                }
            }
        }
    }

    // This block defines actions to be performed after the pipeline stages have completed.
    post {
        // If the pipeline succeeds, it shows the message "Deployment successful!".
        success {
            echo 'Deployment successful'
        }
        // If the pipeline fails, it shows the message "Deployment failed".
        failure {
            echo 'Deployment failed'
        }
        // If the last pipeline build and the current one failed, it shows the message "The current build failed!"
        regression {
            echo 'Tha current build failed'
        }
    }
}