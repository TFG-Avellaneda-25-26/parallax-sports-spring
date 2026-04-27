pipeline {
    agent any

    environment {
        REGISTRY    = 'localhost:5000'
        IMAGE_NAME  = 'parallax-spring'
        STACK_PATH  = '/opt/stack'
    }

    options {
        timeout(time: 15, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Decrypt secrets') {
            steps {
                sh '''
                    git-crypt unlock
                    test -f src/main/resources/api-secrets.yml \
                        || (echo "Secrets not decrypted!" && exit 1)
                    echo "Secrets decrypted"
                '''
            }
        }

        stage('Build jar') {
            steps {
                sh '''
                    chmod +x gradlew
                    ./gradlew clean bootJar -x test --no-daemon
                '''
            }
        }

        stage('Run tests') {
            steps {
                sh './gradlew test --no-daemon'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'build/test-results/test/*.xml'
                }
            }
        }

        stage('Build Docker image') {
            steps {
                sh """
                    docker build \
                        -t ${REGISTRY}/${IMAGE_NAME}:latest \
                        -t ${REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER} \
                        .
                """
            }
        }

        stage('Push to registry') {
            steps {
                sh """
                    docker push ${REGISTRY}/${IMAGE_NAME}:latest
                    docker push ${REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}
                """
            }
        }

        stage('Deploy') {
            steps {
                sh """
                    cd ${STACK_PATH}
                    docker compose pull spring-boot || true
                    docker compose up -d --no-deps spring-boot
                """
            }
        }
    }

    post {
        always {
            sh 'rm -f src/main/resources/api-secrets.yml || true'
            cleanWs()
        }
        success {
            echo "Deployed Spring Boot build #${BUILD_NUMBER}"
        }
        failure {
            echo "Build #${BUILD_NUMBER} failed"
        }
    }
}
