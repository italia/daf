pipeline{
    agent any
    environment {
        CHECK = 'true'
    }
    stages {
         stage('Build') {
         steps {
            script{
                if(env.BRANCH_NAME=='testci' && env.CHECK){
                    sh '''
                    cd security_manager;
                    STAGING=true;
                    sbt " -DSTAGING=$STAGING; reload; clean; compile;  docker:publish";
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
                        cd security_manager/kubernetes;
                        pwd;
                        ls
                    '''
                    }
                }
            }
        }
    }
}