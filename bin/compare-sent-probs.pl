#!/usr/bin/perl

if (@ARGV != 2) {
    print "usage: <reestimate log 1> <reestimate log 2>\n";
    exit 1;
}

open(LOG1, $ARGV[0]);
open(LOG2, $ARGV[1]);

$i = 0;
while (<LOG1>) {
    if (m/sentence inside logProb: ([\d\+\-\.E]+)/) {
	$log1Probs[$i++] = $1;
    }
}
$i = 0;
while (<LOG2>) {
    if (m/sentence inside logProb: ([\d\+\-\.E]+)/) {
	$log2Probs[$i++] = $1;
    }
}
for $i (0 .. $#log1Probs) {
    
    print (($i + 1), "\t", $log1Probs[$i], "\t", $log2Probs[$i], "\t",
	   ($log2Probs[$i] - $log1Probs[$i]), "\n");
}
