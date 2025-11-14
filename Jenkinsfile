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

        //  (3) BUILD DOCKER IMAGE (AFTER SCANS)
        stage('Build Docker Image') {
            when { expression { params.environment == 'dev' } }
            steps {
                echo "Building Docker image..."
                sh "docker build -t my-app-image ."
                echo "✅ Docker image build completed."
            }
        }

        //  (5) PUSH DOCKER IMAGE TO REGISTRY
        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'DOCKER_HUB_CREDS') {
                        def img = docker.build("nehapatil104/devsecops-demo")
                        img.push("${BUILD_ID}")
                    }
                    echo "✅ Docker image pushed."
                }
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

        //  (4) TRIVY SCAN ON DOCKER IMAGE
        stage('Trivy Image Scan') {
            when { expression { params.environment == 'dev' } }
            steps {
                echo "🔎 Running Trivy scan on Docker image..."

                sh """
                    trivy image my-app-image \
                        --format template \
                        --template "@/usr/local/share/trivy/templates/html.tpl" \
                        --output trivy-report.html \
                        --exit-code 1 \
                        --severity CRITICAL,HIGH
                """

                echo "✅ Trivy scan completed."
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
