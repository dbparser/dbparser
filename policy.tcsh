set dbparserDir = ~/jbproject/dbparser
set codebase = "-Djava.rmi.server.codebase=file://$dbparserDir/classes/"
set policyDir = $dbparserDir/policy-files
set sbP = -Djava.security.policy=$policyDir/switchboard.policy
set clP = -Djava.security.policy=$policyDir/client.policy

set sb = ($codebase $sbP)
set cl = ($codebase $clP)
