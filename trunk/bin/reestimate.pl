#!/usr/bin/perl

use Getopt::Long;

$iterations = 20;
$settingsFile = $ENV{"HOME"} . "/jbproject/dbparser/settings/em.properties";
$trainingData =
    $ENV{"HOME"} . "/scratch/wsj-02-21.mrg";
GetOptions("i|iterations=i" => \$iterations,
	   "-sf|settings-file=s" => \$settingsFile,
	   "-t=s" => \$trainingData) or usage();

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
# grab tail root and tail extension of $trainingData
# (we don't currently use the extension)
($path, $root, $extension) = $trainingData =~ m/(.*\/)?(.*)\.(.*)/;
@java = ("java");
@javaServer = ("java", "-server");
$settings = "-Dparser.settingsFile=$settingsFile";
@mem = ("-Xms1800m", "-Xmx1800m");
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

$staticData = "$dirs[0]/" . $root . ".static.observed.gz";
if (! -f $staticData) {
    # try to create static data from first observed file
    $firstObservations = "$dirs[0]/" . $root . ".observed.gz";
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
    open FIRSTOBS, "gunzip -c $firstObservations |";
    open STATIC, "| gzip -c -9 > $staticData";
    while (<FIRSTOBS>) {
	if (! m/^\((mod |head |gap )/) {
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

    $currentModel = "$prevDir/" . $root . ".obj.gz";
    $newCounts = "$nextDir/" . $root . ".observed.gz";
    $newModel = "$nextDir/" . $root . ".obj.gz";

    # first, use EMParser to constrain-parse using current model
    @smoothingSettings =
	("-Dparser.model.smoothingParametersDir=$dirs[0]/smoothing",
	 "-Dparser.model.useSmoothingParameters=true");
    print join(" ", (@javaServer, $settings, @smoothingSettings, @mem,
		     $emParserClass, "-is", $currentModel,
		     "-sa", $trainingData, "-out", $newCounts)),
          "\n";
    system(@javaServer, $settings, @smoothingSettings, @mem,
	   $emParserClass, "-is",  $currentModel,
	   "-sa", $trainingData, "-out", $newCounts);

    # next, append static counts to new counts file
#     open NEWCOUNTS, ">>$newCounts";
#     open STATIC, $staticData;
#     while (<STATIC>) {
# 	print NEWCOUNTS $_;
#     }
#     close NEWCOUNTS;
#     close STATIC;

    # finally, use Trainer to compute derived counts
    print join(" ", (@java, $settings, @smoothingSettings, @mem,
		     $trainerClass, "-it", "-l", $staticData, "-l", $newCounts,
		     "-od", $newModel)),
          "\n";
    system(@java, $settings, @smoothingSettings, @mem,
	   $trainerClass, "-it", "-l", $staticData, "-l", $newCounts,
	   "-od", $newModel);

    push @dirs, $nextDir;
}

sub usage {
    print STDERR
	"usage: [-i|--iterations <num iterations>] ",
	"[-sf|--settings-file <em settings>]\n\t[-t <training file>] ",
	"<experiment directory>\n";
    print STDERR
	"defaults:\n",
	"\t<num iterations>=$iterations\n",
	"\t<em settings>=$settingsFile\n",
	"\t<training file>=$trainingData\n";
    exit(1);
}
