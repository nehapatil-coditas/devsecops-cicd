pipeline {
    parameters {
        choice(name: 'environment', choices: ['dev'], description: 'Select Environment')
    }

    agent any

    tools {
        jdk 'jdk-21'
        maven 'maven-3.9.11'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: "10"))
        timestamps()
    }

    stages {
        //  (1) BUILD APPLICATION (MAVEN)
        stage('Build Application') {
            when { expression { params.environment == 'dev' } }
            steps {
                echo "Building Maven application..."
                sh "mvn clean package -DskipTests"
                echo "✅ Application build completed."
            }
        }

        //   (2) PARALLEL STATIC SCANS (OWASP + SONAR)
        stage('Static Security Scans') {
            when { expression { params.environment == 'dev' } }
            parallel {

                //  ----- OWASP ----- 
                stage('OWASP Dependency Check') {
                    steps {
                        echo "Running OWASP Dependency Check..."

                        dependencyCheck additionalArguments: """
                            -o './dependency-check-report'
                            --scan ./target/*.jar
                            --format HTML
                            --format XML
                            --prettyPrint
                            --failOnCVSS 7
                        """,
                        odcInstallation: 'OWASP-Dependency-Check'

                        dependencyCheckPublisher pattern: 'dependency-check-report/dependency-check-report.xml', 
                                 failedTotalCritical: 1,
                                 failedTotalHigh: 1
                        
                        echo "OWASP Dependency Check stage complete."
                    }
                }

                //  ----- SONARQUBE ----- 
                stage('SonarQube Analysis') {
                    steps {
                        echo "Running SonarQube Analysis..."

                        withSonarQubeEnv('SonarQube') {
                            sh 'mvn sonar:sonar '
                        }
                        echo "✅ SonarQube Analysis complete. Waiting for Quality Gate result..."
                        script {
                            def qg = waitForQualityGate abortPipeline: true
                            echo "Quality Gate status: ${qg.status}"
                        }
                    }
                }
            }
        }

        //  (3) BUILD DOCKER IMAGE (AFTER SCANS) ONLY IF STATIC SCAN PASSES
        stage('Build Docker Image') {
            when { expression { params.environment == 'dev' } }
            steps {
                script{
                    def IMAGE_TAG = "nehapatil104/devsecops-demo:${BUILD_ID}"
                    echo "Building Docker image with tag: ${IMAGE_TAG}..."
                    sh "docker build -t ${IMAGE_TAG} ."
                    env.DOCKER_IMAGE = IMAGE_TAG
                    echo "✅ Docker image built: ${env.DOCKER_IMAGE}"
                }
            }
        }

        //  (4) TRIVY SCAN ON DOCKER IMAGE 
        stage('Trivy Image Scan') {
            when { expression { params.environment == 'dev' } }
            steps {
                echo "🔎 Running Trivy scan on Docker image..."

                sh """
                    trivy image ${env.DOCKER_IMAGE} \
                        --format template \
                        --template "@/usr/local/share/trivy/templates/html.tpl" \
                        --output trivy-report.html \
                        --exit-code 1 \
                        --severity CRITICAL,HIGH
                """

                echo "✅ Trivy scan completed."
            }
        }

         //  (5) PUSH DOCKER IMAGE TO REGISTRY ONLY IF IMAGE SCAN PASSES
        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'DOCKER_HUB_CREDENTIALS') {
                        def img = docker.image("${env.DOCKER_IMAGE}")
                        img.push()
                    }
                    echo "✅ Docker image pushed: ${env.DOCKER_IMAGE}"
                }
            }
        }

        //  (6) DEPLOY BACKEND (ONLY IF ALL SCANS PASS)
        stage('Deploy Backend') {
            when { expression { params.environment == 'dev' } }
            steps {
                echo "🚀 Deploying backend..."

                // Your deployment code.
                
                echo "✅ Deployment completed."
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/*.html', fingerprint: true
            cleanWs(notFailBuild: true)
        }
    }
}
