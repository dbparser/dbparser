set codebase = "-Djava.rmi.server.codebase=file://$HOME/db-parser/"
set sbP = -Djava.security.policy=$HOME/db-parser/policy-files/switchboard.policy
set clP = -Djava.security.policy=$HOME/db-parser/policy-files/client.policy

set sb = ($codebase $sbP)
set cl = ($codebase $clP)
