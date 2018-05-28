//function restore RDS from snapshot def RdsRestore (version){
    assert version != null
    
date= sh ( script: ' date +%d-%m-%Y', returnStdout: true).trim()
 sh "echo aws rds restore-db-instance-from-db-snapshot 
--db-instance-identifier test-ver-${version}-date-${date} 
--db-snapshot-identifier ver-${version}-snapshot"
}
//function test status AWS
 def RdsTestRun (version) {
    
    assert version != null
    date= sh ( script: ' date +%d-%m-%Y', returnStdout: true).trim()
    output= sh ( script: "aws rds describe-db-instances 
--db-instance-identifier test-ver-${version}-date-${date} | head -n 1 
|cut -f 10 " ,
			returnStdout: true).trim()
	
    if ( "${output}" == "db.t2.micro" ) {
                return true;
             
		} else {
                return false;
		}      	
	sh "sleep 28"
		
 }
 
node {
    
	def mvnHome
	
	
    stage('Preparation') {
      git 'https://github.com/IKAMTeam/blank.git'
      mvnHome = tool 'M3'
    }
	
	
    stage('Restore test DB instance') {
		 RdsRestore ("${ver}")
		 echo "${ver}"
     }
     
    stage ('Wait to running DB instance'){
        
              timeout(1) {
                  waitUntil {RdsTestRun ("${ver}")
                  }
                   
               }
    }
	
    stage('Build') {
    
         sh "'${mvnHome}/bin/mvn' clean install"
    }
   
    stage('Results') {
     // junit '**/target/surefire-reports/TEST-*.xml'
      archive '${env.WORKSPACE}/ps-web/target/*.jar'
      
    }
   
    stage('Deploy') {
       
	   //def 
WAR_PUTH="${env.WORKSPACE}/ps-web/target/ps-web-${ver}-SNAPSHOT.war"
	   
	   def 
WAR_PUTH="${env.WORKSPACE}/ps-web/target/ps-web-8.84-SNAPSHOT.war"
	   def SERV_NAME="dev2.vqs.net"
	
       ansiblePlaybook(
			playbook: "${env.HOME}/playbook/deploy.yml",
			extras: "-e war_path='$WAR_PUTH' -e 
serv_name='$SERV_NAME' " )
	
   
	
    
   }
   
}
