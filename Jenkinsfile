pipeline{
    agent any
    enviroment{
        CHANGE=(git diff --name-only $GIT_PREVIOUS_COMMIT $GIT_COMMIT)
    }
    stages {
         stage('Build') {
         steps {
            script{
                if(env.BRANCH_NAME=='testci' && env.CHANGE.contains('security')){
                    sh '''
                    cd security_manager
                    STAGING=true;
                    pwd;
                    ls;
                    sbt " -DSTAGING=$STAGING; reload; clean; compile;  docker:publish"
                    '''
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