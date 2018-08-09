pipeline{
    agent any
    stages {
         stage('Build') {
         steps {
            script{
                def diff = sh(returnStdout: true, script: 'git diff $GIT_PREVIOUS_COMMIT $GIT_COMMITID')
                if(env.BRANCH_NAME=='testci' ){
                    sh '''
                    echo "${diff}";
                    cd security_manager;
                    STAGING=true;
                    sbt " -DSTAGING=$STAGING; reload; clean; compile;  docker:publish";
                    '''
                    sh '''
                    cd storage_manager;
                    STAGING=true;
                    sbt " -DSTAGING=$STAGING; reload; clean; compile;  docker:publish";
                    '''
                    sh '''
                    cd ;
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