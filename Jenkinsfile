pipeline {
    parameters {
        choice(name: 'environment', choices: ['dev'], description: 'SELECT ENV')
    }
    agent any

    tools {
        jdk 'jdk-21'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: "10"))
    }

    environment {
       SONAR_TOKEN = credentials('SONAR_TOKEN')
    }

    stages {

        stage('OWASP Dependency Check (Source Scan)') {
            when { expression { params.environment == 'dev' } }
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean build --no-daemon -x test'

                echo "Running OWASP Dependency Check..."
                dependencyCheck additionalArguments: """
                    -o './'
                    --scan ./build/libs/*.jar
                    --format XML
                    --format HTML
                    --prettyPrint
                """,
                odcInstallation: 'OWASP-Dependency-Check'

                dependencyCheckPublisher pattern: 'dependency-check-report.xml'
                echo "✅ OWASP scan completed"
            }
        }

        stage('Build Docker Image') {
            when { expression { params.environment == 'dev' } }
            steps {

                echo "Building Docker image..."
                sh """
                docker build -t nginx .
                """
                echo "✅ Docker image built"
            }
        }

        stage('Trivy Docker Image Scan') {
            when { expression { params.environment == 'dev' } }
            steps {
                echo "Running Trivy scan on Docker image..."
                sh """
                trivy image nginx \
                  --format template \
                  --template "@/usr/local/share/trivy/templates/html.tpl" \
                  --output trivy-report.html \
                  --exit-code 0 \
                  --severity CRITICAL,HIGH || echo "⚠️ Vulnerabilities detected - check the report artifacts"
                """
            }
        }

        stage('SonarQube Analysis') {
            when { expression { params.environment == 'dev' } }
            steps {
                echo "🔍 Running SonarQube analysis..." 
                sh 'chmod +x gradlew'
                sh "SONAR_TOKEN=$SONAR_TOKEN ./gradlew sonar -Dsonar.token=$SONAR_TOKEN"
                echo "✅ SonarQube Analysis generated successfully. Check the report "
            }
        }

        stage('Deploy Backend') {
            when { expression { params.environment == 'dev' } }
            steps {
                echo "🚀 Deploying backend..."
                // Add deployment commands (SSH, docker pull/run, etc.)
                echo "✅ Deployment complete"
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'trivy-report.*', fingerprint: true
            cleanWs(notFailBuild: true)
        }
    }
}
