pipeline{
    agent any
    enviroment{
        CHECK=true
    }
    stages {
         stage('Build') {
         steps {
            script{
                sh 'CHANGE=(git diff --name-only $GIT_PREVIOUS_COMMIT $GIT_COMMIT); if [[$CHANGE=*'security'*]]; then CHECK=false; echo $CHECK'
                if(env.BRANCH_NAME=='testci' && CHECK){
                    sh '
                    cd security_manager
                    STAGING=true;
                    pwd;
                    ls;
                    sbt " -DSTAGING=$STAGING; reload; clean; compile;  docker:publish"
                    '
                }
                }
            }
        }
        stage('Staging'){
            steps{
            script{
                if(env.BRANCH_NAME=='testci'){
                    sh '''
                        cd security_manager/kubernetes
                        echo "prova"
                    '''
                    }
                }
            }
        }
    }
}