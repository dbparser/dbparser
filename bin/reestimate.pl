#!/usr/bin/perl

use Getopt::Long;

$iterations = 20;
GetOptions("i|iterations=i" => \$iterations) or usage();

if (@ARGV == 1) {
    $expDir = $ARGV[0]
}
else {
    print STDERR "You must specify an experiment directory.\n";
    usage();
}

chdir $expDir or die "Can't cd to $expDir: $!\n";

print "Performing $iterations iteration", ($iterations == 1 ? "" : "s"), ".\n";

# data that won't change from iteration to iteration
$trainingData = $ENV{"HOME"} . "/scratch/wsj-02-21.mrg";
$java = "java";
@mem = ("-Xms1600m", "-Xmx1600m");
$emParserClass = "danbikel.parser.EMParser";
$trainerClass = "danbikel.parser.Trainer";

# grab all directories with numerical names
@dirs = grep -d, <[0-9]*>;

if (@dirs == 0) {
    print STDERR
	"No numerically-named directories in directory \"$expDir\".  ",
	"Exiting.\n";
    exit(1);
}

$staticData = "$dirs[0]/wsj-02-21.static.observed";
if (! -f $staticData) {
    # try to create static data from first observed file
    $firstObservations = "$dirs[0]/wsj-02-21.observed";
    if (! -f $firstObservations) {
	print STDERR
	    "Couldn't find static data file \"$staticData\".\n",
	    "Couldn't find \"$firstObservations\" from which to ",
	    "create static data.\n";
	exit 1;
    }
    print STDERR
	"Creating static data file\n\t\"$staticData\"\nfrom ",
	"initial observations file\n\t\"$firstObservations\".\n";
    open FIRSTOBS, $firstObservations;
    open STATIC, ">$staticData";
    while (<FIRSTOBS>) {
	if (! m/^\((mod |head )/) {
	    print STATIC $_;
	}
    }
    close FIRSTOBS;
    close STATIC;
}

for ($i = 0; $i < $iterations; $i++) {
    $prevDir = $dirs[$#dirs];
    ($nextDir = $prevDir)++;

    print "Beginning iteration No. $nextDir in directory $expDir.\n";

    mkdir($nextDir, 0775);

    $currentModel = "$prevDir/wsj-02-21.obj";
    $newCounts = "$nextDir/wsj-02-21.observed";
    $newModel = "$nextDir/wsj-02-21.obj";

    # first, use EMParser to constrain-parse using current model
    system($java, @mem, $emParserClass, "-is",  $currentModel,
	   "-sa", $trainingData, "-out", $newCounts);

    # next, append static counts to new counts file
    open NEWCOUNTS, ">>$newCounts";
    open STATIC, $staticData;
    while (<STATIC>) {
	print NEWCOUNTS $_;
    }
    close NEWCOUNTS;
    close STATIC;

    # finally, use Trainer to compute derived counts
    system($java, @mem, $trainerClass, "-l", $newCounts, "-od", $newModel);

    push @dirs, $nextDir;
}

sub usage {
    print STDERR
	"usage: [-i|--iterations <num iterations>] <experiment directory>\n";
    exit(1);
}
