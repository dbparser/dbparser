#!/usr/bin/perl

use Getopt::Std;

if (!getopts('frp') || @ARGV == 0) {
    usage();
}

if (defined $opt_r && defined $opt_p) {
    print "\nerror: cannot use both -r and -p\n";
    usage();
}

sub usage {
    print ("\nusage: [-f] [-r | -p] <evalb output file>+\nwhere\n",
	   "\t-f indicates to print stats for sentences of length <= 40\n",
	   "\t-r indicates only to print recall\n",
	   "\t-p indicates only to print precision\n");
    exit 1;
}



foreach $file (@ARGV) {
    open(FILE, $file) or die "could not open \"$file\"";
    $readingScores = 0;
    $readPrecision = 0;
    $readRecall = 0;
    while (<FILE>) {
	if ((!$opt_f && m/^-- All/) ||
	    ($opt_f && m/^-- len<=40/)) {
	    $readingScores = 1;
	}
	if ($readingScores) {
	    if (m/^Bracketing Recall .* (\d+\.\d+)/) {
		$recall = $1;
		$readRecall = 1;
	    }
	    elsif (m/^Bracketing Precision .* (\d+\.\d+)/) {
		$precision = $1;
		$readPrecision = 1;
	    }
	}
	if ($readPrecision && $readRecall) {
	    last;
	}
    }
    #print STDERR "recall=$recall\tprecision=$precision\n";
    $f = ($precision * $recall) / (0.5 * ($precision + $recall));
    $val = ($opt_r ? $recall : ($opt_p ? $precision : $f));
    print $val, "\n";
}
