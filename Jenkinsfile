pipeline{
    agent any
     stages {

         stage('Build') {
         steps {
             script{
                CHANGE=git diff --name-only $GIT_PREVIOUS_COMMIT $GIT_COMMIT
                if(env.BRANCH_NAME=='testci'){
                    if(env.CHANGE.contains("security")){
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