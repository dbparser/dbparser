set codebase = "-Djava.rmi.server.codebase=file://$HOME/dbparser/"
set sbP = -Djava.security.policy=$HOME/dbparser/policy-files/switchboard.policy
set clP = -Djava.security.policy=$HOME/dbparser/policy-files/client.policy

set sb = ($codebase $sbP)
set cl = ($codebase $clP)
