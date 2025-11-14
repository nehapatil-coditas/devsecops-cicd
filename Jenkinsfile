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

        /* -------------------------------------------------
         * (1) BUILD APPLICATION (MAVEN)
         * ------------------------------------------------- */
        stage('Build Application') {
            when { expression { params.environment == 'dev' } }
            steps {
                echo "📦 Building Maven application..."
                sh "mvn clean package -DskipTests"
                echo "✅ Application build completed."
            }
        }

        /* -------------------------------------------------
         * (2) PARALLEL STATIC SCANS (OWASP + SONAR)
         * ------------------------------------------------- */
        stage('Static Security Scans') {
            when { expression { params.environment == 'dev' } }
            parallel {

                /* ----- OWASP ----- */
                stage('OWASP Dependency Check') {
                    steps {
                        echo "Running OWASP Dependency Check..."

                        dependencyCheck additionalArguments: """
                            -o './dependency-check-report'
                            --scan ./target/*.jar
                            --format XML
                            --format HTML
                            --prettyPrint
                        """,
                        odcInstallation: 'OWASP-Dependency-Check'

                        dependencyCheckPublisher pattern: 'dependency-check-report/dependency-check-report.xml'

                        echo "✅ OWASP Dependency Check complete."
                    }
                }

                /* ----- SONARQUBE ----- */
                stage('SonarQube Analysis') {
                    steps {
                        echo "🔍 Running SonarQube Analysis..."

                        withSonarQubeEnv('SonarQube') {
                            sh """
                                mvn sonar:sonar 
                            """
                        }

                        echo "✅ SonarQube Analysis complete."
                    }
                }
            }
        }

        /* -------------------------------------------------
         * (3) BUILD DOCKER IMAGE (AFTER SCANS)
         * ------------------------------------------------- */
        stage('Build Docker Image') {
            when { expression { params.environment == 'dev' } }
            steps {
                echo "🐳 Building Docker image..."
                sh "docker build -t my-app-image ."
                echo "✅ Docker image build completed."
            }
        }

        /* -------------------------------------------------
         * (4) TRIVY SCAN ON DOCKER IMAGE
         * ------------------------------------------------- */
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

        /* -------------------------------------------------
         * (5) PUSH DOCKER IMAGE TO REGISTRY
         * ------------------------------------------------- */
        // stage('Push Docker Image to Registry') {
        //     when { expression { params.environment == 'dev' } }
        //     steps {
        //         echo "📤 Pushing Docker image to registry..."

        //         withCredentials([usernamePassword(
        //             credentialsId: 'DOCKER_REGISTRY_CREDS',   // configure in Jenkins Credentials
        //             usernameVariable: 'REG_USER',
        //             passwordVariable: 'REG_PASS'
        //         )]) {

        //             sh """
        //                 echo "$REG_PASS" | docker login -u "$REG_USER" --password-stdin <your-registry-url>

        //                 docker tag my-app-image <your-registry-url>/my-app:latest
        //                 docker push <your-registry-url>/my-app:latest
        //             """
        //         }

        //         echo "✅ Image pushed to registry successfully."
        //     }
        // }

        /* -------------------------------------------------
         * (6) DEPLOY BACKEND (ONLY IF ALL SCANS PASS)
         * ------------------------------------------------- */
        stage('Deploy Backend') {
            when { expression { params.environment == 'dev' } }
            steps {
                echo "🚀 Deploying backend..."

                // Example:
                // sh "ssh ubuntu@backend-server 'docker stop app || true && docker rm app || true'"
                // sh "ssh ubuntu@backend-server 'docker load < my-app-image.tar'"
                // sh "ssh ubuntu@backend-server 'docker run -d --name app -p 8080:8080 my-app-image'"

                echo "✅ Deployment completed."
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/*.html, **/*.xml', fingerprint: true
            cleanWs(notFailBuild: true)
        }
    }
}
